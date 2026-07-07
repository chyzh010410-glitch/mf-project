import request from '@/utils/request'

export function getPublicConfig() {
  return request({ url: '/client/configs/public', method: 'get' })
}
