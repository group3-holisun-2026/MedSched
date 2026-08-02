// Formatari comune celor trei rapoarte (P1.2). Un singur loc, ca sa nu iasa trei formate
// diferite de procent si doua de bani pe pagini diferite.

const CURRENCY = new Intl.NumberFormat('ro-RO', {
  style: 'currency',
  currency: 'RON',
});

/** Sume de bani: 1234.5 -> "1.234,50 RON". */
export function formatCurrency(value) {
  const numeric = Number(value ?? 0);
  return CURRENCY.format(Number.isFinite(numeric) ? numeric : 0);
}

/**
 * Ratele vin de la backend ca fractie 0..1, nu ca 73.4.
 * Cand numitorul e 0 backend-ul trimite rata 0, dar randul n-are sens procentual —
 * de aceea `hasDenominator: false` afiseaza "—", nu "0.0%" si in niciun caz "NaN%".
 */
export function formatRate(rate, hasDenominator = true) {
  if (!hasDenominator) return '—';
  const numeric = Number(rate ?? 0);
  if (!Number.isFinite(numeric)) return '—';
  return `${(numeric * 100).toFixed(1)}%`;
}

/** 450 -> "7h 30m". Nimeni nu citeste un raport in minute. */
export function formatMinutes(minutes) {
  const total = Math.max(0, Math.round(Number(minutes ?? 0)));
  const hours = Math.floor(total / 60);
  return `${hours}h ${total % 60}m`;
}

/** yyyy-MM-dd pentru <input type="date"> si pentru parametrii from/to. */
export function toIsoDate(date) {
  const pad = (n) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

/** Interval implicit: prima zi a lunii curente -> azi (P1.1). */
export function currentMonthRange() {
  const today = new Date();
  return {
    from: toIsoDate(new Date(today.getFullYear(), today.getMonth(), 1)),
    to: toIsoDate(today),
  };
}
