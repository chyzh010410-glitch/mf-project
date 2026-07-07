import request from '@/utils/request'
export function getMessages(params) { return request({ url:'/client/messages', method:'get', params }) }
export function getUnreadCount() { return request({ url:'/client/messages/unread-count', method:'get' }) }
export function markRead(id) { return request({ url:`/client/messages/${id}/read`, method:'put' }) }
