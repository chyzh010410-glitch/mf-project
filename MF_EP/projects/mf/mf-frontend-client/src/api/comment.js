import request from '@/utils/request'
export function getComments(params) { return request({ url:'/client/comments', method:'get', params }) }
export function getReplies(id) { return request({ url:`/client/comments/${id}/replies`, method:'get' }) }
export function postComment(data) { return request({ url:'/client/comments', method:'post', data }) }
