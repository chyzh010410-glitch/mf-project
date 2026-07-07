export const orderStatusOptions = [
  { label: '全部', value: '' },
  { label: '待付款', value: 'pending_pay' },
  { label: '待发货', value: 'pending_ship' },
  { label: '已发货', value: 'shipped' },
  { label: '已完成', value: 'completed' },
  { label: '退款申请中', value: 'refund_requested' },
  { label: '已退款', value: 'refunded' },
  { label: '已取消', value: 'cancelled' }
]

export const orderStatusMap = {
  pending_pay: '待付款',
  pending_ship: '待发货',
  shipped: '已发货',
  completed: '已完成',
  refund_requested: '退款申请中',
  refunded: '已退款',
  cancelled: '已取消'
}

export const orderStatusType = {
  pending_pay: 'warning',
  pending_ship: 'info',
  shipped: '',
  completed: 'success',
  refund_requested: 'warning',
  refunded: 'info',
  cancelled: 'danger'
}

export const orderStatusActions = {
  pending_ship: '已标记为已付款，可发货',
  cancelled: '订单已取消',
  shipped: '已标记为已发货',
  completed: '订单已完成',
  refund_requested: '退款申请已提交',
  refunded: '订单已退款'
}

export const paymentMethodMap = {
  wechat: '微信支付',
  alipay: '支付宝',
  mock: '模拟支付',
  manual: '后台标记'
}

export function getOrderStatusLabel(status) {
  return orderStatusMap[status] || status || '-'
}

export function getOrderStatusType(status) {
  return orderStatusType[status] || ''
}

export function getPaymentMethodLabel(method) {
  return paymentMethodMap[method] || method || '-'
}

export function buildOrderStats(data = {}) {
  return [
    { label: '全部', count: data.total || 0 },
    { label: '待付款', count: data.pendingPay || 0 },
    { label: '待发货', count: data.pendingShip || 0 },
    { label: '已发货', count: data.shipped || 0 },
    { label: '已完成', count: data.completed || 0 },
    { label: '退款申请中', count: data.refundRequested || 0 },
    { label: '已退款', count: data.refunded || 0 }
  ]
}
