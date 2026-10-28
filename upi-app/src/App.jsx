import { useCallback, useEffect, useState } from 'react';
import './App.css';
import { createPayment, listPayments } from './api.js';
import { PAYER_VPA, randomContact, randomNewPayeeVpa } from './contacts.js';
import { getThisPhoneDeviceId, newPhoneDeviceId } from './device.js';
import HomeScreen from './components/HomeScreen.jsx';
import AmountScreen from './components/AmountScreen.jsx';
import PinScreen from './components/PinScreen.jsx';
import ProcessingScreen from './components/ProcessingScreen.jsx';
import ResultScreen from './components/ResultScreen.jsx';
import DemoPanel from './components/DemoPanel.jsx';

const THIS_PHONE_ID = getThisPhoneDeviceId();

export default function App() {
  const [screen, setScreen] = useState('home');
  const [deviceMode, setDeviceMode] = useState('this');
  const [activeDeviceId, setActiveDeviceId] = useState(THIS_PHONE_ID);

  const [selectedPayee, setSelectedPayee] = useState(null);
  const [pendingPayment, setPendingPayment] = useState(null);
  const [activePayment, setActivePayment] = useState(null);
  const [processingError, setProcessingError] = useState(null);

  const [recentPayments, setRecentPayments] = useState([]);
  const [demoLog, setDemoLog] = useState([]);

  const refreshRecent = useCallback(async () => {
    try {
      const payments = await listPayments(PAYER_VPA);
      setRecentPayments(payments);
    } catch {
      // keep showing whatever we last had; the gateway/backend may not be up yet
    }
  }, []);

  useEffect(() => {
    refreshRecent();
  }, [refreshRecent]);

  function logDemo(message) {
    setDemoLog((entries) => [message, ...entries].slice(0, 20));
  }

  function handleToggleDevice(mode) {
    if (mode === 'this') {
      setDeviceMode('this');
      setActiveDeviceId(THIS_PHONE_ID);
    } else {
      setDeviceMode('new');
      setActiveDeviceId(newPhoneDeviceId());
    }
  }

  function handleSelectPayee(payee) {
    setSelectedPayee(payee);
    setScreen('amount');
  }

  function handleAmountSubmit({ amount, note }) {
    setPendingPayment({
      payerVpa: PAYER_VPA,
      payeeVpa: selectedPayee.vpa,
      amount,
      note,
      deviceId: activeDeviceId,
    });
    setScreen('pin');
  }

  function handlePinComplete() {
    setScreen('processing');
  }

  const handleProcess = useCallback(async () => {
    if (!pendingPayment) return;
    setProcessingError(null);
    try {
      const result = await createPayment(pendingPayment);
      setActivePayment(result);
      setScreen('result');
      refreshRecent();
    } catch (err) {
      setProcessingError(err.message || 'Payment failed');
    }
  }, [pendingPayment, refreshRecent]);

  function handleResultDone() {
    setScreen('home');
    setSelectedPayee(null);
    setPendingPayment(null);
    setActivePayment(null);
    refreshRecent();
  }

  // ---- Demo controls ----

  function handleLargePayment() {
    const payee = randomContact();
    setSelectedPayee(payee);
    setPendingPayment({
      payerVpa: PAYER_VPA,
      payeeVpa: payee.vpa,
      amount: 35000,
      note: 'Large payment',
      deviceId: activeDeviceId,
    });
    setScreen('pin');
    logDemo(`Large payment queued: ₹35,000 to ${payee.vpa}`);
  }

  function handleNewDeviceNewPayee() {
    const freshDeviceId = newPhoneDeviceId();
    const payeeVpa = randomNewPayeeVpa();
    setDeviceMode('new');
    setActiveDeviceId(freshDeviceId);
    setSelectedPayee({ name: 'New payee', vpa: payeeVpa });
    setPendingPayment({
      payerVpa: PAYER_VPA,
      payeeVpa,
      amount: 2200,
      note: 'First payment from a new phone',
      deviceId: freshDeviceId,
    });
    setScreen('pin');
    logDemo(`New device + new payee queued: ${payeeVpa} from a fresh device`);
  }

  async function handleVelocity() {
    const payee = randomContact();
    logDemo(`Firing 3 rapid payments to ${payee.vpa}…`);
    for (let i = 0; i < 3; i += 1) {
      try {
        // eslint-disable-next-line no-await-in-loop
        await createPayment({
          payerVpa: PAYER_VPA,
          payeeVpa: payee.vpa,
          amount: 300 + i * 50,
          note: `Rapid payment ${i + 1}`,
          deviceId: activeDeviceId,
        });
        logDemo(`Rapid payment ${i + 1}/3 sent`);
      } catch (err) {
        logDemo(`Rapid payment ${i + 1}/3 failed: ${err.message}`);
      }
      // eslint-disable-next-line no-await-in-loop
      await refreshRecent();
      if (i < 2) {
        // eslint-disable-next-line no-await-in-loop
        await new Promise((resolve) => setTimeout(resolve, 650));
      }
    }
  }

  return (
    <div className="layout">
      <div className="phone">
        <div className="phone-screen">
          <div className="status-bar">
            <span>9:41</span>
            <span>UPI Pay</span>
          </div>

          {screen === 'home' && (
            <HomeScreen
              deviceMode={deviceMode}
              onToggleDevice={handleToggleDevice}
              recentPayments={recentPayments}
              onSelectPayee={handleSelectPayee}
            />
          )}

          {screen === 'amount' && selectedPayee && (
            <AmountScreen
              payee={selectedPayee}
              onBack={() => setScreen('home')}
              onSubmit={handleAmountSubmit}
            />
          )}

          {screen === 'pin' && <PinScreen onComplete={handlePinComplete} />}

          {screen === 'processing' && (
            <ProcessingScreen
              onProcess={handleProcess}
              error={processingError}
              onDismiss={() => {
                setProcessingError(null);
                setScreen('home');
              }}
            />
          )}

          {screen === 'result' && activePayment && (
            <ResultScreen payment={activePayment} onDone={handleResultDone} />
          )}
        </div>
      </div>

      <DemoPanel
        onLargePayment={handleLargePayment}
        onVelocity={handleVelocity}
        onNewDeviceNewPayee={handleNewDeviceNewPayee}
        log={demoLog}
      />
    </div>
  );
}
