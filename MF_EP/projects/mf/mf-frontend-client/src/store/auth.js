import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('clientToken') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('clientUser') || 'null'))

  function setToken(val) { token.value = val; localStorage.setItem('clientToken', val) }
  function setUserInfo(info) { userInfo.value = info; localStorage.setItem('clientUser', JSON.stringify(info)) }
  function logout() {
    const id = userInfo.value?.id || userInfo.value?.userId
    if (id) {
      Object.keys(localStorage)
        .filter(key => key === `mf-ai-active-session:${id}` || key.startsWith(`mf-ai-chat:${id}:`))
        .forEach(key => localStorage.removeItem(key))
    }
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('clientToken')
    localStorage.removeItem('clientUser')
  }

  return { token, userInfo, setToken, setUserInfo, logout }
})
