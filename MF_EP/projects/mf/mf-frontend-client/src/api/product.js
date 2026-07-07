import request from '@/utils/request'

export function getProductList(params) { return request({ url: '/client/products', method: 'get', params }) }
export function getProductDetail(id) { return request({ url: `/client/products/${id}`, method: 'get' }) }
export function getCategories(type) { return request({ url: '/client/products/categories', method: 'get', params: { type } }) }
