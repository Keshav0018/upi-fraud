import { useEffect, useState } from 'react';

export default function PinScreen({ onComplete }) {
  const [digits, setDigits] = useState('');

  useEffect(() => {
    if (digits.length === 4) {
      const timer = setTimeout(onComplete, 350);
      return () => clearTimeout(timer);
    }
    return undefined;
  }, [digits, onComplete]);

  function press(key) {
    if (key === 'back') {
      setDigits((d) => d.slice(0, -1));
      return;
    }
    setDigits((d) => (d.length < 4 ? d + key : d));
  }

  return (
    <div className="centered-screen">
      <div>
        <div className="screen-title">Enter UPI PIN</div>
        <div className="pay-target-vpa">Any 4 digits work in this demo</div>
      </div>
      <div className="pin-dots">
        {[0, 1, 2, 3].map((i) => (
          <div key={i} className={`pin-dot ${i < digits.length ? 'filled' : ''}`} />
        ))}
      </div>
      <div className="pin-keypad">
        {['1', '2', '3', '4', '5', '6', '7', '8', '9'].map((key) => (
          <button key={key} className="pin-key" onClick={() => press(key)}>
            {key}
          </button>
        ))}
        <div />
        <button className="pin-key" onClick={() => press('0')}>0</button>
        <button className="pin-key" onClick={() => press('back')}>⌫</button>
      </div>
    </div>
  );
}
