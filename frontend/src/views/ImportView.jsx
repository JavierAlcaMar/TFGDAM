function ImportView({
  busy,
  isAuthenticated,
  importForm,
  setImportForm,
  importJobId,
  setImportJobId,
  importJobData,
  detectedRas,
  onCreateImportJob,
  onLoadImportJob,
  onUpdateDetectedRa,
  onPersistDetectedRas,
}) {
  return (
    <section className="panel">
      <h2>Importacion PDF/XLSX</h2>
      <form className="grid two" onSubmit={onCreateImportJob}>
        <label>
          Modulo (ID)
          <input
            value={importForm.moduleId}
            onChange={(e) => setImportForm((p) => ({ ...p, moduleId: e.target.value }))}
            placeholder="Ej: 1"
          />
        </label>
        <label>
          Archivo
          <input
            type="file"
            accept=".pdf,.xlsx"
            onChange={(e) => setImportForm((p) => ({ ...p, file: e.target.files?.[0] || null }))}
            required
          />
        </label>
        <button type="submit" disabled={busy || !isAuthenticated}>
          Subir y extraer
        </button>
      </form>
      <p>
        PDF: requiere modulo ID y crea un job de extraccion de RAs. XLSX: si indicas modulo ID existente lo reemplaza;
        si lo dejas vacio o no existe crea un modulo nuevo.
      </p>

      <form className="inline-actions" onSubmit={onLoadImportJob}>
        <input
          placeholder="Job ID"
          value={importJobId}
          onChange={(e) => setImportJobId(e.target.value)}
          required
        />
        <button type="submit" disabled={busy || !isAuthenticated}>
          Consultar job
        </button>
      </form>

      {importJobData && <pre className="console">{JSON.stringify(importJobData, null, 2)}</pre>}

      {detectedRas.length > 0 && (
        <div className="report-wrap">
          <h3>RAs detectados</h3>
          <table>
            <thead>
              <tr>
                <th>Codigo</th>
                <th>Nombre</th>
                <th>Peso %</th>
              </tr>
            </thead>
            <tbody>
              {detectedRas.map((ra) => (
                <tr key={ra.id}>
                  <td>
                    <input value={ra.code} onChange={(e) => onUpdateDetectedRa(ra.id, 'code', e.target.value)} />
                  </td>
                  <td>
                    <input value={ra.name} onChange={(e) => onUpdateDetectedRa(ra.id, 'name', e.target.value)} />
                  </td>
                  <td>
                    <input
                      type="number"
                      min="0"
                      max="100"
                      step="0.01"
                      value={ra.weightPercent}
                      onChange={(e) => onUpdateDetectedRa(ra.id, 'weightPercent', e.target.value)}
                    />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="inline-actions">
            <button type="button" onClick={onPersistDetectedRas} disabled={busy || !isAuthenticated}>
              Guardar RAs en backend
            </button>
          </div>
        </div>
      )}
    </section>
  )
}

export default ImportView
