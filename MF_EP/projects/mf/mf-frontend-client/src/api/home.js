import request from '@/utils/request'
export function getHomeData() { return request({ url: '/client/home', method: 'get' }) }
