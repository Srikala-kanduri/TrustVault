
import StatusBadge from './StatusBadge'

function formatDate(value) {
  if (!value) return '—'

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) return '—'

  return date.toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  })
}

function getFileType(filename = '') {
  const extension = filename.split('.').pop()?.toLowerCase()

  if (extension === 'pdf') return 'PDF'
  if (extension === 'png') return 'PNG'
  if (extension === 'jpg' || extension === 'jpeg') return 'IMG'

  return 'FILE'
}

function ActionIcon({ name }) {
  const commonProps = {
    'aria-hidden': true,
    width: 18,
    height: 18,
    viewBox: '0 0 24 24',
    fill: 'none',
    stroke: 'currentColor',
    strokeWidth: 1.8,
    strokeLinecap: 'round',
    strokeLinejoin: 'round',
  }

  if (name === 'download') {
    return (
      <svg {...commonProps}>
        <path d="M12 3v12m0 0 4-4m-4 4-4-4" />
        <path d="M5 17v3h14v-3" />
      </svg>
    )
  }

  if (name === 'edit') {
    return (
      <svg {...commonProps}>
        <path d="m15 5 4 4M4 20l4-.8L19 8a2.1 2.1 0 0 0-3-3L5 16l-1 4Z" />
      </svg>
    )
  }

  return (
    <svg {...commonProps}>
      <path d="M4 7h16M10 11v6m4-6v6M6 7l1 14h10l1-14M9 7V4h6v3" />
    </svg>
  )
}

export default function DocumentTable({
  documents = [],
  documentPermissions = {},
  email = '',
  onDownload,
  onEdit,
  onDelete,
  onView,
}) {
  const normalizedEmail = email.trim().toLowerCase()

  return (
    <div className="table-wrapper">
      <table className="data-table">
        <thead>
          <tr>
            <th className="type-column">Type</th>
            <th>Document</th>
            <th>Category</th>
            <th>Owner</th>
            <th>Last updated</th>
            <th>Access</th>
            <th>Actions</th>
          </tr>
        </thead>

        <tbody>
          {documents.map((doc) => {
            const permissions = documentPermissions[doc.id] || {}

            const ownerEmail = (doc.ownerEmail || '')
              .trim()
              .toLowerCase()

            const isOwner =
              permissions.isOwner === true ||
              (Boolean(normalizedEmail) &&
                Boolean(ownerEmail) &&
                ownerEmail === normalizedEmail)

            const canEdit = isOwner || permissions.canEdit === true

            const accessStatus = isOwner
              ? 'Owner'
              : canEdit
                ? 'Can edit'
                : 'Read only'

            const filename =
              doc.originalFilename || doc.title || 'Untitled document'

            const updatedAt =
              doc.updatedAt || doc.createdAt || null

            return (
              <tr key={doc.id}>
                <td>
                  <span className="file-type-icon" aria-hidden="true">
                    {getFileType(filename)}
                  </span>
                </td>

                <td>
                  <button
                    type="button"
                    className="document-title-button"
                    onClick={() => onView?.(doc)}
                    disabled={!onView}
                  >
                    {doc.title || 'Untitled document'}
                    <svg
                      className="document-title-cue"
                      aria-hidden="true"
                      width="15"
                      height="15"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      strokeWidth="2"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                    >
                      <path d="m9 18 6-6-6-6" />
                    </svg>
                  </button>
                </td>

                <td>{doc.category || 'Uncategorized'}</td>

                <td>{doc.ownerEmail || 'Unknown owner'}</td>

                <td>{formatDate(updatedAt)}</td>

                <td>
                  <StatusBadge status={accessStatus} />
                </td>

                <td>
                  <div className="table-actions">
                    <button
                      type="button"
                      className="table-action"
                      aria-label={`Download ${doc.title || filename}`}
                      title="Download"
                      onClick={() => onDownload?.(doc)}
                      disabled={!onDownload}
                    >
                      <ActionIcon name="download" />
                    </button>

                    {canEdit && (
                      <button
                        type="button"
                        className="table-action"
                        aria-label={`Edit ${doc.title || filename}`}
                        title="Edit"
                        onClick={() => onEdit?.(doc)}
                        disabled={!onEdit}
                      >
                        <ActionIcon name="edit" />
                      </button>
                    )}

                    {isOwner && (
                      <button
                        type="button"
                        className="table-action table-action-danger"
                        aria-label={`Delete ${doc.title || filename}`}
                        title="Delete"
                        onClick={() => onDelete?.(doc)}
                        disabled={!onDelete}
                      >
                        <ActionIcon name="delete" />
                      </button>
                    )}
                  </div>
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}
