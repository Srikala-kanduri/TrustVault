import { useState } from 'react'
function formatDate(value) {
  if (!value) return '—'

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) return '—'

  return date.toLocaleString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function getEventTimestamp(event) {
  return (
    event.occurredAt ||
    event.createdAt ||
    event.timestamp ||
    event.eventTime ||
    null
  )
}

function getEventLabel(event) {
  return (
    event.action ||
    event.eventType ||
    event.event ||
    event.type ||
    'Unknown event'
  )
}

function getActor(event) {
  return event.actorEmail || event.username || event.actor || 'Unknown user'
}

function getTarget(event) {
  return (
    event.documentTitle ||
    event.resourceName ||
    event.documentName ||
    event.resourceId ||
    event.documentId ||
    '—'
  )
}
function getOutcome(event) {
  return String(event.outcome || 'UNKNOWN').toUpperCase()
}

function getOutcomeClass(outcome) {
  switch (outcome) {
    case 'SUCCESS':
      return 'audit-outcome-success'
    case 'DENIED':
      return 'audit-outcome-denied'
    case 'PENDING':
      return 'audit-outcome-pending'
    case 'FAILED':
      return 'audit-outcome-failed'
    default:
      return 'audit-outcome-unknown'
  }
}
function getDetails(event) {
  return event.details || '—'
}

export default function AuditPage({
  auditLogs = [],
  busy = false,
  message = '',
  loadAuditLogs,
}) {
  const [search, setSearch] = useState('')
  const [eventType, setEventType] = useState('')
  const [actor, setActor] = useState('')
  const [fromDate, setFromDate] = useState('')
  const [toDate, setToDate] = useState('')

  const filteredLogs = auditLogs.filter((event) => {
    const label = String(getEventLabel(event))
    const actorName = String(getActor(event))
    const target = String(getTarget(event))
    const timestamp = getEventTimestamp(event)
      event.createdAt ||
      event.timestamp ||
      event.eventTime ||
      event.occurredAt

    const date = timestamp ? new Date(timestamp) : null

    const matchesSearch = [label, actorName, target]
      .join(' ')
      .toLowerCase()
      .includes(search.trim().toLowerCase())

    const matchesType =
      !eventType || label.toLowerCase() === eventType.toLowerCase()

    const matchesActor = actorName
      .toLowerCase()
      .includes(actor.trim().toLowerCase())

    const matchesFrom =
      !fromDate ||
      (date && !Number.isNaN(date.getTime()) &&
        date >= new Date(`${fromDate}T00:00:00`))

    const matchesTo =
      !toDate ||
      (date && !Number.isNaN(date.getTime()) &&
        date < new Date(
          new Date(`${toDate}T00:00:00`).getTime() + 86400000,
        ))

    return (
      matchesSearch &&
      matchesType &&
      matchesActor &&
      matchesFrom &&
      matchesTo
    )
  })

  const eventTypes = [
    ...new Set(auditLogs.map(getEventLabel).filter(Boolean)),
  ]

  function clearFilters() {
    setSearch('')
    setEventType('')
    setActor('')
    setFromDate('')
    setToDate('')
  }

  function exportCsv() {
    const headers = [
  'Time',
  'Event',
  'Actor',
  'Target',
  'Outcome',
  'Details',
]
    const rows = filteredLogs.map((event) => [
      formatDate(
        event.createdAt ||
          event.timestamp ||
          event.eventTime ||
          event.occurredAt,
      ),
      getEventLabel(event),
      getActor(event),
      getTarget(event),
      getOutcome(event),
      getDetails(event),
    ])

    const escapeCsv = (value) =>
      `"${String(value ?? '').replace(/"/g, '""')}"`

    const csv = [headers, ...rows]
      .map((row) => row.map(escapeCsv).join(','))
      .join('\r\n')

    const blob = new Blob([csv], {
      type: 'text/csv;charset=utf-8;',
    })

    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')

    link.href = url
    link.download = 'trustvault-audit-log.csv'
    link.click()

    URL.revokeObjectURL(url)
  }

  return (
    <div className="page-stack audit-page">
      <section className="page-intro">
        <p className="eyebrow">SECURITY & ACCOUNTABILITY</p>
        <h2>Audit trail</h2>
        <p>
          Review recorded events and investigate document access and activity.
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
            <p className="eyebrow">EVENT HISTORY</p>
            <h3>Activity records</h3>
            <p className="description">
              Narrow the records by event, user, keyword, or date range.
            </p>
          </div>

          <span className="document-count">
            {filteredLogs.length} records
          </span>
        </div>

        <form
          onSubmit={(event) => event.preventDefault()}
          className="audit-filters"
        >
          <div className="form-field">
            <label htmlFor="audit-search">Search events</label>
            <input
              id="audit-search"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder="Search event or document"
            />
          </div>

          <div className="form-field">
            <label htmlFor="audit-type">Event type</label>
            <select
              id="audit-type"
              value={eventType}
              onChange={(event) => setEventType(event.target.value)}
            >
              <option value="">All event types</option>
              {eventTypes.map((type) => (
                <option key={type} value={type}>
                  {type}
                </option>
              ))}
            </select>
          </div>

          <div className="form-field">
            <label htmlFor="audit-actor">Actor</label>
            <input
              id="audit-actor"
              value={actor}
              onChange={(event) => setActor(event.target.value)}
              placeholder="Filter by user"
            />
          </div>

          <div className="form-field">
            <label htmlFor="audit-from">From date</label>
            <input
              id="audit-from"
              type="date"
              value={fromDate}
              max={toDate || undefined}
              onChange={(event) => setFromDate(event.target.value)}
            />
          </div>

          <div className="form-field">
            <label htmlFor="audit-to">To date</label>
            <input
              id="audit-to"
              type="date"
              value={toDate}
              min={fromDate || undefined}
              onChange={(event) => setToDate(event.target.value)}
            />
          </div>
        </form>

        <div className="button-row">
          <button
            type="button"
            className="secondary-button"
            onClick={clearFilters}
          >
            Clear filters
          </button>

          <button
            type="button"
            className="secondary-button"
            onClick={loadAuditLogs}
            disabled={busy || !loadAuditLogs}
          >
            {busy ? 'Loading…' : 'Refresh audit log'}
          </button>

          <button
            type="button"
            className="primary-button"
            onClick={exportCsv}
            disabled={filteredLogs.length === 0}
          >
            Export CSV
          </button>
        </div>
      </section>

      <section className="panel">
        <div className="section-heading">
          <div>
            <p className="eyebrow">RECORDED ACTIVITY</p>
            <h3>Audit events</h3>
          </div>
        </div>

        {filteredLogs.length === 0 ? (
          <div className="empty-state">
            <div className="empty-state-icon" aria-hidden="true">
              ◷
            </div>
            <h3>No audit events found</h3>
            <p>
              Try adjusting the filters or refresh the audit log.
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
                  <th>Document / resource</th>
                  <th>Outcome</th>
                </tr>
              </thead>

              <tbody>
                {filteredLogs.map((event, index) => (
                  <tr
                    key={
                      event.id ??
                      `${getEventLabel(event)}-${index}`
                    }
                  >
                    <td>
                      {formatDate(
                        event.createdAt ||
                          event.timestamp ||
                          event.eventTime ||
                          event.occurredAt,
                      )}
                    </td>
                    <td>
                      <strong>{getEventLabel(event)}</strong>
                    </td>
                    <td>{getActor(event)}</td>
                    <td>{getTarget(event)}</td>
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
