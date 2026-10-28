import { useState } from 'react';

export default function AmountScreen({ payee, onBack, onSubmit }) {
  const [amount, setAmount] = useState('');
  const [note, setNote] = useState('');

  const numericAmount = Number(amount);
  const isValidAmount = amount !== '' && numericAmount >= 1 && numericAmount <= 100000;
  const error =
    amount !== '' && !isValidAmount ? 'Amount must be between ₹1 and ₹1,00,000' : null;

  return (
    <div className="screen-content">
      <div className="screen-header">
        <button className="back-button" onClick={onBack}>←</button>
        <span className="screen-title">Pay</span>
      </div>

      <div className="pay-target">
        <div className="pay-target-avatar">{payee.name.charAt(0).toUpperCase()}</div>
        <div className="pay-target-name">{payee.name}</div>
        <div className="pay-target-vpa">{payee.vpa}</div>
      </div>

      <div className="amount-input-row">
        <span>₹</span>
        <input
          type="number"
          inputMode="decimal"
          autoFocus
          placeholder="0"
          value={amount}
          onChange={(e) => setAmount(e.target.value)}
        />
      </div>
      {error && <div className="form-error">{error}</div>}

      <input
        className="note-input"
        placeholder="Add a note (optional)"
        maxLength={280}
        value={note}
        onChange={(e) => setNote(e.target.value)}
      />

      <button
        className="btn btn-primary"
        disabled={!isValidAmount}
        onClick={() => onSubmit({ amount: numericAmount, note })}
      >
        Pay {isValidAmount ? `₹${numericAmount.toLocaleString('en-IN')}` : ''}
      </button>
    </div>
  );
}
