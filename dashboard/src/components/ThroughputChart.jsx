import { useState } from 'react'

const WIDTH = 480
const HEIGHT = 140
const PADDING = 20

function formatTime(iso) {
  try {
    return new Date(iso).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })
  } catch {
    return ''
  }
}

export function ThroughputChart({ points }) {
  const [hoverIndex, setHoverIndex] = useState(null)

  const data = points && points.length > 0 ? points : []
  const max = Math.max(1, ...data.map((p) => p.count))
  const barGap = 3
  const plotWidth = WIDTH - PADDING * 2
  const barWidth = data.length > 0 ? plotWidth / data.length - barGap : 0
  const plotHeight = HEIGHT - PADDING * 2

  return (
    <div className="card">
      <p className="panel-title">Throughput (last 30 × 5s)</p>
      {data.length === 0 ? (
        <p className="empty-state">No throughput data yet.</p>
      ) : (
        <div style={{ position: 'relative' }}>
          <svg width="100%" viewBox={`0 0 ${WIDTH} ${HEIGHT}`} role="img" aria-label="Payments checked per 5 second interval">
            <line
              x1={PADDING}
              y1={HEIGHT - PADDING}
              x2={WIDTH - PADDING}
              y2={HEIGHT - PADDING}
              stroke="var(--baseline)"
              strokeWidth="1"
            />
            {data.map((p, i) => {
              const h = max === 0 ? 0 : (p.count / max) * plotHeight
              const x = PADDING + i * (barWidth + barGap)
              const y = HEIGHT - PADDING - h
              return (
                <rect
                  key={p.intervalStart + i}
                  x={x}
                  y={y}
                  width={Math.max(1, barWidth)}
                  height={Math.max(0, h)}
                  rx={2}
                  fill="var(--series-1)"
                  opacity={hoverIndex === null || hoverIndex === i ? 1 : 0.55}
                  onMouseEnter={() => setHoverIndex(i)}
                  onMouseLeave={() => setHoverIndex(null)}
                />
              )
            })}
          </svg>
          {hoverIndex !== null && data[hoverIndex] && (
            <div
              className="tooltip"
              style={{
                left: `${((hoverIndex + 0.5) / data.length) * 100}%`,
                top: 0,
              }}
            >
              {data[hoverIndex].count} checked · {formatTime(data[hoverIndex].intervalStart)}
            </div>
          )}
        </div>
      )}
    </div>
  )
}
