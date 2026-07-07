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

export function parseImageList(value) {
  if (!value) return []
  if (Array.isArray(value)) return value.filter(Boolean)
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed.filter(Boolean) : []
  } catch {
    return value.split(',').map(item => item.trim()).filter(Boolean)
  }
}

export function firstImage(record) {
  const images = parseImageList(record?.images)
  return record?.coverImage || images[0] || ''
}
