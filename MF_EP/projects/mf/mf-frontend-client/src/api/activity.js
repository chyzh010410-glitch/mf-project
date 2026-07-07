import request from '@/utils/request'
export function getActivities() { return request({ url: '/client/activities', method: 'get' }) }
export function getActivityDetail(id) { return request({ url: '/client/activities/'+id, method: 'get' }) }
