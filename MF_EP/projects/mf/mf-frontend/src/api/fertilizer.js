import request from '@/utils/request'

export function getFertilizerPage(params) {
  return request({
    url: '/fertilizer/page',
    method: 'get',
    params
  })
}

export function getFertilizerList() {
  return request({
    url: '/fertilizer/list',
    method: 'get'
  })
}

export function getFertilizerById(id) {
  return request({
    url: `/fertilizer/${id}`,
    method: 'get'
  })
}

export function saveFertilizer(data) {
  return request({
    url: '/fertilizer',
    method: 'post',
    data
  })
}

export function updateFertilizer(data) {
  return request({
    url: '/fertilizer',
    method: 'put',
    data
  })
}

export function deleteFertilizer(id) {
  return request({
    url: `/fertilizer/${id}`,
    method: 'delete'
  })
}
