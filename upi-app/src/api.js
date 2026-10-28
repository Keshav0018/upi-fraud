const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080';

async function parseJsonOrThrow(res) {
  let data = null;
  try {
    data = await res.json();
  } catch {
    // no body
  }
  if (!res.ok) {
    const err = new Error((data && data.message) || `Request failed with status ${res.status}`);
    err.status = res.status;
    err.fieldErrors = data && data.fieldErrors;
    throw err;
  }
  return data;
}

export async function createPayment({ payerVpa, payeeVpa, amount, note, deviceId }) {
  const res = await fetch(`${API_BASE}/api/payments`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ payerVpa, payeeVpa, amount, note, deviceId }),
  });
  return parseJsonOrThrow(res);
}

export async function getPayment(id) {
  const res = await fetch(`${API_BASE}/api/payments/${id}`);
  return parseJsonOrThrow(res);
}

export async function listPayments(payerVpa) {
  const res = await fetch(`${API_BASE}/api/payments?payerVpa=${encodeURIComponent(payerVpa)}`);
  return parseJsonOrThrow(res);
}

export const FINAL_STATUSES = new Set(['COMPLETED', 'BLOCKED']);
