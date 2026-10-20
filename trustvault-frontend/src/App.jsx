
import { useEffect, useState } from 'react'
import './App.css'

function App() {
  const [backendStatus, setBackendStatus] = useState('Checking connection...')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [loginMessage, setLoginMessage] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  useEffect(() => {
    fetch('http://localhost:8080/api/health')
      .then((response) => {
        if (!response.ok) throw new Error('Backend request failed')
        return response.text()
      })
      .then(() => setBackendStatus('Backend connected'))
      .catch(() => setBackendStatus('Backend unavailable'))
  }, [])

  async function handleLogin(event) {
    event.preventDefault()
    setIsSubmitting(true)
    setLoginMessage('')

    try {
      const csrfResponse = await fetch('http://localhost:8080/api/csrf', {
        credentials: 'include',
      })

      if (!csrfResponse.ok) {
        throw new Error('Could not obtain security token')
      }

      const csrf = await csrfResponse.json()

      const body = new URLSearchParams()
      body.set('username', email)
      body.set('password', password)

      const response = await fetch('http://localhost:8080/login', {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded',
          [csrf.headerName]: csrf.token,
        },
        body,
      })

      const result = await response.json()

      if (!response.ok) {
        setLoginMessage(result.message || 'Login failed')
        return
      }

      const userResponse = await fetch('http://localhost:8080/api/me', {
        credentials: 'include',
      })

      if (!userResponse.ok) {
        throw new Error('Login succeeded, but user verification failed')
      }

      const user = await userResponse.text()
      setLoginMessage(user)
      setPassword('')
    } catch (error) {
      setLoginMessage(error.message || 'Unable to connect to the backend')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <main className="login-page">
      <section className="login-card">
        <div className="brand">
          <div className="brand-icon">T</div>
          <h1>TrustVault</h1>
        </div>

        <p className="subtitle">
          Secure document sharing. Complete accountability.
        </p>

        <h2>Welcome back</h2>
        <p className="description">
          Sign in to access your secure workspace.
        </p>

        <form onSubmit={handleLogin}>
          <label htmlFor="email">Email address</label>
          <input
            id="email"
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            placeholder="you@example.com"
            autoComplete="username"
            required
          />

          <label htmlFor="password">Password</label>
          <input
            id="password"
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            placeholder="Enter your password"
            autoComplete="current-password"
            required
          />

          <button type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Signing in...' : 'Sign in'}
          </button>
        </form>

        <p className="security-note">
          Protected access · Auditable activity
        </p>

        <p className="connection-status" role="status">
          {backendStatus}
        </p>

        {loginMessage && (
          <p className="connection-status" role="status">
            {loginMessage}
          </p>
        )}
      </section>
    </main>
  )
}

export default App
