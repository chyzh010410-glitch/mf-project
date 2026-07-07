import request from '@/utils/request'

export const registerMerchant = (data) => request.post('/merchant/auth/register', data)
export const loginMerchant = (data) => request.post('/merchant/auth/login', data)
export const logoutMerchant = () => request.post('/merchant/auth/logout')

export const getProfile = () => request.get('/merchant/profile')
export const updateProfile = (data) => request.put('/merchant/profile', data)

export const getProductPage = (params) => request.get('/merchant/products', { params })
export const getProductDetail = (id) => request.get(`/merchant/products/${id}`)
export const createProduct = (data) => request.post('/merchant/products', data)
export const updateProduct = (id, data) => request.put(`/merchant/products/${id}`, data)
export const deleteProduct = (id) => request.delete(`/merchant/products/${id}`)

export const getOrderPage = (params) => request.get('/merchant/orders', { params })
export const getOrderDetail = (id) => request.get(`/merchant/orders/${id}`)
export const shipOrder = (id, data) => request.post(`/merchant/orders/${id}/ship`, data)

export const uploadProductImage = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/merchant/uploads/product-image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
