import { useState } from 'react';
import { CONTACTS, PAYER_NAME, PAYER_VPA, STARTING_BALANCE, VPA_REGEX, formatRupees } from '../contacts.js';
import PaymentRow from './PaymentRow.jsx';

export default function HomeScreen({ deviceMode, onToggleDevice, recentPayments, onSelectPayee }) {
  const [upiId, setUpiId] = useState('');

  const upiIdValid = VPA_REGEX.test(upiId.trim());

  function handleProceed() {
    if (!upiIdValid) return;
    onSelectPayee({ name: upiId.trim(), vpa: upiId.trim() });
    setUpiId('');
  }

  return (
    <div className="screen-content">
      <div className="profile-row">
        <div className="avatar">KC</div>
        <div>
          <div className="profile-name">{PAYER_NAME}</div>
          <div className="profile-vpa">{PAYER_VPA}</div>
        </div>
      </div>

      <div className="balance-card">
        <div className="balance-label">Available balance</div>
        <div className="balance-amount">{formatRupees(STARTING_BALANCE)}</div>
      </div>

      <div className="device-row">
        <span className="device-row-label">Paying from</span>
        <div className="device-toggle">
          <button className={deviceMode === 'this' ? 'active' : ''} onClick={() => onToggleDevice('this')}>
            This phone
          </button>
          <button className={deviceMode === 'new' ? 'active' : ''} onClick={() => onToggleDevice('new')}>
            New phone
          </button>
        </div>
      </div>

      <div>
        <div className="section-title" style={{ marginBottom: 10 }}>Contacts</div>
        <div className="contact-grid">
          {CONTACTS.map((contact) => (
            <button key={contact.vpa} className="contact-tile" onClick={() => onSelectPayee(contact)}>
              <div className="contact-avatar">{contact.name.charAt(0)}</div>
              <div className="contact-name">{contact.name.split(' ')[0]}</div>
            </button>
          ))}
        </div>
      </div>

      <div>
        <div className="section-title" style={{ marginBottom: 10 }}>Pay to a UPI ID</div>
        <div className="upi-id-row">
          <input
            placeholder="someone@bank"
            value={upiId}
            onChange={(e) => setUpiId(e.target.value.toLowerCase())}
          />
          <button className="btn btn-primary" disabled={!upiIdValid} onClick={handleProceed}>
            Pay
          </button>
        </div>
      </div>

      <div>
        <div className="section-title" style={{ marginBottom: 6 }}>Recent payments</div>
        {recentPayments.length === 0 ? (
          <div className="empty-state">No payments yet. Try paying a contact above.</div>
        ) : (
          recentPayments.map((payment) => <PaymentRow key={payment.id} payment={payment} />)
        )}
      </div>
    </div>
  );
}
