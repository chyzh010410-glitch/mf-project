import request from '@/utils/request'
export function getCart() { return request({ url: '/client/cart', method: 'get' }) }
export function addToCart(data) { return request({ url: '/client/cart', method: 'post', data }) }
export function updateCartItem(id, data) { return request({ url: `/client/cart/${id}`, method: 'put', data }) }
export function removeCartItem(id) { return request({ url: `/client/cart/${id}`, method: 'delete' }) }
export function clearCart() { return request({ url: '/client/cart', method: 'delete' }) }
