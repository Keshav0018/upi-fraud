const RULE_META = {
  LARGE_AMOUNT: { label: 'Large amount', color: 'var(--series-1)' },
  VELOCITY: { label: 'Velocity', color: 'var(--series-2)' },
  NEW_DEVICE_NEW_PAYEE: { label: 'New device + payee', color: 'var(--series-3)' },
}

export function RuleBreakdown({ countsByRule }) {
  const entries = Object.entries(countsByRule || {})
  const max = Math.max(1, ...entries.map(([, count]) => count))

  return (
    <div className="card">
      <p className="panel-title">Per-rule breakdown</p>
      {entries.length === 0 ? (
        <p className="empty-state">No flags yet.</p>
      ) : (
        entries.map(([rule, count]) => {
          const meta = RULE_META[rule] || { label: rule, color: 'var(--text-muted)' }
          return (
            <div className="rule-bar-row" key={rule}>
              <span>{meta.label}</span>
              <div className="rule-bar-track">
                <div
                  className="rule-bar-fill"
                  style={{ width: `${(count / max) * 100}%`, background: meta.color }}
                />
              </div>
              <span className="amount">{count}</span>
            </div>
          )
        })
      )}
    </div>
  )
}
