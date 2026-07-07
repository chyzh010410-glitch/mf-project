import request from '@/utils/request'

// ==================== 商品管理 ====================
export const getProductPage = (params) => request.get('/admin/products', { params })
export const getProductDetail = (id) => request.get(`/admin/products/${id}`)
export const saveProduct = (data) => request.post('/admin/products', data)
export const updateProduct = (id, data) => request.put(`/admin/products/${id}`, data)
export const deleteProduct = (id) => request.delete(`/admin/products/${id}`)
export const toggleProductStatus = (id, status) => request.put(`/admin/products/${id}/status`, { status })
export const toggleProductRecommend = (id) => request.put(`/admin/products/${id}/recommend`)
export const toggleProductNew = (id) => request.put(`/admin/products/${id}/new`)

// ==================== 分类管理 ====================
export const getCategoryPage = (params) => request.get('/admin/categories', { params })
export const getCategoryDetail = (id) => request.get(`/admin/categories/${id}`)
export const saveCategory = (data) => request.post('/admin/categories', data)
export const updateCategory = (id, data) => request.put(`/admin/categories/${id}`, data)
export const deleteCategory = (id) => request.delete(`/admin/categories/${id}`)

// ==================== 百科管理 ====================
export const getEncyclopediaPage = (params) => request.get('/admin/encyclopedia', { params })
export const getEncyclopediaDetail = (id) => request.get(`/admin/encyclopedia/${id}`)
export const saveEncyclopedia = (data) => request.post('/admin/encyclopedia', data)
export const updateEncyclopedia = (id, data) => request.put(`/admin/encyclopedia/${id}`, data)
export const deleteEncyclopedia = (id) => request.delete(`/admin/encyclopedia/${id}`)
export const toggleEncyclopediaPublish = (id) => request.put(`/admin/encyclopedia/${id}/publish`)

// ==================== 文章管理 ====================
export const getArticlePage = (params) => request.get('/admin/articles', { params })
export const getArticleDetail = (id) => request.get(`/admin/articles/${id}`)
export const saveArticle = (data) => request.post('/admin/articles', data)
export const updateArticle = (id, data) => request.put(`/admin/articles/${id}`, data)
export const deleteArticle = (id) => request.delete(`/admin/articles/${id}`)
export const toggleArticlePublish = (id) => request.put(`/admin/articles/${id}/publish`)
export const toggleArticleTop = (id) => request.put(`/admin/articles/${id}/top`)
export const toggleArticleRecommend = (id) => request.put(`/admin/articles/${id}/recommend`)

// ==================== 评论管理 ====================
export const getCommentPage = (params) => request.get('/admin/comments', { params })
export const hideComment = (id) => request.put(`/admin/comments/${id}/hide`)
export const restoreComment = (id) => request.put(`/admin/comments/${id}/restore`)

// ==================== 用户上传审核 ====================
export const getUploadPage = (params) => request.get('/admin/uploads', { params })
export const reviewUpload = (id, data) => request.put(`/admin/uploads/${id}/review`, data)
export const uploadAdminImage = (file, purpose = 'product_image') => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/admin/uploads/image', formData, {
    params: { purpose },
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// ==================== 用户管理 ====================
export const getUserPage = (params) => request.get('/admin/users', { params })
export const toggleUserStatus = (id, status) => request.put(`/admin/users/${id}/status`, { status })

// ==================== 商家管理 ====================
export const getMerchantPage = (params) => request.get('/admin/merchants', { params })
export const getMerchantDetail = (id) => request.get(`/admin/merchants/${id}`)
export const approveMerchant = (id) => request.post(`/admin/merchants/${id}/approve`)
export const rejectMerchant = (id, auditRemark) => request.post(`/admin/merchants/${id}/reject`, { auditRemark })
export const disableMerchant = (id) => request.post(`/admin/merchants/${id}/disable`)

// ==================== 反馈处理 ====================
export const getFeedbackPage = (params) => request.get('/admin/feedbacks', { params })
export const replyFeedback = (id, reply) => request.put(`/admin/feedbacks/${id}/reply`, { reply })

// ==================== FAQ管理 ====================
export const getFaqPage = (params) => request.get('/admin/faqs', { params })
export const getFaqDetail = (id) => request.get(`/admin/faqs/${id}`)
export const saveFaq = (data) => request.post('/admin/faqs', data)
export const updateFaq = (id, data) => request.put(`/admin/faqs/${id}`, data)
export const deleteFaq = (id) => request.delete(`/admin/faqs/${id}`)

// ==================== 消息推送 ====================
export const getMessagePage = (params) => request.get('/admin/messages', { params })
export const sendMessage = (data) => request.post('/admin/messages', data)

// ==================== 活动管理 ====================
export const getActivityPage = (params) => request.get('/admin/activities', { params })
export const getActivityDetail = (id) => request.get(`/admin/activities/${id}`)
export const saveActivity = (data) => request.post('/admin/activities', data)
export const updateActivity = (id, data) => request.put(`/admin/activities/${id}`, data)
export const deleteActivity = (id) => request.delete(`/admin/activities/${id}`)
export const toggleActivityStatus = (id, status) => request.put(`/admin/activities/${id}/status`, { status })

// ==================== 平台配置 ====================
export const getConfigPage = (params) => request.get('/admin/configs', { params })
export const getConfigDetail = (id) => request.get(`/admin/configs/${id}`)
export const saveConfig = (data) => request.post('/admin/configs', data)
export const updateConfig = (id, data) => request.put(`/admin/configs/${id}`, data)
export const deleteConfig = (id) => request.delete(`/admin/configs/${id}`)

// ==================== 管理员管理 ====================
export const getAdminPage = (params) => request.get('/admin/admins', { params })
export const getAdminDetail = (id) => request.get(`/admin/admins/${id}`)
export const saveAdmin = (data) => request.post('/admin/admins', data)
export const updateAdmin = (id, data) => request.put(`/admin/admins/${id}`, data)
export const toggleAdminStatus = (id, status) => request.put(`/admin/admins/${id}/status`, { status })
export const deleteAdmin = (id) => request.delete(`/admin/admins/${id}`)

// ==================== 系统日志 ====================
export const getLogPage = (params) => request.get('/admin/logs', { params })

export const getOrderPage = (params) => request.get('/admin/orders', { params })
export const getOrderDetail = (id) => request.get(`/admin/orders/${id}`)
export const getOrderStatistics = () => request.get('/admin/orders/statistics')
export const shipOrder = (id, data) => request.post(`/admin/orders/${id}/ship`, data)
export const updateOrderStatus = (id, status) => request.post(`/admin/orders/${id}/status`, { status })
export const refundOrder = (id, data) => request.post(`/admin/orders/${id}/refund`, data)

export const getPaymentPage = (params) => request.get('/admin/payments', { params })
