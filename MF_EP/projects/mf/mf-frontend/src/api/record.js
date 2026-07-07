import request from '@/utils/request'

export function getRecordPage(params) {
  return request({ url: '/record/page', method: 'get', params })
}

export function getRecordById(id) {
  return request({ url: `/record/${id}`, method: 'get' })
}

export function saveRecord(data) {
  return request({ url: '/record', method: 'post', data })
}

export function deleteRecord(id) {
  return request({ url: `/record/${id}`, method: 'delete' })
}

export function getRecordStats() {
  return request({ url: '/record/stats', method: 'get' })
}
