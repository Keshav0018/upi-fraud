export default function DemoPanel({ onLargePayment, onVelocity, onNewDeviceNewPayee, log }) {
  return (
    <div className="demo-panel">
      <h2>Demo controls</h2>
      <p>Trigger fraud-rule scenarios without clicking through the phone manually.</p>

      <button className="demo-button" onClick={onLargePayment}>
        Large payment
        <small>₹35,000 to a contact — trips the large-amount rule</small>
      </button>

      <button className="demo-button" onClick={onVelocity}>
        Three payments in two seconds
        <small>Fires rapidly — trips the velocity rule</small>
      </button>

      <button className="demo-button" onClick={onNewDeviceNewPayee}>
        New phone, new receiver
        <small>Unknown device + unknown payee — trips the new-device rule</small>
      </button>

      {log.length > 0 && (
        <div className="demo-log">
          {log.map((entry, i) => (
            <div key={i}>{entry}</div>
          ))}
        </div>
      )}
    </div>
  );
}
