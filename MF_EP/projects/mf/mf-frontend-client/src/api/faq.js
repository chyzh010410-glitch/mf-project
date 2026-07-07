import request from '@/utils/request'
export function getFaqList(params) { return request({ url: '/client/faq', method: 'get', params }) }
