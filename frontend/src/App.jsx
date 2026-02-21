import { useCallback, useEffect, useMemo, useState } from 'react'
import './App.css'
import { apiDownload, apiRequest, getApiBaseUrl, normalizeError } from './lib/api'
import ImportView from './views/ImportView'
import LoginView from './views/LoginView'
import ModulesView from './views/ModulesView'
import PreviewView from './views/PreviewView'

const ROUTES = {
  login: '/login',
  modules: '/modules',
  imports: '/imports',
  preview: '/preview',
}

function getRouteFromHash() {
  const route = window.location.hash.replace('#', '') || ROUTES.login
  if (Object.values(ROUTES).includes(route)) return route
  return ROUTES.login
}

function App() {
  const [route, setRoute] = useState(getRouteFromHash())
  const [token, setToken] = useState(localStorage.getItem('sara_token') || '')
  const [email, setEmail] = useState('admin@admin.com')
  const [password, setPassword] = useState('adminmiralmonte')
  const [selectedModuleId, setSelectedModuleId] = useState('')
  const [newModule, setNewModule] = useState({
    name: '',
    academicYear: '2025-2026',
    teacherName: '',
  })
  const [importForm, setImportForm] = useState({ moduleId: '', file: null })
  const [importJobId, setImportJobId] = useState('')
  const [importJobData, setImportJobData] = useState(null)
  const [detectedRas, setDetectedRas] = useState([])
  const [previewData, setPreviewData] = useState(null)
  const [previewEvaluationReports, setPreviewEvaluationReports] = useState([])
  const [previewFinalReport, setPreviewFinalReport] = useState(null)
  const [busy, setBusy] = useState(false)
  const [log, setLog] = useState('')

  const isAuthenticated = useMemo(() => Boolean(token), [token])

  const navigate = useCallback((nextRoute) => {
    window.location.hash = nextRoute
  }, [])

  useEffect(() => {
    const onHashChange = () => setRoute(getRouteFromHash())
    window.addEventListener('hashchange', onHashChange)
    if (!window.location.hash) window.location.hash = ROUTES.login
    return () => window.removeEventListener('hashchange', onHashChange)
  }, [])

  useEffect(() => {
    if (!isAuthenticated && route !== ROUTES.login) navigate(ROUTES.login)
  }, [isAuthenticated, route, navigate])

  const pushLog = (message) => setLog(message)

  const login = async (event) => {
    event.preventDefault()
    setBusy(true)
    try {
      const response = await apiRequest('/auth/login', {
        method: 'POST',
        body: { email, password },
      })
      const accessToken = response.accessToken || ''
      setToken(accessToken)
      localStorage.setItem('sara_token', accessToken)
      pushLog(`Sesion iniciada: ${response.email || email}`)
      navigate(ROUTES.modules)
    } catch (error) {
      pushLog(normalizeError(error, 'Error en login.'))
    } finally {
      setBusy(false)
    }
  }

  const logout = () => {
    localStorage.removeItem('sara_token')
    setToken('')
    setSelectedModuleId('')
    setImportForm({ moduleId: '', file: null })
    setImportJobId('')
    setImportJobData(null)
    setDetectedRas([])
    setPreviewData(null)
    setPreviewEvaluationReports([])
    setPreviewFinalReport(null)
    pushLog('Sesion cerrada.')
    navigate(ROUTES.login)
  }

  const createModule = async (event) => {
    event.preventDefault()
    if (!token) return
    if (!newModule.teacherName.trim()) {
      pushLog('El campo Docente es obligatorio para crear modulo.')
      return
    }
    setBusy(true)
    try {
      const payload = {
        name: newModule.name.trim(),
        academicYear: newModule.academicYear.trim(),
      }
      if (newModule.teacherName.trim()) payload.teacherName = newModule.teacherName.trim()
      const created = await apiRequest('/modules', { method: 'POST', token, body: payload })
      setSelectedModuleId(String(created.id))
      setImportForm((prev) => ({ ...prev, moduleId: String(created.id) }))
      pushLog(`Modulo creado: #${created.id} ${created.name}`)
    } catch (error) {
      pushLog(normalizeError(error, 'No se pudo crear el modulo.'))
    } finally {
      setBusy(false)
    }
  }

  const deleteModule = async () => {
    if (!token) return

    const moduleId = String(selectedModuleId || '').trim()
    if (!moduleId) {
      pushLog('Indica un modulo (ID) para borrar.')
      return
    }

    const confirmDelete = window.confirm(
      `Se eliminara el modulo #${moduleId} y todos sus datos relacionados. Esta accion no se puede deshacer. ¿Continuar?`,
    )
    if (!confirmDelete) return

    setBusy(true)
    try {
      await apiRequest(`/modules/${moduleId}`, {
        method: 'DELETE',
        token,
      })

      if (String(importForm.moduleId || '').trim() === moduleId) {
        setImportForm((prev) => ({ ...prev, moduleId: '' }))
      }
      if (String(previewData?.moduleId || '').trim() === moduleId) {
        setPreviewData(null)
        setPreviewEvaluationReports([])
        setPreviewFinalReport(null)
      }

      setSelectedModuleId('')
      setImportJobId('')
      setImportJobData(null)
      setDetectedRas([])
      pushLog(`Modulo #${moduleId} eliminado.`)
    } catch (error) {
      pushLog(normalizeError(error, 'No se pudo eliminar el modulo.'))
    } finally {
      setBusy(false)
    }
  }

  const parseDetectedRas = (resultJson) => {
    if (!resultJson) return []
    try {
      const parsed = JSON.parse(resultJson)
      const source = Array.isArray(parsed.detectedRas)
        ? parsed.detectedRas
        : Array.isArray(parsed.ras)
          ? parsed.ras
          : []
      return source.map((ra, index) => ({
        id: `${ra.code || 'RA'}-${index}`,
        code: ra.code || '',
        name: ra.name || '',
        weightPercent: String(ra.weightPercent ?? ''),
      }))
    } catch {
      return []
    }
  }

  const getFileExtension = (fileName) => {
    if (!fileName || typeof fileName !== 'string') return ''
    const dotIndex = fileName.lastIndexOf('.')
    if (dotIndex < 0) return ''
    return fileName.slice(dotIndex).toLowerCase()
  }

  const onCreateImportJob = async (event) => {
    event.preventDefault()
    if (!token) return
    if (!importForm.file) {
      pushLog('Selecciona un archivo para importar.')
      return
    }

    const extension = getFileExtension(importForm.file.name)
    const normalizedModuleId = String(importForm.moduleId || '').trim()

    if (extension === '.pdf' && !normalizedModuleId) {
      pushLog('Para PDF debes indicar el modulo (ID).')
      return
    }

    if (!['.pdf', '.xlsx'].includes(extension)) {
      pushLog('Formato no soportado. Usa .pdf o .xlsx.')
      return
    }

    if (normalizedModuleId && !/^\d+$/.test(normalizedModuleId)) {
      pushLog('El modulo (ID) debe ser numerico.')
      return
    }

    setBusy(true)
    try {
      if (extension === '.pdf') {
        const formData = new FormData()
        formData.append('moduleId', normalizedModuleId)
        formData.append('file', importForm.file)

        const createdJob = await apiRequest('/imports/ra', {
          method: 'POST',
          token,
          body: formData,
        })
        setImportJobData(createdJob)
        setImportJobId(String(createdJob.id || ''))
        setDetectedRas(parseDetectedRas(createdJob.resultJson))
        pushLog(`Import job creado: #${createdJob.id} (${createdJob.status}).`)
        return
      }

      const excelFormData = new FormData()
      if (normalizedModuleId) {
        excelFormData.append('moduleId', normalizedModuleId)
      }
      excelFormData.append('file', importForm.file)

      const imported = await apiRequest('/imports/excel-file', {
        method: 'POST',
        token,
        body: excelFormData,
      })

      setImportJobData(imported)
      setImportJobId('')
      setDetectedRas([])
      if (imported?.moduleId) {
        setSelectedModuleId(String(imported.moduleId))
        setImportForm((prev) => ({ ...prev, moduleId: String(imported.moduleId) }))
      }
      pushLog(
        `Excel importado: modulo #${imported.moduleId}, RAs ${imported.raCount}, UTs ${imported.utCount}, instrumentos ${imported.instrumentCount}, alumnos ${imported.studentCount}, notas ${imported.gradeCount}.`,
      )
    } catch (error) {
      pushLog(normalizeError(error, 'No se pudo importar el archivo.'))
    } finally {
      setBusy(false)
    }
  }

  const onLoadPreview = async (event) => {
    event.preventDefault()
    if (!token) return

    const moduleId = String(selectedModuleId || '').trim()
    if (!moduleId) {
      pushLog('Selecciona un modulo (ID) para cargar la vista previa.')
      return
    }

    setBusy(true)
    try {
      const preview = await apiRequest(`/modules/${moduleId}/preview`, { token })
      setPreviewData(preview)

      const evaluationPeriods = [
        ...new Set((preview.uts || []).map((ut) => Number(ut.evaluationPeriod)).filter(Number.isFinite)),
      ].sort((a, b) => a - b)

      try {
        const evaluationReports = await Promise.all(
          evaluationPeriods.map((period) => apiRequest(`/modules/${moduleId}/reports/evaluation/${period}`, { token })),
        )
        const finalReport = await apiRequest(`/modules/${moduleId}/reports/final`, { token })
        setPreviewEvaluationReports(evaluationReports)
        setPreviewFinalReport(finalReport)
        pushLog(`Vista previa cargada para modulo #${moduleId}.`)
      } catch (reportError) {
        setPreviewEvaluationReports([])
        setPreviewFinalReport(null)
        pushLog(
          normalizeError(
            reportError,
            `Vista previa base cargada para modulo #${moduleId}, pero no se pudieron calcular reportes.`,
          ),
        )
      }
    } catch (error) {
      setPreviewData(null)
      setPreviewEvaluationReports([])
      setPreviewFinalReport(null)
      pushLog(normalizeError(error, 'No se pudo cargar la vista previa del modulo.'))
    } finally {
      setBusy(false)
    }
  }

  const onDownloadExcel = async () => {
    if (!token) return

    const moduleId = String(selectedModuleId || '').trim()
    if (!moduleId) {
      pushLog('Selecciona un modulo (ID) antes de descargar el Excel.')
      return
    }

    setBusy(true)
    try {
      const { blob, filename } = await apiDownload(`/modules/${moduleId}/export/excel`, { token })
      const downloadName = filename || `module-${moduleId}-export.xlsx`

      const url = window.URL.createObjectURL(blob)
      const anchor = document.createElement('a')
      anchor.href = url
      anchor.download = downloadName
      document.body.appendChild(anchor)
      anchor.click()
      anchor.remove()
      window.URL.revokeObjectURL(url)

      pushLog(`Excel descargado: ${downloadName}`)
    } catch (error) {
      pushLog(normalizeError(error, 'No se pudo descargar el Excel del modulo.'))
    } finally {
      setBusy(false)
    }
  }

  const onDownloadBaseTemplate = async () => {
    if (!token) return

    setBusy(true)
    try {
      const { blob, filename } = await apiDownload('/modules/export/template/base', { token })
      const downloadName = filename || 'source_template_unprotected.xlsx'

      const url = window.URL.createObjectURL(blob)
      const anchor = document.createElement('a')
      anchor.href = url
      anchor.download = downloadName
      document.body.appendChild(anchor)
      anchor.click()
      anchor.remove()
      window.URL.revokeObjectURL(url)

      pushLog(`Plantilla base descargada: ${downloadName}`)
    } catch (error) {
      pushLog(normalizeError(error, 'No se pudo descargar la plantilla base.'))
    } finally {
      setBusy(false)
    }
  }

  const onDownloadFilledTemplate = async () => {
    if (!token) return

    setBusy(true)
    try {
      const { blob, filename } = await apiDownload('/modules/export/template/filled', { token })
      const downloadName = filename || 'source_template_rellenada_unprotected.xlsx'

      const url = window.URL.createObjectURL(blob)
      const anchor = document.createElement('a')
      anchor.href = url
      anchor.download = downloadName
      document.body.appendChild(anchor)
      anchor.click()
      anchor.remove()
      window.URL.revokeObjectURL(url)

      pushLog(`Plantilla rellenada descargada: ${downloadName}`)
    } catch (error) {
      pushLog(normalizeError(error, 'No se pudo descargar la plantilla rellenada.'))
    } finally {
      setBusy(false)
    }
  }

  const onSaveGrades = async (changes) => {
    if (!token) return false

    const moduleId = String(selectedModuleId || '').trim()
    if (!moduleId) {
      pushLog('Selecciona un modulo (ID) antes de guardar notas.')
      return false
    }
    if (!Array.isArray(changes) || changes.length === 0) {
      pushLog('No hay notas modificadas para guardar.')
      return false
    }

    setBusy(true)
    try {
      await apiRequest('/grades', {
        method: 'POST',
        token,
        body: {
          grades: changes.map((item) => {
            const payloadItem = {
              studentId: item.studentId,
              instrumentId: item.instrumentId,
            }
            if (item.gradeValue !== undefined && item.gradeValue !== null) {
              payloadItem.gradeValue = item.gradeValue
            }
            if (Array.isArray(item.exerciseGrades)) {
              payloadItem.exerciseGrades = item.exerciseGrades.map((exercise) => ({
                exerciseIndex: exercise.exerciseIndex,
                gradeValue: exercise.gradeValue,
              }))
            }
            return payloadItem
          }),
        },
      })

      const preview = await apiRequest(`/modules/${moduleId}/preview`, { token })
      setPreviewData(preview)

      const evaluationPeriods = [
        ...new Set((preview.uts || []).map((ut) => Number(ut.evaluationPeriod)).filter(Number.isFinite)),
      ].sort((a, b) => a - b)

      try {
        const evaluationReports = await Promise.all(
          evaluationPeriods.map((period) => apiRequest(`/modules/${moduleId}/reports/evaluation/${period}`, { token })),
        )
        const finalReport = await apiRequest(`/modules/${moduleId}/reports/final`, { token })
        setPreviewEvaluationReports(evaluationReports)
        setPreviewFinalReport(finalReport)
      } catch {
        setPreviewEvaluationReports([])
        setPreviewFinalReport(null)
      }

      pushLog(`Notas guardadas en BD: ${changes.length}.`)
      return true
    } catch (error) {
      pushLog(normalizeError(error, 'No se pudieron guardar las notas.'))
      return false
    } finally {
      setBusy(false)
    }
  }

  const onLoadImportJob = async (event) => {
    event.preventDefault()
    if (!token) return
    if (!importJobId.trim()) {
      pushLog('Introduce un jobId.')
      return
    }

    setBusy(true)
    try {
      const loaded = await apiRequest(`/imports/ra/${importJobId.trim()}`, { token })
      setImportJobData(loaded)
      setDetectedRas(parseDetectedRas(loaded.resultJson))
      if (loaded.moduleId) setImportForm((prev) => ({ ...prev, moduleId: String(loaded.moduleId) }))
      pushLog(`Job #${loaded.id} cargado (${loaded.status}).`)
    } catch (error) {
      pushLog(normalizeError(error, 'No se pudo consultar el import job.'))
    } finally {
      setBusy(false)
    }
  }

  const onUpdateDetectedRa = (raId, field, value) => {
    setDetectedRas((prev) => prev.map((ra) => (ra.id === raId ? { ...ra, [field]: value } : ra)))
  }

  const onPersistDetectedRas = async () => {
    if (!token) return
    if (!importForm.moduleId.trim()) {
      pushLog('Selecciona modulo antes de guardar los RAs.')
      return
    }
    if (!detectedRas.length) {
      pushLog('No hay RAs detectados para guardar.')
      return
    }

    setBusy(true)
    try {
      const payload = {
        ras: detectedRas.map((ra) => ({
          code: String(ra.code || '').trim(),
          name: String(ra.name || '').trim(),
          weightPercent: Number(ra.weightPercent || 0),
        })),
      }
      const saved = await apiRequest(`/modules/${importForm.moduleId.trim()}/ras/import`, {
        method: 'POST',
        token,
        body: payload,
      })
      pushLog(`RAs guardados en modulo ${importForm.moduleId.trim()}. Total: ${saved.length}.`)
    } catch (error) {
      pushLog(normalizeError(error, 'No se pudieron guardar los RAs importados.'))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="app">
      <header className="header">
        <h1>SARA - Plantilla oficial</h1>
        <p>Flujo: subir PDF/XLSX, extraer con Python, guardar en Spring y mostrar en web.</p>
      </header>

      <nav className="panel nav">
        <button type="button" onClick={() => navigate(ROUTES.login)}>
          Login
        </button>
        <button type="button" onClick={() => navigate(ROUTES.modules)} disabled={!isAuthenticated}>
          Modulos
        </button>
        <button type="button" onClick={() => navigate(ROUTES.imports)} disabled={!isAuthenticated}>
          Importar
        </button>
        <button type="button" onClick={() => navigate(ROUTES.preview)} disabled={!isAuthenticated}>
          Preview
        </button>
      </nav>

      {route === ROUTES.login && (
        <LoginView
          busy={busy}
          isAuthenticated={isAuthenticated}
          email={email}
          password={password}
          setEmail={setEmail}
          setPassword={setPassword}
          onLogin={login}
          onLogout={logout}
        />
      )}

      {route === ROUTES.modules && (
        <ModulesView
          busy={busy}
          isAuthenticated={isAuthenticated}
          newModule={newModule}
          setNewModule={setNewModule}
          onCreateModule={createModule}
          onDeleteModule={deleteModule}
          selectedModuleId={selectedModuleId}
          setSelectedModuleId={setSelectedModuleId}
        />
      )}

      {route === ROUTES.imports && (
        <ImportView
          busy={busy}
          isAuthenticated={isAuthenticated}
          importForm={importForm}
          setImportForm={setImportForm}
          importJobId={importJobId}
          setImportJobId={setImportJobId}
          importJobData={importJobData}
          detectedRas={detectedRas}
          onCreateImportJob={onCreateImportJob}
          onLoadImportJob={onLoadImportJob}
          onUpdateDetectedRa={onUpdateDetectedRa}
          onPersistDetectedRas={onPersistDetectedRas}
        />
      )}

      {route === ROUTES.preview && (
        <PreviewView
          busy={busy}
          isAuthenticated={isAuthenticated}
          moduleId={selectedModuleId}
          setModuleId={setSelectedModuleId}
          previewData={previewData}
          evaluationReports={previewEvaluationReports}
          finalReport={previewFinalReport}
          onLoadPreview={onLoadPreview}
          onDownloadExcel={onDownloadExcel}
          onDownloadBaseTemplate={onDownloadBaseTemplate}
          onDownloadFilledTemplate={onDownloadFilledTemplate}
          onSaveGrades={onSaveGrades}
        />
      )}

      <section className="panel">
        <h2>Estado</h2>
        <p>{busy ? 'Procesando...' : 'Listo'}</p>
        <p>{log || 'Sin eventos.'}</p>
        <p>API: {getApiBaseUrl()}</p>
      </section>
    </div>
  )
}

export default App
