
import StatusBadge from '../components/StatusBadge'


function getOutcome(event) {
  return String(event.outcome || 'UNKNOWN').toUpperCase()
}

function getOutcomeClass(outcome) {
  switch (outcome) {
    case 'SUCCESS':
      return 'audit-outcome-success'
    case 'DENIED':
      return 'audit-outcome-denied'
    case 'FAILED':
      return 'audit-outcome-failed'
    case 'PENDING':
      return 'audit-outcome-pending'
    default:
      return 'audit-outcome-unknown'
  }
}

export default function AdminPage({
  auditLogs = [],
  busy = false,
  message = '',
  loadAuditLogs,
  currentUser = null,
}) {

  const getEventTimestamp = (event) =>
    event.occurredAt ||
    event.createdAt ||
    event.timestamp ||
    event.eventTime ||
    event.occurredOn ||
    null

  const recentEvents = [...auditLogs]
    .sort((a, b) => {
      const dateA = new Date(getEventTimestamp(a) || 0).getTime()
      const dateB = new Date(getEventTimestamp(b) || 0).getTime()

      return dateB - dateA
    })
    .slice(0, 5)


  return (
    <div className="page-stack admin-page">
      <section className="page-intro">
        <p className="eyebrow">SYSTEM CONTROL</p>
        <h2>Administration</h2>
        <p>
          Review recent activity and manage administrative operations.
        </p>
      </section>

      {message && (
        <div className="notice" role="status">
          {message}
        </div>
      )}

      <section className="admin-overview-grid">
        <article className="panel admin-stat">
          <span className="admin-stat-icon" aria-hidden="true">
            ◷
          </span>
          <p>Loaded audit events</p>
          <strong>{auditLogs.length}</strong>
          <span>Events available in this session</span>
        </article>

        <article className="panel admin-stat">
          <span className="admin-stat-icon" aria-hidden="true">
            ◈
          </span>
          <p>Administrator</p>
          <strong>
            {currentUser?.email || currentUser?.username || 'Current user'}
          </strong>
          <span>Signed-in account</span>
        </article>

        <article className="panel admin-stat">
          <span className="admin-stat-icon" aria-hidden="true">
            ✓
          </span>
          <p>Audit status</p>
          <strong>{busy ? 'Loading' : 'Available'}</strong>
          <span>Based on the current page state</span>
        </article>
      </section>

      <section className="panel">
        <div className="section-heading">
          <div>
            <p className="eyebrow">SECURITY MONITORING</p>
            <h3>Recent audit activity</h3>
            <p className="description">
              Review the latest audit events loaded by the application.
            </p>
          </div>

          <button
            type="button"
            className="secondary-button"
            onClick={loadAuditLogs}
            disabled={busy || !loadAuditLogs}
          >
            {busy ? 'Loading…' : 'Refresh activity'}
          </button>
        </div>

        {recentEvents.length === 0 ? (
          <div className="empty-state">
            <div className="empty-state-icon" aria-hidden="true">
              ◷
            </div>
            <h3>No activity loaded</h3>
            <p>
              Refresh the audit log to load available activity records.
            </p>
          </div>
        ) : (
          <div className="table-wrapper">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Date & time</th>
                  <th>Event</th>
                  <th>Actor</th>
                  <th>Resource</th>
                  <th>Outcome</th>
                </tr>
              </thead>

              <tbody>
                {recentEvents.map((event, index) => (
                  <tr key={event.id ?? `${event.action}-${index}`}>

  <td>
    {(() => {
      const value = getEventTimestamp(event)

      if (!value) return '—'

      const date = new Date(value)

      return Number.isNaN(date.getTime())
        ? '—'
        : date.toLocaleString(undefined, {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
          })
    })()}
  </td>


                    <td>
                      <StatusBadge
                        status={event.action || event.eventType || 'Recorded'}
                      />
                    </td>

                    <td>
                      {event.actorEmail ||
                        event.username ||
                        event.actor ||
                        'Unknown user'}
                    </td>

                    <td>
                      {event.documentTitle ||
                        event.documentName ||
                        event.resourceName ||
                        event.documentId ||
                        '—'}
                    </td>

<td>
  <span
    className={`audit-outcome ${getOutcomeClass(getOutcome(event))}`}
  >
    {getOutcome(event)}
  </span>
</td>

                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  )
}
