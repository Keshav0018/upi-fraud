import { useEffect, useRef } from 'react';

export default function ProcessingScreen({ onProcess, error, onDismiss }) {
  const fired = useRef(false);

  useEffect(() => {
    if (!fired.current) {
      fired.current = true;
      onProcess();
    }
  }, [onProcess]);

  if (error) {
    return (
      <div className="centered-screen">
        <div className="result-icon blocked">!</div>
        <div className="result-title">Something went wrong</div>
        <div className="result-subtitle">{error}</div>
        <button className="btn btn-primary" onClick={onDismiss}>Back to home</button>
      </div>
    );
  }

  return (
    <div className="centered-screen">
      <div className="spinner" />
      <div className="screen-title">Processing payment…</div>
    </div>
  );
}
