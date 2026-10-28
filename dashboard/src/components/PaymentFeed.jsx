import { PaymentStatusBadge } from './StatusBadge'

function formatAmount(amount) {
  return `₹${Number(amount).toLocaleString('en-IN')}`
}

export function PaymentFeed({ payments }) {
  return (
    <div className="card">
      <p className="panel-title">Live payment feed</p>
      {payments.length === 0 ? (
        <p className="empty-state">Waiting for payments…</p>
      ) : (
        <ul className="feed-list">
          {payments.map((p) => (
            <li className="feed-row" key={`${p.id}-${p.updatedAt}`}>
              <div className="vpa-pair">
                <span className="route">
                  {p.payerVpa} → {p.payeeVpa}
                </span>
              </div>
              <span className="amount">{formatAmount(p.amount)}</span>
              <PaymentStatusBadge status={p.status} />
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
