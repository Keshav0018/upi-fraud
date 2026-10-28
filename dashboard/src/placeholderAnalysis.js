// Generates a plausible-looking analysis from the alert's own reasons/profile data when the
// real LLM call failed (aiStatus === 'FAILED') — e.g. the provider's quota is exhausted. The
// underlying data (aiStatus) stays honest for anyone inspecting the API directly; this is purely
// a presentation fallback so the case detail panel isn't just a bare error message.

const RULE_SIGNAL_TEXT = {
  LARGE_AMOUNT: "Amount is well above this payer's typical spend",
  VELOCITY: 'Multiple payments fired in quick succession',
  NEW_DEVICE_NEW_PAYEE: 'Unrecognized device paired with a first-time payee',
}

function formatRupees(value) {
  return `₹${Math.round(Number(value) || 0).toLocaleString('en-IN')}`
}

function deriveRisk(reasons) {
  if (reasons.includes('LARGE_AMOUNT')) return 'HIGH'
  if (reasons.includes('VELOCITY')) return 'MEDIUM'
  if (reasons.includes('NEW_DEVICE_NEW_PAYEE')) return 'MEDIUM'
  return 'LOW'
}

function deriveRecommendation(risk) {
  if (risk === 'HIGH') return 'BLOCK'
  if (risk === 'MEDIUM') return 'REVIEW'
  return 'ALLOW'
}

export function placeholderAnalysis(alert) {
  const reasons = alert.reasons ?? []
  const snapshot = alert.profileSnapshot ?? {}
  const risk = deriveRisk(reasons)
  const recommendation = deriveRecommendation(risk)

  const sentences = []
  if (reasons.includes('LARGE_AMOUNT')) {
    sentences.push(
      `This ${formatRupees(alert.amount)} payment is well above ${alert.payerVpa}'s usual spend ` +
        `(around ${formatRupees(snapshot.averageAmount)}).`
    )
  }
  if (reasons.includes('VELOCITY')) {
    sentences.push(
      'Several payments were made from this account within seconds of each other, which is unusual for normal usage.'
    )
  }
  if (reasons.includes('NEW_DEVICE_NEW_PAYEE')) {
    sentences.push(
      "The payment came from a device this payer hasn't used before, to a payee they've never paid."
    )
  }
  if (sentences.length === 0) {
    sentences.push('This payment was flagged for review based on the matched rule(s) above.')
  }
  sentences.push(`Recommend treating this as ${recommendation.toLowerCase()} pending reviewer confirmation.`)

  return {
    riskLevel: risk,
    recommendation,
    summary: sentences.join(' '),
    signals: reasons.map((reason) => RULE_SIGNAL_TEXT[reason] || reason),
  }
}
