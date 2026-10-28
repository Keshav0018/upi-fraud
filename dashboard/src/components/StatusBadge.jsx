const ALERT_LABELS = {
  PENDING: { label: 'Pending', className: 'badge-pending' },
  FRAUD: { label: 'Fraud', className: 'badge-fraud' },
  CLEARED: { label: 'Cleared', className: 'badge-cleared' },
}

const PAYMENT_LABELS = {
  PENDING_CHECK: { label: 'Checking…', className: 'badge-approved' },
  COMPLETED: { label: 'Paid', className: 'badge-cleared' },
  HELD: { label: 'Held', className: 'badge-pending' },
  BLOCKED: { label: 'Blocked', className: 'badge-fraud' },
}

export function AlertStatusBadge({ status }) {
  const meta = ALERT_LABELS[status] || { label: status, className: 'badge-approved' }
  return (
    <span className={`badge ${meta.className}`}>
      <span className="dot" aria-hidden="true" />
      {meta.label}
    </span>
  )
}

export function PaymentStatusBadge({ status }) {
  const meta = PAYMENT_LABELS[status] || { label: status, className: 'badge-approved' }
  return (
    <span className={`badge ${meta.className}`}>
      <span className="dot" aria-hidden="true" />
      {meta.label}
    </span>
  )
}
