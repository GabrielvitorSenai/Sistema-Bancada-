import axios from 'axios'

export const api = axios.create({
  baseURL: '',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
})

api.interceptors.response.use(
  response => response,
  error => {
    const message =
      error.response?.data ||
      error.message ||
      'Erro inesperado ao comunicar com o backend.'

    return Promise.reject({
      status: error.response?.status,
      message,
      original: error,
    })
  },
)