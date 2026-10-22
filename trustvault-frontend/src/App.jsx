
import { useCallback, useEffect, useState } from 'react'
import './App.css'

const API = 'http://localhost:8080'

async function request(path, options = {}) {
  const response = await fetch(`${API}${path}`, {
    credentials: 'include',
    ...options,
  })

  if (!response.ok) {
    const message = await response.text()
    throw new Error(message || `Request failed (${response.status})`)
  }

  if (response.status === 204) return null

  const contentType = response.headers.get('content-type') || ''
  return contentType.includes('application/json')
    ? response.json()
    : response.text()
}

async function csrfToken() {
  return request('/api/csrf')
}

async function protectedRequest(path, options = {}) {
  const csrf = await csrfToken()
  return request(path, {
    ...options,
    headers: {
      ...(options.headers || {}),
      [csrf.headerName]: csrf.token,
    },
  })
}

function App() {
  const [backendStatus, setBackendStatus] = useState('Checking backend...')
  const [user, setUser] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [loginMessage, setLoginMessage] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const [documents, setDocuments] = useState([])
  const [title, setTitle] = useState('')
  const [category, setCategory] = useState('')
  const [file, setFile] = useState(null)
  const [searchTitle, setSearchTitle] = useState('')
  const [searchCategory, setSearchCategory] = useState('')
  const [searchOwner, setSearchOwner] = useState('')
  const [message, setMessage] = useState('')
  const [busy, setBusy] = useState(false)
  const [auditLogs, setAuditLogs] = useState([])
  const [showAudit, setShowAudit] = useState(false)
  const [sharingDocumentId, setSharingDocumentId] = useState('')
const [sharingEmail, setSharingEmail] = useState('')
const [sharingLevel, setSharingLevel] = useState('READ')
const [sharingBusy, setSharingBusy] = useState(false)

  const isAdmin = user.includes('ROLE_ADMIN') || user.includes('ADMIN')

  useEffect(() => {
    fetch(`${API}/api/health`)
      .then((response) => {
        if (!response.ok) throw new Error()
        setBackendStatus('Backend connected')
      })
      .catch(() => setBackendStatus('Backend unavailable'))
  }, [])

  const loadDocuments = useCallback(async () => {
    try {
      const result = await request('/api/documents')
      setDocuments(result)
    } catch (error) {
      setMessage(`Could not load documents: ${error.message}`)
    }
  }, [])

  async function handleLogin(event) {
    event.preventDefault()
    setIsSubmitting(true)
    setLoginMessage('')

    try {
      const csrf = await csrfToken()
      const body = new URLSearchParams({ username: email, password })

      const response = await fetch(`${API}/login`, {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded',
          [csrf.headerName]: csrf.token,
        },
        body,
      })

      if (!response.ok) {
        setLoginMessage('Login failed. Check your credentials.')
        return
      }

      const currentUser = await request('/api/me')
      setUser(currentUser)
      setPassword('')
      setLoginMessage('')
      await loadDocuments()
    } catch (error) {
      setLoginMessage(error.message || 'Unable to connect to backend')
    } finally {
      setIsSubmitting(false)
    }
  }

  async function uploadDocument(event) {
    event.preventDefault()
    if (!file) {
      setMessage('Choose a file to upload.')
      return
    }

    setBusy(true)
    setMessage('')

    try {
      const form = new FormData()
      form.append('title', title)
      form.append('category', category)
      form.append('file', file)

      await protectedRequest('/api/documents', {
        method: 'POST',
        body: form,
      })

      setTitle('')
      setCategory('')
      setFile(null)
      event.target.reset()
      setMessage('Document uploaded successfully.')
      await loadDocuments()
    } catch (error) {
      setMessage(`Upload failed: ${error.message}`)
    } finally {
      setBusy(false)
    }
  }

  async function searchDocuments(event) {
    event.preventDefault()
    const params = new URLSearchParams()

    if (searchTitle.trim()) params.set('title', searchTitle.trim())
    if (searchCategory.trim()) params.set('category', searchCategory.trim())
    if (searchOwner.trim()) params.set('ownerEmail', searchOwner.trim())

    setBusy(true)
    setMessage('')

    try {
      const query = params.toString()
      const result = await request(
        `/api/documents/search${query ? `?${query}` : ''}`,
      )
      setDocuments(result)
    } catch (error) {
      setMessage(`Search failed: ${error.message}`)
    } finally {
      setBusy(false)
    }
  }

  async function downloadDocument(document) {
    setMessage('')

    try {
      const response = await fetch(
        `${API}/api/documents/${document.id}/download`,
        { credentials: 'include' },
      )

      if (!response.ok) {
        throw new Error(`Download failed (${response.status})`)
      }

      const blob = await response.blob()
      const url = URL.createObjectURL(blob)
      const link = window.document.createElement('a')
      link.href = url
      link.download = document.originalFilename || document.title
      link.click()
      URL.revokeObjectURL(url)
    } catch (error) {
      setMessage(error.message)
    }
  }

  async function deleteDocument(document) {
    if (!window.confirm(`Delete "${document.title}" permanently?`)) return

    setBusy(true)
    setMessage('')

    try {
      await protectedRequest(`/api/documents/${document.id}`, {
        method: 'DELETE',
      })
      setMessage('Document deleted.')
      await loadDocuments()
    } catch (error) {
      setMessage(`Deletion failed: ${error.message}`)
    } finally {
      setBusy(false)
    }
  }

  async function loadAuditLogs() {
    try {
      const logs = await request('/api/admin/audit-logs')
      setAuditLogs(logs)
      setShowAudit(true)
      setMessage('')
    } catch (error) {
      setMessage(`Could not load audit logs: ${error.message}`)
    }
  }

  async function logout() {
    try {
      const csrf = await csrfToken()
      await fetch(`${API}/logout`, {
        method: 'POST',
        credentials: 'include',
        headers: { [csrf.headerName]: csrf.token },
      })
    } finally {
      setUser('')
      setDocuments([])
      setAuditLogs([])
      setShowAudit(false)
      setMessage('')
      setLoginMessage('You have signed out.')
    }
  }

