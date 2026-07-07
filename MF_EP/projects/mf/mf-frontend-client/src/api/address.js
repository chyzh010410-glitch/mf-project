import request from '@/utils/request'
export function getAddresses() { return request({ url: '/client/addresses', method: 'get' }) }
export function addAddress(data) { return request({ url: '/client/addresses', method: 'post', data }) }
export function updateAddress(id, data) { return request({ url: '/client/addresses/'+id, method: 'put', data }) }
export function deleteAddress(id) { return request({ url: '/client/addresses/'+id, method: 'delete' }) }
export function setDefaultAddress(id) { return request({ url: '/client/addresses/'+id+'/default', method: 'put' }) }
