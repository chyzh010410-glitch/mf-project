import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('clientToken') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('clientUser') || 'null'))

  function setToken(val) { token.value = val; localStorage.setItem('clientToken', val) }
  function setUserInfo(info) { userInfo.value = info; localStorage.setItem('clientUser', JSON.stringify(info)) }
  function logout() { token.value = ''; userInfo.value = null; localStorage.removeItem('clientToken'); localStorage.removeItem('clientUser') }

  return { token, userInfo, setToken, setUserInfo, logout }
})
