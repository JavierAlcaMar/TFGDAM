function ModulesView({
  busy,
  isAuthenticated,
  newModule,
  setNewModule,
  onCreateModule,
  onDeleteModule,
  selectedModuleId,
  setSelectedModuleId,
}) {
  return (
    <section className="panel">
      <h2>Modulos</h2>
      <form className="grid two" onSubmit={onCreateModule}>
        <label>
          Nombre modulo
          <input
            value={newModule.name}
            onChange={(e) => setNewModule((p) => ({ ...p, name: e.target.value }))}
            required
          />
        </label>
        <label>
          Curso academico
          <input
            value={newModule.academicYear}
            onChange={(e) => setNewModule((p) => ({ ...p, academicYear: e.target.value }))}
            required
          />
        </label>
        <label>
          Docente
          <input
            value={newModule.teacherName}
            onChange={(e) => setNewModule((p) => ({ ...p, teacherName: e.target.value }))}
            required
          />
        </label>
        <button type="submit" disabled={busy || !isAuthenticated}>
          Crear modulo
        </button>
      </form>

      <div className="inline-actions">
        <label>
          Modulo activo (ID)
          <input
            value={selectedModuleId}
            onChange={(e) => setSelectedModuleId(e.target.value)}
            placeholder="Ej: 1"
          />
        </label>
        <button
          type="button"
          onClick={onDeleteModule}
          disabled={busy || !isAuthenticated || !String(selectedModuleId || '').trim()}
        >
          Eliminar modulo
        </button>
      </div>
    </section>
  )
}

export default ModulesView
