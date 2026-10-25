
export default function LoginPage({
  email,
  setEmail,
  password,
  setPassword,
  handleLogin,
  isSubmitting,
  backendStatus,
  loginMessage,
}) {
  return (
    <main className="login-page">
      <section className="login-hero">
        <div className="login-brand">
          <span className="login-brand-icon">T</span>
          <span>TrustVault</span>
        </div>

        <div className="login-hero-copy">
          <p className="eyebrow">SECURE DOCUMENT WORKSPACE</p>
          <h1>The right people.<br />Provably.</h1>
          <p>
            Every document reachable only by those granted access.
            Every open, download and refused attempt on the record.
          </p>
        </div>

        <p className="login-hero-footer">
          Access revoked means access ended, immediately.
        </p>
      </section>

      <section className="login-panel">
        <form className="login-form" onSubmit={handleLogin}>
          <p className="eyebrow">WELCOME BACK</p>
          <h2>Sign in</h2>
          <p className="login-description">
            Use your TrustVault account to continue.
          </p>

          <label htmlFor="login-email">Email</label>
          <input
            id="login-email"
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            autoComplete="username"
            required
          />

          <label htmlFor="login-password">Password</label>
          <input
            id="login-password"
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            autoComplete="current-password"
            required
          />

          <button type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Signing in…' : 'Continue'}
          </button>

          <p className="connection-status" role="status">
            {backendStatus}
          </p>

          {loginMessage && (
            <p className="login-message" role="status">
              {loginMessage}
            </p>
          )}
        </form>
      </section>
    </main>
  )
}
