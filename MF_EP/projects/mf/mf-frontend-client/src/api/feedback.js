import request from '@/utils/request'
export function submitFeedback(data) { return request({ url: '/client/feedback', method: 'post', data }) }
