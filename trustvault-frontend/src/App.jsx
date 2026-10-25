
import { useCallback, useEffect, useState } from 'react'

import {
  API,
  request,
  csrfToken,
  protectedRequest,
} from './api/trustVaultApi'

import './Styles/tokens.css'
import './Styles/global.css'
import './Styles/layout.css'

import LoginPage from './pages/LoginPage'
import DocumentsPage from './pages/DocumentsPage'
import UploadPage from './pages/UploadPage'
import SharedDocumentsPage from './pages/SharedDocumentsPage'
import DocumentDetailsPage from './pages/DocumentDetailsPage'
import AuditPage from './pages/AuditPage'
import AdminPage from './pages/AdminPage'
import AccessDeniedPage from './pages/AccessDeniedPage'

import Sidebar from './components/Sidebar'
import Brand from './components/Brand'

function hasAdminRole(currentUser) {
  if (!currentUser) return false

  const roleValues = []

  if (typeof currentUser === 'string') {
    roleValues.push(currentUser)
  } else if (typeof currentUser === 'object') {
    roleValues.push(
      currentUser.role,
      currentUser.authorities,
      currentUser.roles,
      currentUser.authority,
    )
  }

  const rolesText = roleValues
    .flat(Infinity)
    .filter(Boolean)
    .join(' ')
    .toUpperCase()

  return (
    rolesText.includes('ROLE_ADMIN') ||
    /\bADMIN\b/.test(rolesText)
  )
}

function setDocumentIdInUrl(documentId, replace = false) {
  const url = new URL(window.location.href)
  if (documentId == null || documentId === '') {
    url.searchParams.delete('documentId')
  } else {
    url.searchParams.set('documentId', String(documentId))
  }

  const method = replace ? 'replaceState' : 'pushState'
  window.history[method]({}, '', `${url.pathname}${url.search}${url.hash}`)
}

