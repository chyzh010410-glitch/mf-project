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
  pending_ship: '',
  shipped: '',
  completed: 'success',
  refund_requested: 'warning',
  refunded: 'info',
  cancelled: 'info'
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
