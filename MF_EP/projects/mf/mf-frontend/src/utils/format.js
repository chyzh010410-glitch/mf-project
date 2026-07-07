export function formatAmount(value) {
  const amount = Number(value)
  return Number.isFinite(amount) ? amount.toFixed(2) : '0.00'
}

export function formatCurrency(value) {
  return `¥${formatAmount(value)}`
}

export function resolveImageUrl(url) {
  if (!url) return ''
  if (/^https?:\/\//i.test(url)) return url
  return `http://localhost:8080${url.startsWith('/') ? url : `/${url}`}`
}
