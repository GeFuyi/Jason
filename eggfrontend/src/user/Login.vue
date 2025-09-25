<template>
  <div class="auth-container">
    <el-card class="auth-card">
      <h2 style="text-align:center;margin-bottom:20px">登录</h2>
      <el-form :model="form" ref="loginForm" label-width="80px">
        <el-form-item label="用户名/邮箱">
          <el-input v-model="form.username"></el-input>
        </el-form-item>
        <el-form-item label="密码">
          <el-input type="password" v-model="form.password"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submit" style="width:100%">登录</el-button>
        </el-form-item>
      </el-form>
      <div style="text-align:center">
        <span>还没有账号？</span>
        <el-link type="primary" @click="goRegister">去注册</el-link>
      </div>
    </el-card>
  </div>
</template>

<script>
import egg from '@/utils/egg'

export default {
  name: 'UserLogin',
  data() {
    return {
      form: {
        username: '',
        password: ''
      }
    }
  },
  methods: {
    async submit() {
      try {
        // 直接 await，出错会被拦截器弹窗
        const res = await egg.post('/user/login', this.form)

        // 成功后处理 token
        if (res.token) localStorage.setItem('token', res.token)
        if (res.userId) sessionStorage.setItem('userId', res.userId)

        this.$message.success(res.message || '登录成功')
        this.$router.push('/chatroom')
      }catch (e) {
        // 已有拦截器处理弹窗，这里无需写东西
      }
    },

    goRegister() {
      this.$router.push('/register')
    }
  }
}
</script>

<style scoped>
.auth-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: #f5f5f5;
}

.auth-card {
  width: 400px;
  padding: 30px;
}
</style>
