import { useCallback, useEffect, useState } from 'react'
import { api } from './api'
import { useEventStream } from './useEventStream'
import { StatsStrip } from './components/StatsStrip'
import { PaymentFeed } from './components/PaymentFeed'
import { ReviewQueue } from './components/ReviewQueue'
import { CaseDetail } from './components/CaseDetail'
import { ThroughputChart } from './components/ThroughputChart'
import { RuleBreakdown } from './components/RuleBreakdown'
import { SummaryPanel } from './components/SummaryPanel'
import { ThemeToggle } from './components/ThemeToggle'

const MAX_FEED_ITEMS = 25

function getInitialTheme() {
  try {
    return localStorage.getItem('payguard-theme')
  } catch {
    return null
  }
}

export default function App() {
  const [theme, setTheme] = useState(getInitialTheme)
  const [stats, setStats] = useState(null)
  const [payments, setPayments] = useState([])
  const [filter, setFilter] = useState('PENDING')
  const [alerts, setAlerts] = useState([])
  const [pendingCount, setPendingCount] = useState(0)
  const [selectedId, setSelectedId] = useState(null)
  const [selectedAlert, setSelectedAlert] = useState(null)

  useEffect(() => {
    if (theme) {
      document.documentElement.setAttribute('data-theme', theme)
    } else {
      document.documentElement.removeAttribute('data-theme')
    }
    try {
      if (theme) localStorage.setItem('payguard-theme', theme)
      else localStorage.removeItem('payguard-theme')
    } catch {
      // storage unavailable, ignore
    }
  }, [theme])

  const toggleTheme = () => {
    setTheme((current) => {
      const effective = current || (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light')
      return effective === 'dark' ? 'light' : 'dark'
    })
  }

  const refreshStats = useCallback(() => {
    api.getStats().then(setStats).catch((err) => console.error('Failed to load stats', err))
  }, [])

  const refreshPendingCount = useCallback(() => {
    api.getAlerts('PENDING').then((list) => setPendingCount(list.length)).catch(() => {})
  }, [])

  const loadAlerts = useCallback((f) => {
    const status = f === 'ALL' ? undefined : f
    api.getAlerts(status).then(setAlerts).catch((err) => console.error('Failed to load alerts', err))
  }, [])

  useEffect(() => {
    refreshStats()
    refreshPendingCount()
    const interval = setInterval(refreshStats, 7000)
    return () => clearInterval(interval)
  }, [refreshStats, refreshPendingCount])

  useEffect(() => {
    loadAlerts(filter)
  }, [filter, loadAlerts])

  useEffect(() => {
    if (!selectedId) {
      setSelectedAlert(null)
      return
    }
    api.getAlert(selectedId).then(setSelectedAlert).catch((err) => console.error('Failed to load alert', err))
  }, [selectedId])

  useEventStream({
    'payment.checked': (payment) => {
      setPayments((prev) => [payment, ...prev].slice(0, MAX_FEED_ITEMS))
    },
    'alert.created': (alert) => {
      setPendingCount((c) => c + 1)
      setAlerts((prev) => (filter === 'PENDING' || filter === 'ALL' ? [alert, ...prev] : prev))
    },
    'alert.analyzed': (alert) => {
      setAlerts((prev) => prev.map((a) => (a.id === alert.id ? alert : a)))
      setSelectedAlert((prev) => (prev && prev.id === alert.id ? alert : prev))
    },
    'alert.decided': (alert) => {
      setPendingCount((c) => Math.max(0, c - 1))
      setAlerts((prev) => {
        const matchesFilter = filter === 'ALL' || filter === alert.status
        if (matchesFilter) {
          const exists = prev.some((a) => a.id === alert.id)
          return exists ? prev.map((a) => (a.id === alert.id ? alert : a)) : [alert, ...prev]
        }
        return prev.filter((a) => a.id !== alert.id)
      })
      setSelectedAlert((prev) => (prev && prev.id === alert.id ? alert : prev))
    },
  })

  async function handleDecide(id, decision) {
    const updated = await api.decide(id, decision)
    setSelectedAlert(updated)
  }

  return (
    <div className="app">
      <header className="app-header">
        <div>
          <h1>PayGuard · Fraud Review</h1>
          <div className="subtitle">Live UPI payment monitoring</div>
        </div>
        <ThemeToggle theme={theme || 'light'} onToggle={toggleTheme} />
      </header>

      <StatsStrip stats={stats} pendingCount={pendingCount} />

      <div className="main-grid">
        <PaymentFeed payments={payments} />
        <ReviewQueue
          alerts={alerts}
          filter={filter}
          onFilterChange={setFilter}
          selectedId={selectedId}
          onSelect={setSelectedId}
        />
        <CaseDetail alert={selectedAlert} onDecide={handleDecide} />
      </div>

      <div className="charts-row">
        <ThroughputChart points={stats?.throughput} />
        <RuleBreakdown countsByRule={stats?.countsByRule} />
      </div>

      <div style={{ marginTop: 16 }}>
        <SummaryPanel />
      </div>
    </div>
  )
}
