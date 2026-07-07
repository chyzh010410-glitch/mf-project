import request from '@/utils/request'
export function checkLike(targetType, targetId) { return request({ url:'/client/likes/check', method:'get', params:{targetType,targetId} }) }
export function toggleLike(data) { return request({ url:'/client/likes', method:'post', data }) }
