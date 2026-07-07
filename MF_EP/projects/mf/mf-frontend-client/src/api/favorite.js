import request from '@/utils/request'
export function getFavorites(params) { return request({ url: '/client/favorites', method: 'get', params }) }
export function addFavorite(data) { return request({ url: '/client/favorites', method: 'post', data }) }
export function removeFavorite(id) { return request({ url: '/client/favorites/' + id, method: 'delete' }) }
export function checkFavorited(targetType, targetId) {
  return request({ url: '/client/favorites', method: 'get', params: { targetType, targetId, page: 1, size: 1 } })
}
