import request from '@/utils/request'

export function login(data) { return request({ url: '/client/auth/login', method: 'post', data }) }
export function register(data) { return request({ url: '/client/auth/register', method: 'post', data }) }
export function sendCode(data) { return request({ url: '/client/auth/captcha', method: 'post', data }) }
export function resetPassword(data) { return request({ url: '/client/auth/reset-password', method: 'post', data }) }
