
export default function Brand({
  subtitle = 'Secure document sharing',
  compact = false,
}) {
  return (
    <div className={`brand ${compact ? 'brand-compact' : ''}`}>
      <div className="brand-icon" aria-hidden="true">
        T
      </div>

      <div className="brand-copy">
        <span className="brand-name">TrustVault</span>

        {!compact && subtitle && (
          <span className="subtitle">{subtitle}</span>
        )}
      </div>
    </div>
  )
}
