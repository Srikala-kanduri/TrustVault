
import DocumentTable from '../components/DocumentTable'

export default function SharedDocumentsPage({
  documents = [],
  documentPermissions = {},
  email = '',
  loadDocuments,
  downloadDocument,
  startEditing,
  deleteDocument,
  busy = false,
  message = '',
}) {
  const currentEmail = email.trim().toLowerCase()

  const sharedDocuments = documents.filter((document) => {
    const permission = documentPermissions[document.id] || {}
    const ownerEmail = (document.ownerEmail || '').trim().toLowerCase()

    return (
      permission.isOwner !== true &&
      (!ownerEmail || ownerEmail !== currentEmail)
    )
  })

  return (
    <div className="page-stack shared-documents-page">
      <section className="page-intro">
        <p className="eyebrow">COLLABORATION</p>
        <h2>Shared with me</h2>
        <p>
          Documents owned by others that are available to you through access
          permissions.
        </p>
      </section>

      {message && (
        <div className="notice" role="status">
          {message}
        </div>
      )}

      <section className="panel">
        <div className="section-heading">
          <div>
            <p className="eyebrow">SHARED WORKSPACE</p>
            <h3>Your shared documents</h3>
            <p className="description">
              Your available actions depend on the permissions granted to you.
            </p>
          </div>

          <span className="document-count">
            {sharedDocuments.length}{' '}
            {sharedDocuments.length === 1 ? 'document' : 'documents'}
          </span>
        </div>

        <div className="button-row">
          <button
            type="button"
            className="secondary-button"
            onClick={loadDocuments}
            disabled={busy}
          >
            {busy ? 'Refreshing…' : 'Refresh documents'}
          </button>
        </div>

        {sharedDocuments.length === 0 ? (
          <div className="empty-state">
            <div className="empty-state-icon" aria-hidden="true">
              ◇
            </div>
            <h3>No shared documents yet</h3>
            <p>
              When someone grants you access to a document, it will appear here.
            </p>
          </div>
        ) : (
          <DocumentTable
            documents={sharedDocuments}
            documentPermissions={documentPermissions}
            email={email}
            onDownload={downloadDocument}
            onEdit={startEditing}
            onDelete={deleteDocument}
          />
        )}
      </section>

      <section className="upload-security-note">
        <span aria-hidden="true">⌑</span>
        <div>
          <strong>Permission-aware access</strong>
          <p>
            Having a document listed here does not automatically grant editing
            or deletion rights. The server must enforce permissions for every
            operation.
          </p>
        </div>
      </section>
    </div>
  )
}
