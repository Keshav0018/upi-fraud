import { useEffect, useState } from 'react';
import { getPayment, FINAL_STATUSES } from '../api.js';
import { formatRupees } from '../contacts.js';

const STATUS_LABEL = {
  PENDING_CHECK: 'Processing',
  COMPLETED: 'Paid',
  HELD: 'On hold',
  BLOCKED: 'Blocked',
};

export default function PaymentRow({ payment }) {
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
        // transient network/poll error; next tick will retry
      }
    }, 2000);
    return () => clearInterval(timer);
  }, [current.id, current.status]);

  return (
    <div className="payment-row">
      <div className="payment-row-main">
        <span className="payment-row-payee">{current.payeeVpa}</span>
        <span className="payment-row-amount">{formatRupees(current.amount)}</span>
      </div>
      <span className={`status-pill status-${current.status}`}>
        {STATUS_LABEL[current.status] || current.status}
      </span>
    </div>
  );
}
