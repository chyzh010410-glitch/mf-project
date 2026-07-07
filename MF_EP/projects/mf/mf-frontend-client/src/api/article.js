import request from '@/utils/request'
export function getArticleList(params) { return request({ url: '/client/articles', method: 'get', params }) }
export function getArticleDetail(id) { return request({ url: '/client/articles/'+id, method: 'get' }) }
