import request from '@/utils/request'

export function getTreePage(params) {
  return request({ url: '/tree/page', method: 'get', params })
}

export function getTreeById(id) {
  return request({ url: `/tree/${id}`, method: 'get' })
}

export function getTreeSpecies() {
  return request({ url: '/tree/species', method: 'get' })
}

export function saveTree(data) {
  return request({ url: '/tree', method: 'post', data })
}

export function updateTree(data) {
  return request({ url: '/tree', method: 'put', data })
}

export function deleteTree(id) {
  return request({ url: `/tree/${id}`, method: 'delete' })
}
