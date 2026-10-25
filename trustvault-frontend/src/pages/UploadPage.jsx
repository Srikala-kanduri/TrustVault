
export default function UploadPage({
  title = '',
  setTitle,
  category = '',
  setCategory,
  file = null,
  setFile,
  uploadDocument,
  busy = false,
  message = '',
}) {
  function handleSubmit(event) {
    event.preventDefault()

    if (!title.trim()) {
      return
    }

    if (!file) {
      return
    }

    uploadDocument(event)
  }

  return (
    <div className="page-stack upload-page">
      <section className="page-intro">
        <p className="eyebrow">SECURE STORAGE</p>
        <h2>Upload a document</h2>
        <p>
          Add a document to your private workspace and manage who can access it.
        </p>
      </section>

      {message && (
        <div className="notice" role="status">
          {message}
        </div>
      )}

      <section className="panel upload-panel">
        <div className="section-heading">
          <div>
            <p className="eyebrow">NEW DOCUMENT</p>
            <h3>Document details</h3>
            <p className="description">
              Complete the details below and choose a supported file.
            </p>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="upload-form">
          <div className="form-field">
            <label htmlFor="upload-title">Document title</label>
            <input
              id="upload-title"
              type="text"
              value={title}
              onChange={(event) => setTitle(event.target.value)}
              placeholder="e.g. Project Proposal"
              maxLength={150}
              required
            />
          </div>

          <div className="form-field">
            <label htmlFor="upload-category">Category</label>
            <input
              id="upload-category"
              type="text"
              value={category}
              onChange={(event) => setCategory(event.target.value)}
              placeholder="e.g. Work, Finance, Personal"
              maxLength={80}
            />
          </div>

          <div className="form-field">
            <label htmlFor="upload-file">Choose a file</label>
            <input
              id="upload-file"
              type="file"
              accept=".pdf,.png,.jpg,.jpeg"
              onChange={(event) =>
                setFile(event.target.files?.[0] || null)
              }
              required
            />
            <p className="field-hint">
              Supported formats: PDF, PNG and JPG/JPEG.
            </p>
          </div>

          {file && (
            <div className="selected-file">
              <span className="selected-file-icon" aria-hidden="true">
                ↗
              </span>
              <div>
                <strong>{file.name}</strong>
                <p>
                  {(file.size / 1024).toFixed(1)} KB selected
                </p>
              </div>
            </div>
          )}

          <div className="button-row">
            <button
              type="submit"
              className="primary-button"
              disabled={busy || !title.trim() || !file}
            >
              {busy ? 'Uploading…' : 'Upload document'}
            </button>
          </div>
        </form>
      </section>

      <section className="upload-security-note">
        <span aria-hidden="true">◇</span>
        <div>
          <strong>Your documents, your control</strong>
          <p>
            Access permissions are enforced by the server. Only authorized
            users should be able to view or modify a document.
          </p>
        </div>
      </section>
    </div>
  )
}
