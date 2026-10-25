
import StatusBadge from '../components/StatusBadge'

function formatDate(value) {
  if (!value) return 'Not available'

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return 'Not available'
  }

  return date.toLocaleString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function getFileType(filename = '') {
  const extension = filename.split('.').pop()?.toLowerCase()

  if (extension === 'pdf') return 'PDF document'
  if (extension === 'png') return 'PNG image'
  if (extension === 'jpg' || extension === 'jpeg') return 'JPEG image'

  return 'File'
}

export default function DocumentDetailsPage({
  document = null,
  permission = {},
  email = '',
  downloadDocument,
  startEditing,
  deleteDocument,
  goBack,
  busy = false,
  message = '',
}) {
  if (!document) {
    return (
      <div className="page-stack">
        <section className="page-intro">
          <p className="eyebrow">DOCUMENT DETAILS</p>
          <h2>Document not selected</h2>
          <p>Select a document from your library to view its details.</p>
        </section>

        <div>
          <button
            type="button"
            className="secondary-button"
            onClick={goBack}
          >
            Back to documents
          </button>
        </div>
      </div>
    )
  }

  const filename = document.originalFilename || document.title || 'Untitled'
  const ownerEmail = document.ownerEmail || 'Unknown owner'
  const normalizedEmail = email.trim().toLowerCase()
  const isOwner =
    permission.isOwner === true ||
    (Boolean(normalizedEmail) &&
      ownerEmail.trim().toLowerCase() === normalizedEmail)

  const canEdit = isOwner || permission.canEdit === true

  const accessStatus = isOwner
    ? 'Owner'
    : canEdit
      ? 'Can edit'
      : 'Read only'

  const details = [
    {
      label: 'Document title',
      value: document.title || 'Untitled',
    },
    {
      label: 'Original filename',
      value: filename,
    },
    {
      label: 'Category',
      value: document.category || 'Uncategorized',
    },
    {
      label: 'Owner',
      value: ownerEmail,
    },
    {
      label: 'File type',
      value: getFileType(filename),
    },
    {
      label: 'Created',
      value: formatDate(document.createdAt),
    },
    {
      label: 'Last updated',
      value: formatDate(document.updatedAt),
    },
  ]

  return (
    <div className="page-stack document-details-page">
      <section className="page-intro">
        <p className="eyebrow">DOCUMENT INFORMATION</p>
        <h2>Document details</h2>
        <p>Review the document information and your access level.</p>
      </section>

      {message && (
        <div className="notice" role="status">
          {message}
        </div>
      )}

      <section className="panel details-panel">
        <div className="details-header">
          <div className="document-file-icon" aria-hidden="true">
            ▤
          </div>

          <div className="details-title">
            <h3>{document.title || 'Untitled document'}</h3>
            <p>{filename}</p>
          </div>

          <StatusBadge status={accessStatus} />
        </div>

        <div className="details-grid">
          {details.map((item) => (
            <div className="detail-item" key={item.label}>
              <span>{item.label}</span>
              <strong>{item.value}</strong>
            </div>
          ))}
        </div>

        <div className="details-actions">
          <button
            type="button"
            className="primary-button"
            onClick={() => downloadDocument?.(document)}
            disabled={busy || !downloadDocument}
          >
            Download document
          </button>

          {canEdit && (
            <button
              type="button"
              className="secondary-button"
              onClick={() => startEditing?.(document)}
              disabled={busy || !startEditing}
            >
              Edit document
            </button>
          )}

          {isOwner && (
            <button
              type="button"
              className="danger-button"
              onClick={() => deleteDocument?.(document)}
              disabled={busy || !deleteDocument}
            >
              Delete document
            </button>
          )}

          <button
            type="button"
            className="secondary-button"
            onClick={goBack}
          >
            Back to documents
          </button>
        </div>
      </section>

      <section className="upload-security-note">
        <span aria-hidden="true">◇</span>
        <div>
          <strong>Access is permission-based</strong>
          <p>
            The information shown here is for display purposes. The backend
            must independently authorize downloads, edits, and deletions.
          </p>
        </div>
      </section>
    </div>
  )
}
