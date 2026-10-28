import { AlertStatusBadge } from './StatusBadge'

const FILTERS = ['PENDING', 'FRAUD', 'CLEARED', 'ALL']
const FILTER_LABELS = { PENDING: 'Pending', FRAUD: 'Fraud', CLEARED: 'Cleared', ALL: 'All' }

function formatAmount(amount) {
  return `₹${Number(amount).toLocaleString('en-IN')}`
}

export function ReviewQueue({ alerts, filter, onFilterChange, selectedId, onSelect }) {
  return (
    <div className="card">
      <p className="panel-title">Review queue</p>
      <div className="filter-row">
        {FILTERS.map((f) => (
          <button
            key={f}
            className={filter === f ? 'active' : ''}
            onClick={() => onFilterChange(f)}
            type="button"
          >
            {FILTER_LABELS[f]}
          </button>
        ))}
      </div>
      {alerts.length === 0 ? (
        <p className="empty-state">No alerts in this view.</p>
      ) : (
        <ul className="queue-list">
          {alerts.map((alert) => (
            <li key={alert.id}>
              <button
                type="button"
                className={`queue-row ${selectedId === alert.id ? 'selected' : ''}`}
                onClick={() => onSelect(alert.id)}
              >
                <div className="vpa-pair">
                  <span className="route">
                    {alert.payerVpa} → {alert.payeeVpa}
                  </span>
                  <span className="reasons">{alert.reasons.join(', ')}</span>
                </div>
                <span className="amount">{formatAmount(alert.amount)}</span>
                <AlertStatusBadge status={alert.status} />
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
