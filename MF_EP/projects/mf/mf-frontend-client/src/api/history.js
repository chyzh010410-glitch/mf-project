import request from '@/utils/request'
export function getHistory(params) { return request({ url: '/client/history', method: 'get', params }) }
export function clearHistory() { return request({ url: '/client/history', method: 'delete' }) }
