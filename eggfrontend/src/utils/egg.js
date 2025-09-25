import axios from 'axios'
import { ElMessage } from 'element-plus'

// 创建 axios 实例
const egg = axios.create({
    baseURL: 'http://localhost:8090',
    timeout: 10000
})

// 请求拦截器：自动带 token
egg.interceptors.request.use(
    config => {
        const token = localStorage.getItem('token')
        if (token) {
            config.headers['Authorization'] = 'Bearer ' + token
        }
        return config
    },
    error => Promise.reject(error)
)

// 响应拦截器：自动解封装 Result + 全局错误弹窗
egg.interceptors.response.use(
    response => {
        const res = response.data
        if (res.success) {
            ElMessage.success(res.message || '您又操作成功了！~~');
            return res.data
        } else {
            ElMessage.error('业务异常: ' + (res.message || '未知错误'))
            return Promise.reject(new Error(res.message || 'Error'))
        }
    },
    error => {
        // 如果是后端返回的 4xx / 5xx
        if (error.response && error.response.data) {
            const msg = error.response.data.message || error.response.statusText
            ElMessage.error('业务异常: ' + msg)
            return Promise.reject(new Error(msg))
        } else {
            ElMessage.error('系统内部异常或网络错误: ' + (error.message || '未知错误'))
            return Promise.reject(error)
        }
    }
)

export default egg
