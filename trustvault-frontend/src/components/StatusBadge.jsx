
const statusStyles = {
  Owner: 'status-owner',
  'Can edit': 'status-edit',
  'Read only': 'status-read',
  Admin: 'status-admin',
  Success: 'status-success',
  Denied: 'status-denied',
  Pending: 'status-pending',
  Recorded: 'status-default',
}

export default function StatusBadge({ status = 'Read only' }) {
  const normalizedStatus = String(status || 'Read only').trim()
  const className = statusStyles[normalizedStatus] || 'status-default'

  return (
    <span
      className={`status-badge ${className}`}
      role="status"
      aria-label={`Status: ${normalizedStatus}`}
    >
      <span className="status-dot" aria-hidden="true" />
      <span>{normalizedStatus}</span>
    </span>
  )
}
