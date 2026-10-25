
export default function AccessDeniedPage({
  message = '',
  goBack,
  goToDocuments,
}) {
  return (
    <div className="page-stack access-denied-page">
      <section className="panel access-denied-panel">
        <div className="access-denied-icon" aria-hidden="true">
          !
        </div>

        <p className="eyebrow">ACCESS CONTROL</p>

        <h2>Access denied</h2>

        <p className="access-denied-description">
          {message ||
            "You don't have permission to access this resource. Contact the document owner or an administrator if you believe this is a mistake."}
        </p>

        <div className="access-denied-details">
          <span className="access-denied-indicator" aria-hidden="true" />
          <div>
            <strong>Your access is protected</strong>
            <p>
              TrustVault uses permission checks to help protect private
              documents and sensitive information.
            </p>
          </div>
        </div>

        <div className="button-row access-denied-actions">
          <button
            type="button"
            className="primary-button"
            onClick={goToDocuments}
            disabled={!goToDocuments}
          >
            Return to documents
          </button>

          <button
            type="button"
            className="secondary-button"
            onClick={goBack}
            disabled={!goBack}
          >
            Go back
          </button>
        </div>

        <p className="access-denied-reference">
          If you need access, request it from the document owner.
        </p>
      </section>
    </div>
  )
}
