
const navigationItems = [
  {
    id: 'documents',
    label: 'All documents',
    description: 'Your document library',
    icon: '▤',
  },
  {
    id: 'upload',
    ownerOnly: true,
    label: 'Upload document',
    description: 'Add a new document',
    icon: '↑',
  },
  {
    id: 'shared',
    label: 'Shared with me',
    description: 'Documents shared with you',
    icon: '◇',
  },
  {
    id: 'audit',
    label: 'Audit trail',
    description: 'Recorded activity',
    icon: '◷',
  },
  {
    id: 'admin',
    label: 'Administration',
    description: 'Administrative tools',
    icon: '⚙',
    adminOnly: true,
  },
]

export default function Sidebar({
  activePage = 'documents',
  setActivePage,
  currentUser = null,
  email = '',
  isAdmin = false,
  canUpload = false,
  onLogout,
  busy = false,
}) {
  const userEmail =
    email ||
    currentUser?.email ||
    currentUser?.username ||
    'Signed-in user'

  const visibleItems = navigationItems.filter(
    (item) => (!item.adminOnly || isAdmin) && (!item.ownerOnly || canUpload),
  )

  function navigateTo(page) {
    setActivePage?.(page)
  }

  return (
    <aside className="sidebar">
      <div className="sidebar-top">
        <div className="sidebar-brand">
          <div className="brand-icon" aria-hidden="true">
            T
          </div>
          <div className="brand-copy">
            <span className="brand-name">TrustVault</span>
            <span className="subtitle">Secure workspace</span>
          </div>
        </div>

        <div className="sidebar-section-label">WORKSPACE</div>

        <nav className="sidebar-navigation" aria-label="Main navigation">
          {visibleItems.map((item) => {
            const isActive = activePage === item.id

            return (
              <button
                key={item.id}
                type="button"
                className={`sidebar-nav-item ${
                  isActive ? 'active' : ''
                }`}
                onClick={() => navigateTo(item.id)}
                aria-current={isActive ? 'page' : undefined}
              >
                <span className="sidebar-nav-icon" aria-hidden="true">
                  {item.icon}
                </span>

                <span className="sidebar-nav-copy">
                  <span className="sidebar-nav-label">
                    {item.label}
                  </span>
                  <span className="sidebar-nav-description">
                    {item.description}
                  </span>
                </span>

                {isActive && (
                  <span className="sidebar-active-indicator" />
                )}
              </button>
            )
          })}
        </nav>
      </div>

      <div className="sidebar-bottom">
        <div className="sidebar-security-card">
          <span aria-hidden="true">◇</span>
          <div>
            <strong>Secure by design</strong>
            <p>Access is permission-based.</p>
          </div>
        </div>

        <div className="sidebar-user">
          <div className="sidebar-avatar" aria-hidden="true">
            {userEmail.charAt(0).toUpperCase()}
          </div>

          <div className="sidebar-user-info">
            <strong title={userEmail}>{userEmail}</strong>
            <span>{isAdmin ? 'Administrator' : 'Signed in'}</span>
          </div>

          <button
            type="button"
            className="sidebar-logout"
            onClick={onLogout}
            disabled={busy || !onLogout}
            aria-label="Sign out"
            title="Sign out"
          >
            ↗
          </button>
        </div>
      </div>
    </aside>
  )
}
