import { useEffect, useMemo, useState } from 'react'

const TABS = {
  ras: 'ras',
  utra: 'utra',
  actividades: 'actividades',
  alumnos: 'alumnos',
  notas: 'notas',
  evaluaciones: 'evaluaciones',
}

const naturalCollator = new Intl.Collator('es', { numeric: true, sensitivity: 'base' })

function compareNatural(a, b) {
  return naturalCollator.compare(String(a ?? ''), String(b ?? ''))
}

function toNumber(value) {
  const n = Number(value)
  return Number.isFinite(n) ? n : null
}

function formatNumber(value, digits = 2) {
  const n = toNumber(value)
  if (n === null) return '-'
  return n.toFixed(digits)
}

function parseInputNumber(raw) {
  const text = String(raw ?? '').trim()
  if (!text) return null
  const parsed = Number(text.replace(',', '.'))
  return Number.isFinite(parsed) ? parsed : null
}

function PreviewView({
  busy,
  isAuthenticated,
  moduleId,
  setModuleId,
  previewData,
  evaluationReports,
  finalReport,
  onLoadPreview,
  onDownloadExcel,
  onDownloadBaseTemplate,
  onDownloadFilledTemplate,
  onSaveGrades,
}) {
  const [activeTab, setActiveTab] = useState(TABS.ras)
  const [exerciseEdits, setExerciseEdits] = useState({})
  const [exerciseStudentId, setExerciseStudentId] = useState('')
  const [exerciseInstrumentId, setExerciseInstrumentId] = useState('')

  const ras = previewData?.ras || []
  const uts = previewData?.uts || []
  const utRaLinks = previewData?.utRaLinks || []
  const activities = previewData?.activities || []
  const instruments = previewData?.instruments || []
  const students = previewData?.students || []
  const grades = previewData?.grades || []

  const sortedRas = useMemo(
    () => [...ras].sort((a, b) => compareNatural(a.code, b.code)),
    [ras],
  )

  const sortedUts = useMemo(
    () =>
      [...uts].sort((a, b) => {
        const periodDiff = (toNumber(a.evaluationPeriod) || 0) - (toNumber(b.evaluationPeriod) || 0)
        if (periodDiff !== 0) return periodDiff
        return compareNatural(a.name, b.name)
      }),
    [uts],
  )

  const sortedStudents = useMemo(
    () => [...students].sort((a, b) => compareNatural(a.studentCode, b.studentCode)),
    [students],
  )

  const utById = useMemo(() => {
    const map = new Map()
    uts.forEach((ut) => map.set(ut.id, ut))
    return map
  }, [uts])

  const activityById = useMemo(() => {
    const map = new Map()
    activities.forEach((activity) => map.set(activity.id, activity))
    return map
  }, [activities])

  const raCodeById = useMemo(() => {
    const map = new Map()
    ras.forEach((ra) => map.set(ra.id, ra.code))
    return map
  }, [ras])

  const utRaPercentByKey = useMemo(() => {
    const map = new Map()
    utRaLinks.forEach((link) => {
      map.set(`${link.utId}-${link.raId}`, link.percent)
    })
    return map
  }, [utRaLinks])

  const gradeByStudentInstrument = useMemo(() => {
    const map = new Map()
    grades.forEach((grade) => {
      map.set(`${grade.studentId}-${grade.instrumentId}`, grade.gradeValue)
    })
    return map
  }, [grades])

  const exerciseByStudentInstrument = useMemo(() => {
    const map = new Map()
    grades.forEach((grade) => {
      const key = `${grade.studentId}-${grade.instrumentId}`
      const byIndex = new Map()
      ;(grade.exerciseGrades || []).forEach((item) => {
        const index = Number(item?.exerciseIndex)
        const value = toNumber(item?.gradeValue)
        if (Number.isFinite(index) && index >= 1 && index <= 10 && value !== null) {
          byIndex.set(index, value)
        }
      })
      map.set(key, byIndex)
    })
    return map
  }, [grades])

  const exerciseWeightByInstrument = useMemo(() => {
    const map = new Map()
    instruments.forEach((instrument) => {
      const byIndex = new Map()
      ;(instrument.exerciseWeights || []).forEach((item) => {
        const index = Number(item?.exerciseIndex)
        const weight = toNumber(item?.weightPercent)
        if (Number.isFinite(index) && index >= 1 && index <= 10 && weight !== null && weight > 0) {
          byIndex.set(index, weight)
        }
      })
      map.set(`${instrument.id}`, byIndex)
    })
    return map
  }, [instruments])

  useEffect(() => {
    setExerciseEdits({})
  }, [grades])

  const sortedInstruments = useMemo(
    () =>
      [...instruments].sort((a, b) => {
        const utA = utById.get(a.utId)
        const utB = utById.get(b.utId)
        const evalDiff = (toNumber(utA?.evaluationPeriod) || 0) - (toNumber(utB?.evaluationPeriod) || 0)
        if (evalDiff !== 0) return evalDiff

        const utNameDiff = compareNatural(utA?.name, utB?.name)
        if (utNameDiff !== 0) return utNameDiff

        const activityA = activityById.get(a.activityId)
        const activityB = activityById.get(b.activityId)
        const activityDiff = compareNatural(activityA?.name, activityB?.name)
        if (activityDiff !== 0) return activityDiff

        return compareNatural(a.name, b.name)
      }),
    [instruments, utById, activityById],
  )

  useEffect(() => {
    if (!sortedStudents.length || !sortedInstruments.length) {
      setExerciseStudentId('')
      setExerciseInstrumentId('')
      return
    }

    const studentExists = sortedStudents.some((item) => String(item.id) === String(exerciseStudentId))
    if (!studentExists) {
      setExerciseStudentId(String(sortedStudents[0].id))
    }

    const instrumentExists = sortedInstruments.some((item) => String(item.id) === String(exerciseInstrumentId))
    if (!instrumentExists) {
      setExerciseInstrumentId(String(sortedInstruments[0].id))
    }
  }, [sortedStudents, sortedInstruments, exerciseStudentId, exerciseInstrumentId])

  const sortedEvaluationReports = useMemo(
    () =>
      (evaluationReports || [])
        .map((report) => ({
          ...report,
          students: [...(report.students || [])].sort((a, b) => compareNatural(a.studentCode, b.studentCode)),
        }))
        .sort((a, b) => (toNumber(a.evaluationPeriod) || 0) - (toNumber(b.evaluationPeriod) || 0)),
    [evaluationReports],
  )

  const sortedFinalRows = useMemo(
    () => [...(finalReport?.students || [])].sort((a, b) => compareNatural(a.studentCode, b.studentCode)),
    [finalReport],
  )

  const raWeightSum = useMemo(
    () => sortedRas.reduce((acc, ra) => acc + (toNumber(ra.weightPercent) || 0), 0),
    [sortedRas],
  )

  const getExerciseInputValue = (studentId, instrumentId, exerciseIndex) => {
    const key = `${studentId}-${instrumentId}-${exerciseIndex}`
    if (Object.prototype.hasOwnProperty.call(exerciseEdits, key)) {
      return exerciseEdits[key]
    }
    const persisted = exerciseByStudentInstrument.get(`${studentId}-${instrumentId}`)?.get(exerciseIndex)
    return persisted == null ? '' : String(persisted)
  }

  const onExerciseInputChange = (studentId, instrumentId, exerciseIndex, value) => {
    const key = `${studentId}-${instrumentId}-${exerciseIndex}`
    setExerciseEdits((prev) => ({ ...prev, [key]: value }))
  }

  const getExerciseValueForComputation = (studentId, instrumentId, exerciseIndex) => {
    const inputKey = `${studentId}-${instrumentId}-${exerciseIndex}`
    if (Object.prototype.hasOwnProperty.call(exerciseEdits, inputKey)) {
      return parseInputNumber(exerciseEdits[inputKey])
    }
    return exerciseByStudentInstrument.get(`${studentId}-${instrumentId}`)?.get(exerciseIndex) ?? null
  }

  const getPreviewAverageByExercises = (studentId, instrumentId) => {
    const weights = exerciseWeightByInstrument.get(`${instrumentId}`) || new Map()
    let weighted = 0
    let totalWeight = 0

    for (let idx = 1; idx <= 10; idx += 1) {
      const weight = toNumber(weights.get(idx)) || 0
      if (weight <= 0) continue
      const grade = getExerciseValueForComputation(studentId, instrumentId, idx)
      weighted += (grade ?? 0) * weight
      totalWeight += weight
    }

    if (totalWeight <= 0) return null
    return weighted / totalWeight
  }

  const editedExercisePairs = useMemo(() => {
    const pairs = new Set()
    Object.keys(exerciseEdits).forEach((key) => {
      const [studentId, instrumentId] = key.split('-')
      if (studentId && instrumentId) {
        pairs.add(`${studentId}-${instrumentId}`)
      }
    })
    return pairs
  }, [exerciseEdits])

  const selectedExercisePreview = useMemo(() => {
    const studentId = Number(exerciseStudentId)
    const instrumentId = Number(exerciseInstrumentId)
    if (!Number.isFinite(studentId) || !Number.isFinite(instrumentId)) return null
    return getPreviewAverageByExercises(studentId, instrumentId)
  }, [exerciseStudentId, exerciseInstrumentId, exerciseEdits, exerciseByStudentInstrument, exerciseWeightByInstrument])

  const selectedPersistedAverage = useMemo(() => {
    const studentId = Number(exerciseStudentId)
    const instrumentId = Number(exerciseInstrumentId)
    if (!Number.isFinite(studentId) || !Number.isFinite(instrumentId)) return null
    return toNumber(gradeByStudentInstrument.get(`${studentId}-${instrumentId}`))
  }, [exerciseStudentId, exerciseInstrumentId, gradeByStudentInstrument])

  const onSaveSelectedExercises = async () => {
    const studentId = Number(exerciseStudentId)
    const instrumentId = Number(exerciseInstrumentId)
    if (!Number.isFinite(studentId) || !Number.isFinite(instrumentId)) {
      window.alert('Selecciona alumno e instrumento.')
      return
    }

    const exerciseGrades = []
    let hasAnyChange = false
    const persistedExercises = exerciseByStudentInstrument.get(`${studentId}-${instrumentId}`) || new Map()

    for (let idx = 1; idx <= 10; idx += 1) {
      const raw = String(getExerciseInputValue(studentId, instrumentId, idx) ?? '').trim()
      const persisted = persistedExercises.get(idx)
      if (!raw) {
        if (persisted != null) hasAnyChange = true
        continue
      }

      const parsed = Number(raw.replace(',', '.'))
      if (!Number.isFinite(parsed) || parsed < 0 || parsed > 10) {
        window.alert(`Valor invalido en ejercicio ${idx}. Debe estar entre 0 y 10.`)
        return
      }

      if (persisted == null || Math.abs(persisted - parsed) >= 0.000001) {
        hasAnyChange = true
      }

      exerciseGrades.push({
        exerciseIndex: idx,
        gradeValue: parsed,
      })
    }

    if (!hasAnyChange) {
      window.alert('No hay cambios de ejercicios para guardar.')
      return
    }

    const persistedMainGrade = toNumber(gradeByStudentInstrument.get(`${studentId}-${instrumentId}`))
    const saved = await onSaveGrades([
      {
        studentId,
        instrumentId,
        gradeValue: persistedMainGrade,
        exerciseGrades,
      },
    ])
    if (saved) {
      setExerciseEdits({})
    }
  }

  return (
    <section className="panel">
      <h2>Vista previa Excel por modulo</h2>
      <form className="inline-actions" onSubmit={onLoadPreview}>
        <label>
          Modulo (ID)
          <input value={moduleId} onChange={(e) => setModuleId(e.target.value)} placeholder="Ej: 1" required />
        </label>
        <button type="submit" disabled={busy || !isAuthenticated}>
          Cargar vista previa
        </button>
        <button type="button" onClick={onDownloadExcel} disabled={busy || !isAuthenticated || !String(moduleId || '').trim()}>
          Descargar Excel
        </button>
        <button type="button" onClick={onDownloadBaseTemplate} disabled={busy || !isAuthenticated}>
          Descargar plantilla base
        </button>
        <button type="button" onClick={onDownloadFilledTemplate} disabled={busy || !isAuthenticated}>
          Descargar plantilla rellenada
        </button>
      </form>

      {previewData && (
        <div className="report-wrap">
          <p>
            <strong>Modulo:</strong> #{previewData.moduleId} {previewData.moduleName} ({previewData.academicYear})
          </p>
          <p>
            <strong>Docente:</strong> {previewData.teacherName || '-'}
          </p>
          <p>
            <strong>Resumen:</strong> {sortedRas.length} RAs, {sortedUts.length} UTs, {activities.length} actividades,{' '}
            {sortedInstruments.length} instrumentos, {sortedStudents.length} alumnos, {grades.length} notas.
          </p>
        </div>
      )}

      {!previewData && <p>Carga un modulo para ver las tablas y vistas tipo Excel.</p>}

      {previewData && (
        <>
          <div className="tabs">
            <button type="button" className={activeTab === TABS.ras ? 'tab-active' : ''} onClick={() => setActiveTab(TABS.ras)}>
              Datos Iniciales - Tabla 1 (RAs)
            </button>
            <button type="button" className={activeTab === TABS.utra ? 'tab-active' : ''} onClick={() => setActiveTab(TABS.utra)}>
              Datos Iniciales - Tabla 2 (UT-RA)
            </button>
            <button
              type="button"
              className={activeTab === TABS.actividades ? 'tab-active' : ''}
              onClick={() => setActiveTab(TABS.actividades)}
            >
              Datos Iniciales - Tabla 3 (Actividades)
            </button>
            <button
              type="button"
              className={activeTab === TABS.alumnos ? 'tab-active' : ''}
              onClick={() => setActiveTab(TABS.alumnos)}
            >
              Datos Iniciales - Alumnos
            </button>
            <button type="button" className={activeTab === TABS.notas ? 'tab-active' : ''} onClick={() => setActiveTab(TABS.notas)}>
              Actividades (Notas)
            </button>
            <button
              type="button"
              className={activeTab === TABS.evaluaciones ? 'tab-active' : ''}
              onClick={() => setActiveTab(TABS.evaluaciones)}
            >
              Evaluaciones
            </button>
          </div>

          {activeTab === TABS.ras && (
            <div className="report-wrap">
              <h3>Tabla 1: Resultados de aprendizaje</h3>
              <div className="table-scroll">
                <table>
                  <thead>
                    <tr>
                      <th>Codigo</th>
                      <th>Nombre</th>
                      <th>Peso %</th>
                    </tr>
                  </thead>
                  <tbody>
                    {sortedRas.map((ra) => (
                      <tr key={ra.id}>
                        <td>{ra.code}</td>
                        <td>{ra.name}</td>
                        <td>{formatNumber(ra.weightPercent)}</td>
                      </tr>
                    ))}
                    <tr>
                      <td colSpan={2}>
                        <strong>Total</strong>
                      </td>
                      <td>
                        <strong>{formatNumber(raWeightSum)}</strong>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {activeTab === TABS.utra && (
            <div className="report-wrap">
              <h3>Tabla 2: Distribucion UT-RA</h3>
              <div className="table-scroll">
                <table>
                  <thead>
                    <tr>
                      <th>UT</th>
                      <th>Evaluacion</th>
                      {sortedRas.map((ra) => (
                        <th key={`ra-head-${ra.id}`}>{ra.code}</th>
                      ))}
                      <th>Total %</th>
                    </tr>
                  </thead>
                  <tbody>
                    {sortedUts.map((ut) => {
                      const total = sortedRas.reduce(
                        (acc, ra) => acc + (toNumber(utRaPercentByKey.get(`${ut.id}-${ra.id}`)) || 0),
                        0,
                      )
                      return (
                        <tr key={ut.id}>
                          <td>{ut.name}</td>
                          <td>{ut.evaluationPeriod}</td>
                          {sortedRas.map((ra) => (
                            <td key={`${ut.id}-${ra.id}`}>{formatNumber(utRaPercentByKey.get(`${ut.id}-${ra.id}`))}</td>
                          ))}
                          <td>{formatNumber(total)}</td>
                        </tr>
                      )
                    })}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {activeTab === TABS.actividades && (
            <div className="report-wrap">
              <h3>Tabla 3: Actividades e instrumentos</h3>
              <div className="table-scroll">
                <table>
                  <thead>
                    <tr>
                      <th>UT</th>
                      <th>Evaluacion</th>
                      <th>Actividad</th>
                      <th>Instrumento</th>
                      <th>Peso %</th>
                      <th>RAs</th>
                    </tr>
                  </thead>
                  <tbody>
                    {sortedInstruments.map((instrument) => {
                      const activity = activityById.get(instrument.activityId)
                      const ut = utById.get(instrument.utId)
                      const raCodes = (instrument.raIds || [])
                        .map((raId) => raCodeById.get(raId))
                        .filter(Boolean)
                        .sort(compareNatural)
                        .join(', ')
                      return (
                        <tr key={instrument.id}>
                          <td>{ut?.name || '-'}</td>
                          <td>{ut?.evaluationPeriod || '-'}</td>
                          <td>{activity?.name || '-'}</td>
                          <td>{instrument.name}</td>
                          <td>{formatNumber(instrument.weightPercent)}</td>
                          <td>{raCodes || '-'}</td>
                        </tr>
                      )
                    })}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {activeTab === TABS.alumnos && (
            <div className="report-wrap">
              <h3>Alumnos del modulo</h3>
              <div className="table-scroll">
                <table>
                  <thead>
                    <tr>
                      <th>Codigo</th>
                      <th>Nombre</th>
                    </tr>
                  </thead>
                  <tbody>
                    {sortedStudents.map((student) => (
                      <tr key={student.id}>
                        <td>{student.studentCode}</td>
                        <td>{student.fullName}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {activeTab === TABS.notas && (
            <div className="report-wrap">
              <h3>Hoja Actividades: notas por instrumento</h3>
              <div className="exercise-editor">
                <h4>Editar ejercicios por alumno/instrumento</h4>
                <form
                  className="exercise-form"
                  onSubmit={(e) => {
                    e.preventDefault()
                    onSaveSelectedExercises()
                  }}
                >
                  <label className="exercise-field">
                    Alumno
                    <select
                      value={exerciseStudentId}
                      onChange={(e) => setExerciseStudentId(e.target.value)}
                      disabled={busy || !isAuthenticated || sortedStudents.length === 0}
                    >
                      {sortedStudents.map((student) => (
                        <option key={`exercise-student-${student.id}`} value={student.id}>
                          {student.studentCode} - {student.fullName}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label className="exercise-field">
                    Instrumento
                    <select
                      value={exerciseInstrumentId}
                      onChange={(e) => setExerciseInstrumentId(e.target.value)}
                      disabled={busy || !isAuthenticated || sortedInstruments.length === 0}
                    >
                      {sortedInstruments.map((instrument) => (
                        <option key={`exercise-instrument-${instrument.id}`} value={instrument.id}>
                          {instrument.name}
                        </option>
                      ))}
                    </select>
                  </label>
                  <button
                    type="submit"
                    className="exercise-submit"
                    disabled={busy || !isAuthenticated || !exerciseStudentId || !exerciseInstrumentId}
                  >
                    Guardar ejercicios
                  </button>
                </form>

                {exerciseStudentId && exerciseInstrumentId && (
                  <p className="exercise-preview">
                    Nota media (guardada -&gt; previa):{' '}
                    <strong>
                      {formatNumber(selectedPersistedAverage)} -&gt; {formatNumber(selectedExercisePreview)}
                    </strong>
                  </p>
                )}

                {exerciseStudentId && exerciseInstrumentId && (
                  <div className="table-scroll">
                    <table className="exercise-table">
                      <thead>
                        <tr>
                          {[1, 2, 3, 4, 5, 6, 7, 8, 9, 10].map((idx) => (
                            <th key={`exercise-head-${idx}`}>Ej{idx}</th>
                          ))}
                        </tr>
                      </thead>
                      <tbody>
                        <tr>
                          {[1, 2, 3, 4, 5, 6, 7, 8, 9, 10].map((idx) => (
                            <td key={`exercise-cell-${idx}`}>
                              <input
                                type="number"
                                min="0"
                                max="10"
                                step="0.01"
                                value={getExerciseInputValue(exerciseStudentId, exerciseInstrumentId, idx)}
                                onChange={(e) =>
                                  onExerciseInputChange(exerciseStudentId, exerciseInstrumentId, idx, e.target.value)
                                }
                                disabled={busy || !isAuthenticated}
                              />
                            </td>
                          ))}
                        </tr>
                      </tbody>
                    </table>
                  </div>
                )}
              </div>

              <div className="report-wrap">
                <h4>Vista de nota media por actividad (solo lectura)</h4>
                <div className="table-scroll">
                  <table>
                    <thead>
                      <tr>
                        <th>Alumno</th>
                        {sortedInstruments.map((instrument) => (
                          <th key={`instrument-head-${instrument.id}`}>{instrument.name}</th>
                        ))}
                      </tr>
                    </thead>
                    <tbody>
                      {sortedStudents.map((student) => (
                        <tr key={student.id}>
                          <td>
                            {student.studentCode} - {student.fullName}
                          </td>
                          {sortedInstruments.map((instrument) => (
                            <td key={`${student.id}-${instrument.id}`}>
                              {(() => {
                                const pairKey = `${student.id}-${instrument.id}`
                                const persisted = toNumber(gradeByStudentInstrument.get(pairKey))
                                const preview = getPreviewAverageByExercises(student.id, instrument.id)
                                const hasLiveEdit = editedExercisePairs.has(pairKey)
                                if (hasLiveEdit && preview !== null) {
                                  return `${formatNumber(persisted)} -> ${formatNumber(preview)}`
                                }
                                return formatNumber(persisted)
                              })()}
                            </td>
                          ))}
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}

          {activeTab === TABS.evaluaciones && (
            <div className="report-wrap">
              <h3>Hoja Evaluaciones</h3>
              {sortedEvaluationReports.length === 0 && !finalReport && (
                <p>No hay reportes de evaluacion/final disponibles. Revisa consistencia del modulo.</p>
              )}

              {sortedEvaluationReports.map((report) => (
                <div key={`evaluation-${report.evaluationPeriod}`} className="report-wrap">
                  <h4>Evaluacion {report.evaluationPeriod}</h4>
                  <div className="table-scroll">
                    <table>
                      <thead>
                        <tr>
                          <th>Codigo</th>
                          <th>Alumno</th>
                          <th>Nota numerica</th>
                          <th>Boletin sugerido</th>
                          <th>RAs superados</th>
                        </tr>
                      </thead>
                      <tbody>
                        {report.students.map((row) => (
                          <tr key={row.studentId}>
                            <td>{row.studentCode}</td>
                            <td>{row.studentName}</td>
                            <td>{formatNumber(row.numericGrade, 4)}</td>
                            <td>{row.suggestedBulletinGrade}</td>
                            <td>{row.allRAsPassed ? 'SI' : 'NO'}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              ))}

              {finalReport && (
                <div className="report-wrap">
                  <h4>Final</h4>
                  <div className="table-scroll">
                    <table>
                      <thead>
                        <tr>
                          <th>Codigo</th>
                          <th>Alumno</th>
                          <th>Nota final</th>
                        </tr>
                      </thead>
                      <tbody>
                        {sortedFinalRows.map((row) => (
                          <tr key={row.studentId}>
                            <td>{row.studentCode}</td>
                            <td>{row.studentName}</td>
                            <td>{formatNumber(row.finalGrade, 4)}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}
            </div>
          )}

        </>
      )}
    </section>
  )
}

export default PreviewView