async function grantDocumentPermission(event) {
  event.preventDefault()

  if (!sharingDocumentId || !sharingEmail.trim()) {
    setMessage('Select a document and enter the recipient email.')
    return
  }

  setSharingBusy(true)
  setMessage('')

  try {
    const result = await protectedRequest(
      `/api/documents/${sharingDocumentId}/permissions`,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          email: sharingEmail.trim(),
          permissionLevel: sharingLevel,
        }),
      },
    )

    setMessage(
      `Permission ${result.permissionLevel} granted to ${result.email}.`,
    )
  } catch (error) {
    setMessage(`Could not grant permission: ${error.message}`)
  } finally {
    setSharingBusy(false)
  }
}

async function revokeDocumentPermission(event) {
  event.preventDefault()

  if (!sharingDocumentId || !sharingEmail.trim()) {
    setMessage('Select a document and enter the recipient email.')
    return
  }

  if (!window.confirm(
    `Revoke access for ${sharingEmail.trim()} to this document?`,
  )) {
    return
  }

  setSharingBusy(true)
  setMessage('')

  try {
    const params = new URLSearchParams({ email: sharingEmail.trim() })

    await protectedRequest(
      `/api/documents/${sharingDocumentId}/permissions?${params}`,
      { method: 'DELETE' },
    )

    setMessage(`Permission revoked for ${sharingEmail.trim()}.`)
  } catch (error) {
    setMessage(`Could not revoke permission: ${error.message}`)
  } finally {
    setSharingBusy(false)
  }
}

  if (!user) {
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
          <p className="description">Sign in to your secure workspace.</p>

          <form onSubmit={handleLogin}>
            <label htmlFor="email">Email address</label>
            <input
              id="email"
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              autoComplete="username"
              required
            />
            <label htmlFor="password">Password</label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              autoComplete="current-password"
              required
            />
            <button type="submit" disabled={isSubmitting}>
              {isSubmitting ? 'Signing in...' : 'Sign in'}
            </button>
          </form>

          <p className="connection-status" role="status">{backendStatus}</p>
          {loginMessage && <p role="status">{loginMessage}</p>}
        </section>
      </main>
    )
  }

  return (
    <main className="workspace">
      <header className="workspace-header">
        <div className="brand">
          <div className="brand-icon">T</div>
          <div>
            <h1>TrustVault</h1>
            <p className="subtitle">Secure document workspace</p>
          </div>
        </div>
        <div className="session-actions">
          <span>{user}</span>
          <button type="button" onClick={logout}>Sign out</button>
        </div>
      </header>

      <section className="workspace-content">
        <h2>Your documents</h2>
        <p className="description">
          Upload and manage documents you own or have permission to access.
        </p>

        <form className="panel" onSubmit={uploadDocument}>
          <h3>Upload document</h3>
          <label htmlFor="doc-title">Title</label>
          <input
            id="doc-title"
            value={title}
            onChange={(event) => setTitle(event.target.value)}
            maxLength={200}
            required
          />
          <label htmlFor="doc-category">Category</label>
          <input
            id="doc-category"
            value={category}
            onChange={(event) => setCategory(event.target.value)}
            maxLength={100}
            required
          />
          <label htmlFor="doc-file">File (PDF, PNG, or JPEG supported by backend)</label>
          <input
            id="doc-file"
            type="file"
            accept=".pdf,.png,.jpg,.jpeg"
            onChange={(event) => setFile(event.target.files?.[0] || null)}
            required
          />
          <button disabled={busy} type="submit">
            {busy ? 'Please wait...' : 'Upload document'}
          </button>
        </form>

<section className="panel">
  <h3>Share a document</h3>
  <p className="description">
    Grant or revoke access to a document you own.
  </p>

  <form onSubmit={grantDocumentPermission}>
    <label htmlFor="sharing-document">Document</label>
    <select
      id="sharing-document"
      value={sharingDocumentId}
      onChange={(event) => setSharingDocumentId(event.target.value)}
      required
    >
      <option value="">Select a document</option>
      {documents
        .filter((doc) => doc.ownerEmail.toLowerCase() === email.toLowerCase())
        .map((doc) => (
          <option key={doc.id} value={doc.id}>
            {doc.title}
          </option>
        ))}
    </select>

    <label htmlFor="sharing-email">Recipient email</label>
    <input
      id="sharing-email"
      type="email"
      value={sharingEmail}
      onChange={(event) => setSharingEmail(event.target.value)}
      required
    />

    <label htmlFor="sharing-level">Permission level</label>
    <select
      id="sharing-level"
      value={sharingLevel}
      onChange={(event) => setSharingLevel(event.target.value)}
    >
      <option value="READ">Read only</option>
      <option value="EDIT">Read and edit</option>
    </select>

    <div className="button-row">
      <button type="submit" disabled={sharingBusy}>
        {sharingBusy ? 'Please wait...' : 'Grant access'}
      </button>

      <button
        type="button"
        className="danger-button"
        disabled={sharingBusy}
        onClick={revokeDocumentPermission}
      >
        Revoke access
      </button>
    </div>
  </form>
</section>

        <form className="panel search-panel" onSubmit={searchDocuments}>
          <h3>Search documents</h3>
          <label htmlFor="search-title">Title</label>
          <input
            id="search-title"
            value={searchTitle}
            onChange={(event) => setSearchTitle(event.target.value)}
          />
          <label htmlFor="search-category">Category</label>
          <input
            id="search-category"
            value={searchCategory}
            onChange={(event) => setSearchCategory(event.target.value)}
          />
          <label htmlFor="search-owner">Owner email</label>
          <input
            id="search-owner"
            type="email"
            value={searchOwner}
            onChange={(event) => setSearchOwner(event.target.value)}
          />
          <div className="button-row">
            <button disabled={busy} type="submit">Search</button>
            <button
              type="button"
              className="secondary-button"
              onClick={() => {
                setSearchTitle('')
                setSearchCategory('')
                setSearchOwner('')
                loadDocuments()
              }}
            >
              Clear filters
            </button>
          </div>
        </form>

        {message && <p className="notice" role="status">{message}</p>}

        <section className="panel">
          <div className="section-heading">
            <h3>Accessible documents</h3>
            <button type="button" onClick={loadDocuments}>Refresh</button>
          </div>
          {documents.length === 0 ? (
            <p className="description">No documents found.</p>
          ) : (
            <div className="table-wrapper">
              <table>
                <thead>
                  <tr>
                    <th>Title</th>
                    <th>Category</th>
                    <th>Owner</th>
                    <th>Updated</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {documents.map((doc) => (
                    <tr key={doc.id}>
                      <td>{doc.title}</td>
                      <td>{doc.category}</td>
                      <td>{doc.ownerEmail}</td>
                      <td>{doc.updatedAt ? new Date(doc.updatedAt).toLocaleString() : '—'}</td>
                      <td>
                        <div className="button-row">
                          <button type="button" onClick={() => downloadDocument(doc)}>
                            Download
                          </button>
                          <button
                            type="button"
                            className="danger-button"
                            onClick={() => deleteDocument(doc)}
                          >
                            Delete
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>

        {isAdmin && (
          <section className="panel">
            <div className="section-heading">
              <h3>Administrator audit logs</h3>
              <button type="button" onClick={loadAuditLogs}>Load audit logs</button>
            </div>
            {showAudit && (
              <div className="table-wrapper">
                <table>
                  <thead>
                    <tr>
                      <th>Time</th>
                      <th>Actor</th>
                      <th>Action</th>
                      <th>Document</th>
                      <th>Outcome</th>
                    </tr>
                  </thead>
                  <tbody>
                    {auditLogs.map((log, index) => (
                      <tr key={log.id ?? index}>
                        <td>{log.occurredAt ? new Date(log.occurredAt).toLocaleString() : '—'}</td>
                        <td>{log.actorEmail}</td>
                        <td>{log.action}</td>
                        <td>{log.documentId ?? '—'}</td>
                        <td>{log.outcome}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
        )}
      </section>
    </main>
  )
}

export default App
