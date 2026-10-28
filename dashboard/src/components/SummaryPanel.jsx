import { useState } from 'react'
import { api } from '../api'

export function SummaryPanel() {
  const [loading, setLoading] = useState(false)
  const [result, setResult] = useState(null)
  const [error, setError] = useState(null)

  async function handleClick() {
    setLoading(true)
    setError(null)
    try {
      const response = await api.summarize()
      setResult(response)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="card">
      <p className="panel-title">AI summary of the last hour</p>
      <button type="button" className="summary-button" onClick={handleClick} disabled={loading}>
        {loading ? 'Asking the model…' : 'Summarize last hour'}
      </button>
      {error && <p className="empty-state">{error}</p>}
      {result && (
        <>
          <p className="summary-text">{result.summary}</p>
          <p className="stat-sub">{result.heldCount} held in the last hour</p>
        </>
      )}
    </div>
  )
}
