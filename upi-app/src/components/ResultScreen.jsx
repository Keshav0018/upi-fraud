import { useEffect, useState } from 'react';
import { getPayment, FINAL_STATUSES } from '../api.js';
import { formatRupees } from '../contacts.js';

const STATUS_COPY = {
  PENDING_CHECK: {
    icon: '…',
    iconClass: 'held',
    title: 'Processing',
    subtitle: 'Just a moment, checking this payment…',
  },
  COMPLETED: {
    icon: '✓',
    iconClass: 'success',
    title: 'Payment successful',
    subtitle: null,
  },
  HELD: {
    icon: '!',
    iconClass: 'held',
    title: 'Payment on hold',
    subtitle: "We're reviewing this payment for fraud. This screen will update automatically.",
  },
  BLOCKED: {
    icon: '✕',
    iconClass: 'blocked',
    title: 'Payment blocked',
    subtitle: 'This payment was blocked after review.',
  },
};

export default function ResultScreen({ payment, onDone }) {
  const [current, setCurrent] = useState(payment);

  useEffect(() => {
    setCurrent(payment);
  }, [payment.id]);

  useEffect(() => {
    if (FINAL_STATUSES.has(current.status)) {
      return undefined;
    }
    const timer = setInterval(async () => {
      try {
        const updated = await getPayment(current.id);
        setCurrent(updated);
      } catch {
        // transient poll error, retry next tick
      }
    }, 2000);
    return () => clearInterval(timer);
  }, [current.id, current.status]);

  const copy = STATUS_COPY[current.status] || STATUS_COPY.PENDING_CHECK;

  return (
    <div className="centered-screen">
      <div className={`result-icon ${copy.iconClass}`}>{copy.icon}</div>
      <div>
        <div className="result-title">{copy.title}</div>
        <div className="result-subtitle">
          {formatRupees(current.amount)} to {current.payeeVpa}
        </div>
        {copy.subtitle && <div className="result-subtitle">{copy.subtitle}</div>}
      </div>
      <button className="btn btn-primary" onClick={onDone}>Done</button>
    </div>
  );
}
