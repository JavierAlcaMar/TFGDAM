function LoginView({
  busy,
  isAuthenticated,
  email,
  password,
  setEmail,
  setPassword,
  onLogin,
  onLogout,
}) {
  return (
    <section className="panel">
      <h2>Login</h2>
      <form className="grid two" onSubmit={onLogin}>
        <label>
          Email
          <input value={email} onChange={(e) => setEmail(e.target.value)} required />
        </label>
        <label>
          Password
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        </label>
        <button type="submit" disabled={busy}>
          Entrar
        </button>
        <button type="button" onClick={onLogout} disabled={busy || !isAuthenticated}>
          Salir
        </button>
      </form>
    </section>
  )
}

export default LoginView
