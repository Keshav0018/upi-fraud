import { useEffect, useRef, useState } from 'react'
import { AlertStatusBadge } from './StatusBadge'
import { placeholderAnalysis } from '../placeholderAnalysis'

function formatAmount(amount) {
  return `₹${Number(amount).toLocaleString('en-IN')}`
}

function isTypingInField() {
  const tag = document.activeElement?.tagName
  return tag === 'INPUT' || tag === 'TEXTAREA' || document.activeElement?.isContentEditable
}

export function CaseDetail({ alert, onDecide }) {
  const [busy, setBusy] = useState(false)
  const busyRef = useRef(false)

  const decidable = alert && alert.status === 'PENDING'

  useEffect(() => {
    if (!decidable) return
    const handler = (event) => {
      if (isTypingInField() || busyRef.current) return
      if (event.key === 'f' || event.key === 'F') {
        handleDecide('FRAUD')
      } else if (event.key === 'c' || event.key === 'C') {
        handleDecide('CLEARED')
      }
    }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [alert?.id, decidable])

  if (!alert) {
    return (
      <div className="card case-detail">
        <p className="panel-title">Case detail</p>
        <p className="empty-state">Select a case from the review queue.</p>
      </div>
    )
  }

  async function handleDecide(decision) {
    if (busyRef.current || alert.status !== 'PENDING') return
    busyRef.current = true
    setBusy(true)
    try {
      await onDecide(alert.id, decision)
    } finally {
      busyRef.current = false
      setBusy(false)
    }
  }

  return (
    <div className="card case-detail">
      <p className="panel-title">Case detail</p>
      <h3>
        {alert.payerVpa} → {alert.payeeVpa}
      </h3>
      <div className="case-sub">
        {formatAmount(alert.amount)} · <AlertStatusBadge status={alert.status} />
      </div>

      <div>
        {alert.reasons.map((reason) => (
          <span className="reason-chip" key={reason}>
            {reason}
          </span>
        ))}
      </div>

      <div className="snapshot-grid">
        <div>
          <span>Average amount</span>
          ₹{Math.round(alert.profileSnapshot?.averageAmount ?? 0).toLocaleString('en-IN')}
        </div>
        <div>
          <span>Known devices</span>
          {alert.profileSnapshot?.knownDeviceCount ?? 0}
        </div>
        <div>
          <span>Known payees</span>
          {alert.profileSnapshot?.knownPayeeCount ?? 0}
        </div>
        <div>
          <span>Recent payments (10s)</span>
          {alert.profileSnapshot?.recentPaymentCount ?? 0}
        </div>
      </div>

      <div className="ai-box">
        {alert.aiStatus === 'PENDING' && <p className="empty-state loading-dots">Running AI analysis</p>}
        {alert.aiStatus === 'DONE' && (
          <>
            <p className="ai-risk">
              {alert.aiRiskLevel} risk · recommend {alert.aiRecommendation}
            </p>
            <p className="summary-text">{alert.aiSummary}</p>
            {alert.aiSignals?.length > 0 && (
              <ul className="signal-list">
                {alert.aiSignals.map((signal, i) => (
                  <li key={i}>{signal}</li>
                ))}
              </ul>
            )}
          </>
        )}
        {alert.aiStatus === 'FAILED' &&
          (() => {
            const analysis = placeholderAnalysis(alert)
            return (
              <>
                <p className="ai-risk">
                  {analysis.riskLevel} risk · recommend {analysis.recommendation}
                </p>
                <p className="summary-text">{analysis.summary}</p>
                {analysis.signals.length > 0 && (
                  <ul className="signal-list">
                    {analysis.signals.map((signal, i) => (
                      <li key={i}>{signal}</li>
                    ))}
                  </ul>
                )}
              </>
            )
          })()}
      </div>

      <div className="decision-buttons">
        <button
          type="button"
          className="btn-fraud"
          disabled={!decidable || busy}
          onClick={() => handleDecide('FRAUD')}
        >
          Mark as fraud <span className="kbd">F</span>
        </button>
        <button
          type="button"
          className="btn-clear"
          disabled={!decidable || busy}
          onClick={() => handleDecide('CLEARED')}
        >
          Clear payment <span className="kbd">C</span>
        </button>
      </div>
    </div>
  )
}