function App() {
  const [user, setUser] = useState(null)
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [loginMessage, setLoginMessage] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [isRestoringSession, setIsRestoringSession] = useState(true)

  const [activePage, setActivePage] = useState('documents')
  const [selectedDocument, setSelectedDocument] = useState(null)
  const [documentAccessError, setDocumentAccessError] = useState('')

  const [documents, setDocuments] = useState([])
  const [documentPermissions, setDocumentPermissions] = useState({})

  const [title, setTitle] = useState('')
  const [category, setCategory] = useState('')
  const [file, setFile] = useState(null)

  const [searchTitle, setSearchTitle] = useState('')
  const [searchCategory, setSearchCategory] = useState('')
  const [searchOwner, setSearchOwner] = useState('')
  const [fromDate, setFromDate] = useState('')
  const [toDate, setToDate] = useState('')

  const [message, setMessage] = useState('')
  const [busy, setBusy] = useState(false)

  const [auditLogs, setAuditLogs] = useState([])
  const [showAudit, setShowAudit] = useState(false)

 const [sharingDocumentId, setSharingDocumentId] = useState('')
const [sharingEmail, setSharingEmail] = useState('')
const [sharingLevel, setSharingLevel] = useState('READ')
const [sharingBusy, setSharingBusy] = useState(false)
const [existingShares, setExistingShares] = useState([])
const [sharesBusy, setSharesBusy] = useState(false)

  const [editingDocument, setEditingDocument] = useState(null)
  const [editTitle, setEditTitle] = useState('')
  const [editCategory, setEditCategory] = useState('')
  const [editBusy, setEditBusy] = useState(false)
  const [replacementFile, setReplacementFile] = useState(null)

  const normalizedEmail = email.trim().toLowerCase()
  const isAdmin = hasAdminRole(user)

  useEffect(() => {
    let cancelled = false

    request('/api/me', { suppressSessionExpired: true })
      .then((currentUser) => {
        if (cancelled) return
        setUser(currentUser)
        setEmail(currentUser.email || '')
      })
      .catch(() => {
        // No valid session is normal when a user first opens the login page.
      })
      .finally(() => {
        if (!cancelled) setIsRestoringSession(false)
      })

    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    const documentId = new URLSearchParams(window.location.search)
      .get('documentId')
    if (user && documentId) {
      openDocumentById(documentId)
    }
  }, [user])

  useEffect(() => {
    function handleBrowserNavigation() {
      const documentId = new URLSearchParams(window.location.search)
        .get('documentId')
      if (documentId) {
        if (user) openDocumentById(documentId)
        return
      }

      setSelectedDocument(null)
      setDocumentAccessError('')
      if (activePage === 'details') setActivePage('documents')
    }

    window.addEventListener('popstate', handleBrowserNavigation)
    return () => window.removeEventListener('popstate', handleBrowserNavigation)
  }, [user, activePage])

  useEffect(() => {
    function handleSessionExpired() {
      setUser(null)
      setEmail('')
      setPassword('')
      setDocuments([])
      setDocumentPermissions({})
      setAuditLogs([])
      setShowAudit(false)
      setSelectedDocument(null)
      setDocumentAccessError('')
      setActivePage('documents')
      setMessage('')
      setBusy(false)
      setLoginMessage('Your session expired. Please sign in again.')
    }

    window.addEventListener('trustvault:session-expired', handleSessionExpired)
    return () => {
      window.removeEventListener('trustvault:session-expired', handleSessionExpired)
    }
  }, [])

  // Retrieve the current user's permissions for each accessible document.
  const loadDocumentPermissions = useCallback(async (documentList) => {
    const results = await Promise.all(
      documentList.map(async (doc) => {
        try {
          const permissions = await request(
            `/api/documents/${doc.id}/my-permissions`,
          )

          return [doc.id, permissions]
        } catch {
          return [
            doc.id,
            { isOwner: false, canEdit: false },
          ]
        }
      }),
    )

    setDocumentPermissions(Object.fromEntries(results))
  }, [])

  // Load the documents available to the authenticated account.
  const loadDocuments = useCallback(async () => {
    try {
      const result = await request('/api/documents')
      const safeDocuments = Array.isArray(result) ? result : []

      setDocuments(safeDocuments)
      await loadDocumentPermissions(safeDocuments)
    } catch (error) {
      setMessage(`Could not load documents: ${error.message}`)
    }
  }, [loadDocumentPermissions])

  useEffect(() => {
    if (user) loadDocuments()
  }, [user, loadDocuments])

  // The LoginPage passes { email, password } to this callback.
 const handleLogin = async (event) => {
  event.preventDefault()
  setIsSubmitting(true)
  setLoginMessage('')

  try {
    const csrf = await csrfToken()

    const response = await fetch(`${API}/login`, {
      method: 'POST',
      credentials: 'include',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
        [csrf.headerName]: csrf.token,
      },
      body: new URLSearchParams({
        username: email.trim(),
        password,
      }),
    })

    if (!response.ok) {
      throw new Error(`Login failed (${response.status})`)
    }

    const currentUser = await request('/api/me')

    setUser(currentUser)
    setEmail(currentUser.email || email.trim())
    setPassword('')
    setActivePage('documents')
    setLoginMessage('')

  } catch (error) {
    console.error('Login error:', error)
    setLoginMessage(
      error.message === 'Login failed (401)'
        ? 'Login failed: the backend rejected the credentials.'
        : `Login failed: ${error.message}`,
    )
  } finally {
    setIsSubmitting(false)
  }
}

  // Upload using the existing document endpoint.
  async function uploadDocument(event) {
    event.preventDefault()

    // Preserve the form reference before awaiting network requests.
    const formElement = event.currentTarget

    if (!title.trim()) {
      setMessage('Enter a document title.')
      return
    }

    if (!file) {
      setMessage('Choose a file to upload.')
      return
    }

    setBusy(true)
    setMessage('')

    try {
      const form = new FormData()

      form.append('title', title.trim())
      form.append('category', category.trim())
      form.append('file', file)

      await protectedRequest('/api/documents', {
        method: 'POST',
        body: form,
      })

      setTitle('')
      setCategory('')
      setFile(null)
      formElement.reset()

      setMessage('Document uploaded successfully.')

      await loadDocuments()
      setActivePage('documents')
    } catch (error) {
      setMessage(`Upload failed: ${error.message}`)
    } finally {
      setBusy(false)
    }
  }

  // Search through the backend so all filters use its accessible-document
  // and created-date rules consistently.
  async function searchDocuments(event) {
    event.preventDefault()

    if (fromDate && toDate && fromDate > toDate) {
      setMessage('The from date must not be later than the to date.')
      return
    }

    const params = new URLSearchParams()

    if (searchTitle.trim()) {
      params.set('title', searchTitle.trim())
    }

    if (searchCategory.trim()) {
      params.set('category', searchCategory.trim())
    }

    if (searchOwner.trim()) {
      params.set('ownerEmail', searchOwner.trim())
    }

    if (fromDate) {
      params.set('fromDate', `${fromDate}T00:00:00Z`)
    }

    if (toDate) {
      params.set('toDate', `${toDate}T23:59:59.999999999Z`)
    }

    setBusy(true)
    setMessage('')

    try {
      const query = params.toString()

      const result = await request(
        `/api/documents/search${query ? `?${query}` : ''}`,
      )

      const searchResults = Array.isArray(result) ? result : []

      setDocuments(searchResults)
      await loadDocumentPermissions(searchResults)

      if (
        (searchTitle.trim() || searchCategory.trim() || searchOwner.trim() ||
          fromDate || toDate) &&
        searchResults.length === 0
      ) {
        setMessage('No documents match the selected filters.')
      }
    } catch (error) {
      setMessage(`Search failed: ${error.message}`)
    } finally {
      setBusy(false)
    }
  }

  // Download a document using the existing download endpoint.
  async function downloadDocument(doc) {
    setMessage('')

    try {
      const response = await fetch(
        `${API}/api/documents/${doc.id}/download`,
        { credentials: 'include' },
      )

      if (!response.ok) {
        throw new Error(`Download failed (${response.status})`)
      }

      const blob = await response.blob()
      const url = URL.createObjectURL(blob)
      const link = window.document.createElement('a')

      link.href = url
      link.download =
        doc.originalFilename || doc.title || 'document'

      window.document.body.appendChild(link)
      link.click()
      link.remove()

      // Revoke after the browser has had time to start the download.
      window.setTimeout(() => URL.revokeObjectURL(url), 1000)
    } catch (error) {
      setMessage(error.message)
    }
  }

  async function openDocumentById(documentId, { updateUrl = false } = {}) {
    if (updateUrl) setDocumentIdInUrl(documentId)
    setBusy(true)
    setMessage('')
    setDocumentAccessError('')

    try {
      const endpoint = `/api/documents/${encodeURIComponent(documentId)}`
      const [documentDetails, permissions] = await Promise.all([
        request(endpoint),
        request(`${endpoint}/my-permissions`),
      ])
      setSelectedDocument(documentDetails)
      setDocumentPermissions((current) => ({
        ...current,
        [documentDetails.id]: permissions,
      }))
      setActivePage('details')
    } catch (error) {
      setSelectedDocument(null)
      setDocumentAccessError(
        error.status === 403 || error.status === 404
          ? 'You do not have permission to view this document, or it does not exist.'
          : error.status === 400
            ? 'The document link is invalid.'
            : `Could not open document: ${error.message}`,
      )
      setActivePage('details')
    } finally {
      setBusy(false)
    }
  }

  function viewDocument(doc) {
    if (busy) return
    return openDocumentById(doc.id, { updateUrl: true })
  }

  function closeDocumentDetails() {
    setDocumentIdInUrl(null)
    setSelectedDocument(null)
    setDocumentAccessError('')
    setActivePage('documents')
  }

  function startEditing(doc) {
    setEditingDocument(doc)
    setEditTitle(doc.title || '')
    setEditCategory(doc.category || '')
    setReplacementFile(null)
    setMessage('')
  }

  function cancelEditing() {
    setEditingDocument(null)
    setEditTitle('')
    setEditCategory('')
    setReplacementFile(null)
  }

  // Update metadata and, optionally, replace the underlying file.
  async function saveDocumentEdit(event) {
    event.preventDefault()

    if (!editingDocument || editBusy) return

    setEditBusy(true)
    setMessage('')

    try {
      await protectedRequest(
        `/api/documents/${editingDocument.id}`,
        {
          method: 'PATCH',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            title: editTitle.trim(),
            category: editCategory.trim(),
          }),
        },
      )

      if (replacementFile) {
        const form = new FormData()
        form.append('file', replacementFile)

        await protectedRequest(
          `/api/documents/${editingDocument.id}/file`,
          {
            method: 'PUT',
            body: form,
          },
        )
      }

      await loadDocuments()

      setMessage(
        replacementFile
          ? 'Document details and file updated successfully.'
          : 'Document details updated successfully.',
      )

      cancelEditing()
    } catch (error) {
      setMessage(
        `Update failed: ${error.message}. Verify the document before retrying.`,
      )

      await loadDocuments()
    } finally {
      setEditBusy(false)
    }
  }

  async function deleteDocument(doc) {
    if (!window.confirm(`Delete "${doc.title}" permanently?`)) {
      return
    }

    setBusy(true)
    setMessage('')

    try {
      await protectedRequest(`/api/documents/${doc.id}`, {
        method: 'DELETE',
      })

      setExistingShares((currentShares) =>
        currentShares.filter((share) => share.documentId !== doc.id),
      )
      setMessage('Document deleted.')

      if (selectedDocument?.id === doc.id) {
        closeDocumentDetails()
      }

      if (editingDocument?.id === doc.id) {
        cancelEditing()
      }

      await Promise.all([loadDocuments(), loadExistingShares()])
    } catch (error) {
      setMessage(`Deletion failed: ${error.message}`)
    } finally {
      setBusy(false)
    }
  }

  // Administrators see the global log; other roles see only their own events.
  const loadAuditLogs = useCallback(async () => {
    setBusy(true)

    try {
      const endpoint = isAdmin
        ? '/api/admin/audit-logs'
        : '/api/audit-logs'
      const logs = await request(endpoint)

      setAuditLogs(Array.isArray(logs) ? logs : [])
      setShowAudit(true)
      setMessage('')
    } catch (error) {
      setAuditLogs([])
      setMessage(`Could not load audit logs: ${error.message}`)
    } finally {
      setBusy(false)
    }
  }, [isAdmin])

  useEffect(() => {
    if (user && activePage === 'audit') {
      loadAuditLogs()
    }
  }, [user, activePage, loadAuditLogs])

  async function logout() {
    try {
      const csrf = await csrfToken()

      await fetch(`${API}/logout`, {
        method: 'POST',
        credentials: 'include',
        headers: {
          [csrf.headerName]: csrf.token,
        },
      })
    } catch {
      // Clear the local session even if the logout request fails.
    } finally {
      setUser(null)
      setEmail('')
      setDocuments([])
      setDocumentPermissions({})
      setAuditLogs([])
      setShowAudit(false)
      setSelectedDocument(null)
      setDocumentAccessError('')
      setDocumentIdInUrl(null)

      setTitle('')
      setCategory('')
      setFile(null)

      setSearchTitle('')
      setSearchCategory('')
      setSearchOwner('')
      setFromDate('')
      setToDate('')

      cancelEditing()

      setSharingDocumentId('')
      setSharingEmail('')
      setSharingLevel('READ')
      setExistingShares([])

      setMessage('')
      setActivePage('documents')
      setLoginMessage('You have signed out.')
    }
  }

  // Grant access through the existing permission endpoint.
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
      await loadExistingShares()
      await loadDocuments()

    } catch (error) {
      setMessage(`Could not grant permission: ${error.message}`)
    } finally {
      setSharingBusy(false)
    }
  }

  // Revoke access through the existing permission endpoint.
  async function revokeDocumentPermission(event) {
    event.preventDefault()

    if (!sharingDocumentId || !sharingEmail.trim()) {
      setMessage('Select a document and enter the recipient email.')
      return
    }

    if (
      !window.confirm(
        `Revoke access for ${sharingEmail.trim()} to this document?`,
      )
    ) {
      return
    }

    setSharingBusy(true)
    setMessage('')

    try {
      const params = new URLSearchParams({
        email: sharingEmail.trim(),
      })

      await protectedRequest(
        `/api/documents/${sharingDocumentId}/permissions?${params}`,
        { method: 'DELETE' },
      )

      setMessage(`Permission revoked for ${sharingEmail.trim()}.`)

      await loadDocuments()
    } catch (error) {
      setMessage(`Could not revoke permission: ${error.message}`)
    } finally {
      setSharingBusy(false)
    }
  }

  // Clear the filter values before refreshing the library.
  function clearDocumentFilters() {
    setSearchTitle('')
    setSearchCategory('')
    setSearchOwner('')
    setFromDate('')
    setToDate('')

    loadDocuments()
  }

  const ownedDocuments = documents.filter((doc) => {
    const permissions = documentPermissions[doc.id] || {}
    const ownerEmail = (doc.ownerEmail || '').trim().toLowerCase()

    return (
      permissions.isOwner === true ||
      (Boolean(normalizedEmail) && ownerEmail === normalizedEmail)
    )
  })

  const sharedDocuments = documents.filter((doc) => {
    const permissions = documentPermissions[doc.id] || {}
    const ownerEmail = (doc.ownerEmail || '').trim().toLowerCase()

    return (
      permissions.isOwner !== true &&
      (!ownerEmail || ownerEmail !== normalizedEmail)
    )
  })

  function renderEditForm() {
    if (!editingDocument) return null

    return (
      <form
        className="panel edit-document-form"
        onSubmit={saveDocumentEdit}
      >
        <div className="section-heading">
          <div>
            <p className="eyebrow">DOCUMENT SETTINGS</p>
            <h3>Edit document</h3>
          </div>

          <button
            type="button"
            className="secondary-button"
            onClick={cancelEditing}
            disabled={editBusy}
          >
            Cancel
          </button>
        </div>

        <p className="description">
          Editing:{' '}
          {editingDocument.originalFilename || editingDocument.title}
        </p>

        <div className="form-grid">
          <div className="form-field">
            <label htmlFor="edit-title">Document title</label>
            <input
              id="edit-title"
              value={editTitle}
              onChange={(event) => setEditTitle(event.target.value)}
              maxLength={200}
              required
            />
          </div>

          <div className="form-field">
            <label htmlFor="edit-category">Category</label>
            <input
              id="edit-category"
              value={editCategory}
              onChange={(event) => setEditCategory(event.target.value)}
              maxLength={100}
              required
            />
          </div>

          <div className="form-field">
            <label htmlFor="replacement-file">
              Replace file (optional)
            </label>

            <input
              id="replacement-file"
              type="file"
              accept=".pdf,.png,.jpg,.jpeg"
              onChange={(event) =>
                setReplacementFile(event.target.files?.[0] || null)
              }
            />

            {replacementFile && (
              <p className="field-hint">
                Selected: {replacementFile.name}
              </p>
            )}
          </div>
        </div>

        <div className="button-row">
          <button
            type="submit"
            className="primary-button"
            disabled={editBusy}
          >
            {editBusy ? 'Saving…' : 'Save changes'}
          </button>

          <button
            type="button"
            className="secondary-button"
            onClick={cancelEditing}
            disabled={editBusy}
          >
            Cancel
          </button>
        </div>
      </form>
    )
  }




