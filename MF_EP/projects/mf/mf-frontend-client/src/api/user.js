import request from '@/utils/request'
export function getProfile() { return request({ url: '/client/user/profile', method: 'get' }) }
export function updateProfile(data) { return request({ url: '/client/user/profile', method: 'put', data }) }
export function changePassword(data) { return request({ url: '/client/user/password', method: 'put', data }) }
