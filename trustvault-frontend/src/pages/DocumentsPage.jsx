
import DocumentTable from '../components/DocumentTable'

export default function DocumentsPage({
  documents = [],
  documentPermissions = {},
  email = '',
  searchTitle = '',
  setSearchTitle,
  searchCategory = '',
  setSearchCategory,
  searchOwner = '',
  setSearchOwner,
  fromDate = '',
  setFromDate,
  toDate = '',
  setToDate,
  searchDocuments,
  loadDocuments,
  downloadDocument,
  startEditing,
  deleteDocument,
  viewDocument,
  busy = false,
  message = '',
}) {
  const hasFilters = Boolean(
    searchTitle.trim() ||
    searchCategory.trim() ||
    searchOwner.trim() ||
    fromDate ||
    toDate
  )

  function clearFilters() {
    setSearchTitle('')
    setSearchCategory('')
    setSearchOwner('')
    setFromDate('')
    setToDate('')
    loadDocuments()
  }

  function handleSubmit(event) {
    event.preventDefault()

    if (fromDate && toDate && fromDate > toDate) {
      return
    }

    searchDocuments(event)
  }

  return (
    <div className="page-stack documents-page">
      <section className="page-intro">
        <p className="eyebrow">DOCUMENT LIBRARY</p>
        <h2>Your documents</h2>
        <p>
          Find documents using their details, ownership, or date range.
        </p>
      </section>

      {message && (
        <div className="notice" role="status">
          {message}
        </div>
      )}

      <section className="panel search-panel">
        <div className="section-heading">
          <div>
            <p className="eyebrow">SEARCH & FILTER</p>
            <h3>Find a document</h3>
            <p className="description">
              Combine filters to narrow down your document library.
            </p>
          </div>

          <span className="document-count">
            {documents.length}{' '}
            {documents.length === 1 ? 'document' : 'documents'}
          </span>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="search-fields">
            <div className="form-field">
              <label htmlFor="search-title">Document name</label>
              <input
                id="search-title"
                value={searchTitle}
                onChange={(event) => setSearchTitle(event.target.value)}
                placeholder="Search by document name"
              />
            </div>

            <div className="form-field">
              <label htmlFor="search-category">Category</label>
              <input
                id="search-category"
                value={searchCategory}
                onChange={(event) => setSearchCategory(event.target.value)}
                placeholder="e.g. Reports"
              />
            </div>

            <div className="form-field">
              <label htmlFor="search-owner">Owner email</label>
              <input
                id="search-owner"
                type="email"
                value={searchOwner}
                onChange={(event) => setSearchOwner(event.target.value)}
                placeholder="owner@example.com"
              />
            </div>

            <div className="form-field">
              <label htmlFor="search-from-date">From date</label>
              <input
                id="search-from-date"
                type="date"
                value={fromDate}
                max={toDate || undefined}
                onChange={(event) => setFromDate(event.target.value)}
              />
            </div>

            <div className="form-field">
              <label htmlFor="search-to-date">To date</label>
              <input
                id="search-to-date"
                type="date"
                value={toDate}
                min={fromDate || undefined}
                onChange={(event) => setToDate(event.target.value)}
              />
            </div>
          </div>

          {fromDate && toDate && fromDate > toDate && (
            <p className="field-error" role="alert">
              The from date must not be later than the to date.
            </p>
          )}

          <div className="button-row">
            <button
              type="submit"
              className="primary-button"
              disabled={busy || Boolean(fromDate && toDate && fromDate > toDate)}
            >
              {busy ? 'Searching…' : 'Apply filters'}
            </button>

            <button
              type="button"
              className="secondary-button"
              onClick={clearFilters}
              disabled={busy || !hasFilters}
            >
              Clear filters
            </button>

            <button
              type="button"
              className="secondary-button"
              onClick={loadDocuments}
              disabled={busy}
            >
              Refresh
            </button>
          </div>
        </form>
      </section>

      <section className="panel document-library">
        <div className="section-heading">
          <div>
            <p className="eyebrow">YOUR WORKSPACE</p>
            <h3>Accessible documents</h3>
          </div>

          <span className="document-count">
            {documents.length} total
          </span>
        </div>

        {documents.length === 0 ? (
          <div className="empty-state">
            <div className="empty-state-icon" aria-hidden="true">
              ▤
            </div>

            <h3>
              {hasFilters ? 'No matching documents' : 'Your library is empty'}
            </h3>

            <p>
              {hasFilters
                ? 'Try adjusting your filters or clearing them to see more documents.'
                : 'Documents you upload or that are shared with you will appear here.'}
            </p>

            {hasFilters && (
              <button
                type="button"
                className="secondary-button"
                onClick={clearFilters}
              >
                Clear filters
              </button>
            )}
          </div>
        ) : (
          <DocumentTable
            documents={documents}
            documentPermissions={documentPermissions}
            email={email}
            onDownload={downloadDocument}
            onEdit={startEditing}
            onDelete={deleteDocument}
            onView={viewDocument}
          />
        )}
      </section>
    </div>
  )
}
