import request from '@/utils/request'

export function getRulePage(params) {
  return request({ url: '/rule/page', method: 'get', params })
}

export function getRuleById(id) {
  return request({ url: `/rule/${id}`, method: 'get' })
}

export function saveRule(data) {
  return request({ url: '/rule', method: 'post', data })
}

export function updateRule(data) {
  return request({ url: '/rule', method: 'put', data })
}

export function deleteRule(id) {
  return request({ url: `/rule/${id}`, method: 'delete' })
}

export function recommend(data) {
  return request({ url: '/rule/recommend', method: 'post', data })
}
