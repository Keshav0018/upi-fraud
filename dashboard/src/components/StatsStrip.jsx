function formatPercent(rate) {
  return `${(rate * 100).toFixed(1)}%`
}

export function StatsStrip({ stats, pendingCount }) {
  const checked = stats?.checkedCount ?? 0
  const flagged = stats?.flaggedCount ?? 0
  const flagRate = stats?.flagRate ?? 0
  const avgLatency = stats?.averageCheckLatencyMs ?? 0

  return (
    <div className="stats-strip">
      <div className="card stat-tile">
        <div className="stat-label">Payments checked</div>
        <div className="stat-value">{checked.toLocaleString()}</div>
      </div>
      <div className="card stat-tile">
        <div className="stat-label">Flagged</div>
        <div className="stat-value">{flagged.toLocaleString()}</div>
        <div className="stat-sub">{formatPercent(flagRate)} flag rate</div>
      </div>
      <div className="card stat-tile">
        <div className="stat-label">Waiting for review</div>
        <div className="stat-value">{pendingCount ?? 0}</div>
      </div>
      <div className="card stat-tile">
        <div className="stat-label">Avg check time</div>
        <div className="stat-value">{Math.round(avgLatency)} ms</div>
      </div>
    </div>
  )
}