const loadExistingShares = useCallback(async () => {
  setSharesBusy(true)

  try {
    const result = await request('/api/permissions/my-grants')
    setExistingShares(Array.isArray(result) ? result : [])
  } catch (error) {
    setExistingShares([])
    setMessage(
      `Could not load existing shares: ${error.message}`
    )
  } finally {
    setSharesBusy(false)
  }
}, [])

  useEffect(() => {
    if (!user) {
      setExistingShares([])
      return
    }

    loadExistingShares()
  }, [user, loadExistingShares])



  function renderSharingPanel() {
    return (
      <>
      <section className="panel">
        <div className="section-heading">
          <div>
            <p className="eyebrow">ACCESS CONTROL</p>
            <h3>Share a document</h3>
            <p className="description">
              Grant or revoke access to a document you own.
            </p>
          </div>
        </div>

        <form onSubmit={grantDocumentPermission}>
          <div className="form-grid">
            <div className="form-field">
              <label htmlFor="sharing-document">Document</label>
              <select
                id="sharing-document"
                value={sharingDocumentId}
                onChange={(event) =>
                  setSharingDocumentId(event.target.value)
                }
                required
              >
                <option value="">Select a document</option>

                {ownedDocuments.map((doc) => (
                  <option key={doc.id} value={doc.id}>
                    {doc.title || doc.originalFilename || doc.id}
                  </option>
                ))}
              </select>
            </div>

            <div className="form-field">
              <label htmlFor="sharing-email">Recipient email</label>
              <input
                id="sharing-email"
                type="email"
                value={sharingEmail}
                onChange={(event) => setSharingEmail(event.target.value)}
                required
              />
            </div>

            <div className="form-field">
              <label htmlFor="sharing-level">Permission level</label>
              <select
                id="sharing-level"
                value={sharingLevel}
                onChange={(event) => setSharingLevel(event.target.value)}
              >
                <option value="READ">Read only</option>
                <option value="EDIT">Read and edit</option>
              </select>
            </div>
          </div>

          <div className="button-row">
            <button
              type="submit"
              className="primary-button"
              disabled={sharingBusy}
            >
              {sharingBusy ? 'Please wait…' : 'Grant access'}
            </button>
          </div>
        </form>

      </section>

      <section className="panel existing-shares">
  <div className="section-heading">
    <div>
      <p className="eyebrow">ACCESS MANAGEMENT</p>
      <h3>All active access grants</h3>
      <p className="description">
        Review and revoke access across all documents you own.
      </p>
    </div>

    <button
      type="button"
      className="secondary-button"
      onClick={loadExistingShares}
      disabled={sharesBusy}
    >
      {sharesBusy ? 'Loading…' : 'Refresh shares'}
    </button>
  </div>

  {sharesBusy ? (
    <p>Loading access grants…</p>
  ) : existingShares.length === 0 ? (
    <div className="empty-state">
      <h3>No active access grants</h3>
      <p>
        Grants you create for documents you own will appear here.
      </p>
    </div>
  ) : (
    <div className="table-wrapper">
      <table className="data-table">
        <thead>
          <tr>
            <th>Document</th>
            <th>Recipient</th>
            <th>Permission</th>
            <th>Action</th>
          </tr>
        </thead>

        <tbody>
          {existingShares.map((share) => (
            <tr
              key={`${share.documentId}-${share.recipientEmail}`}
            >
              <td>{share.documentTitle}</td>
              <td>{share.recipientEmail}</td>
              <td>
                {share.permissionLevel === 'EDIT'
                  ? 'Read and edit'
                  : share.permissionLevel === 'READ'
                    ? 'Read only'
                    : share.permissionLevel}
              </td>
              <td>
                <button
                  type="button"
                  className="table-action table-action-danger"
                  disabled={sharingBusy || share.status === 'REVOKED'}
                  onClick={async () => {
                    if (
                      !window.confirm(
                        `Revoke access for ${share.recipientEmail} to ${share.documentTitle}?`
                      )
                    ) {
                      return
                    }

                    setSharingBusy(true)
                    setMessage('')

                    try {
                      const params = new URLSearchParams({
                        email: share.recipientEmail,
                      })

                      await protectedRequest(
                        `/api/documents/${share.documentId}/permissions?${params}`,
                        { method: 'DELETE' }
                      )

                      setExistingShares((currentShares) =>
                        currentShares.map((currentShare) =>
                          currentShare.documentId === share.documentId &&
                          currentShare.recipientEmail === share.recipientEmail
                            ? { ...currentShare, status: 'REVOKED' }
                            : currentShare,
                        ),
                      )
                      setMessage(
                        `Access revoked for ${share.recipientEmail} on ${share.documentTitle}.`
                      )

                      await loadDocuments()
                    } catch (error) {
                      setMessage(
                        `Could not revoke access: ${error.message}`
                      )
                    } finally {
                      setSharingBusy(false)
                    }
                  }}
                >
                  {share.status === 'REVOKED' ? 'Revoked' : 'Revoke'}
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )}
      </section>
      </>
    )
  }

  function renderPage() {
    switch (activePage) {
      case 'documents':
        return (
          <>
            <DocumentsPage
              documents={documents}
              documentPermissions={documentPermissions}
              email={email}
              searchTitle={searchTitle}
              setSearchTitle={setSearchTitle}
              searchCategory={searchCategory}
              setSearchCategory={setSearchCategory}
              searchOwner={searchOwner}
              setSearchOwner={setSearchOwner}
              fromDate={fromDate}
              setFromDate={setFromDate}
              toDate={toDate}
              setToDate={setToDate}
              searchDocuments={searchDocuments}
              loadDocuments={clearDocumentFilters}
              downloadDocument={downloadDocument}
              startEditing={startEditing}
              deleteDocument={deleteDocument}
              viewDocument={viewDocument}
              busy={busy}
              message={message}
            />

            {renderEditForm()}
            {renderSharingPanel()}
          </>
        )

      case 'upload':
        return (
          <UploadPage
            title={title}
            setTitle={setTitle}
            category={category}
            setCategory={setCategory}
            file={file}
            setFile={setFile}
            uploadDocument={uploadDocument}
            busy={busy}
            message={message}
          />
        )

      case 'shared':
        return (
          <>
            <SharedDocumentsPage
              documents={sharedDocuments}
              documentPermissions={documentPermissions}
              email={email}
              loadDocuments={loadDocuments}
              downloadDocument={downloadDocument}
              startEditing={startEditing}
              deleteDocument={deleteDocument}
              busy={busy}
              message={message}
            />

            {renderEditForm()}
          </>
        )

      case 'details':
        if (documentAccessError) {
          return (
            <AccessDeniedPage
              message={documentAccessError}
              goBack={closeDocumentDetails}
              goToDocuments={closeDocumentDetails}
            />
          )
        }
        return (
          <DocumentDetailsPage
            document={selectedDocument}
            permission={
              selectedDocument
                ? documentPermissions[selectedDocument.id] || {}
                : {}
            }
            email={email}
            downloadDocument={downloadDocument}
            startEditing={startEditing}
            deleteDocument={deleteDocument}
            goBack={closeDocumentDetails}
            busy={busy || editBusy}
            message={message}
          />
        )


case 'audit':
  return (
    <AuditPage
      auditLogs={showAudit ? auditLogs : []}
      busy={busy}
      message={message}
      loadAuditLogs={loadAuditLogs}
    />
  )


        return (
          <AuditPage
            auditLogs={showAudit ? auditLogs : []}
            busy={busy}
            message={message}
            loadAuditLogs={loadAuditLogs}
          />
        )

      case 'admin':
        if (!isAdmin) {
          return (
            <AccessDeniedPage
              message="Administrator privileges are required to access this page."
              goBack={() => setActivePage('documents')}
              goToDocuments={() => setActivePage('documents')}
            />
          )
        }

        return (
          <AdminPage
            auditLogs={showAudit ? auditLogs : []}
            busy={busy}
            message={message}
            loadAuditLogs={loadAuditLogs}
            currentUser={user}
          >
            {renderSharingPanel()}
          </AdminPage>
        )

      default:
        return (
          <AccessDeniedPage
            message="This page is not available."
            goBack={() => setActivePage('documents')}
            goToDocuments={() => setActivePage('documents')}
          />
        )
    }
  }

  if (!user) {
    return (
      <LoginPage
  email={email}
  setEmail={setEmail}
  password={password}
  setPassword={setPassword}
  handleLogin={handleLogin}
  isSubmitting={isSubmitting}
  isRestoringSession={isRestoringSession}
  loginMessage={loginMessage}
/>
    )
  }

  return (
    <div className="app-layout">

<Sidebar
  activePage={activePage}
  setActivePage={(page) => {
    // Administration remains restricted to admins.
    if (page === 'admin' && !isAdmin) {
      setMessage('Administrator privileges are required.')
      return
    }

    setMessage('')
    setActivePage(page)
  }}
  currentUser={user}
  email={email}
  isAdmin={isAdmin}
  onLogout={logout}
  busy={isSubmitting}
/>


      <main className="workspace-main">


<header className="workspace-header">
  <div className="header-spacer" />

  <button
    type="button"
    className="secondary-button"
    onClick={logout}
    disabled={isSubmitting}
  >
    Sign out
  </button>
</header>



        <section className="workspace-content">
          {renderPage()}
        </section>
      </main>
    </div>
  )
}

export default App
