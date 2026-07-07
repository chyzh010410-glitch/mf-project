import request from '@/utils/request'
export function getPoints() { return request({ url: '/client/points', method: 'get' }) }
export function getPointsRecords(params) { return request({ url: '/client/points/records', method: 'get', params }) }
