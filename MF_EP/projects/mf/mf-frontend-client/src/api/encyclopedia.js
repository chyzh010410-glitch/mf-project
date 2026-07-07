import request from '@/utils/request'
export function getEncyclopediaList(params) { return request({ url: '/client/encyclopedia', method: 'get', params }) }
export function getEncyclopediaDetail(id) { return request({ url: '/client/encyclopedia/'+id, method: 'get' }) }
