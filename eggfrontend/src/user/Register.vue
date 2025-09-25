<template>
  <div class="auth-container">
    <el-card class="auth-card">
      <h2 style="text-align:center;margin-bottom:20px">注册</h2>
      <el-form :model="form" ref="registerForm" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="form.username"></el-input>
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email"></el-input>
        </el-form-item>
        <el-form-item label="密码">
          <el-input type="password" v-model="form.password"></el-input>
        </el-form-item>
        <el-form-item label="年龄">
          <el-input type="number" v-model="form.age"></el-input>
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="form.gender">
            <el-radio :label="1">男</el-radio>
            <el-radio :label="0">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submit" style="width:100%">注册</el-button>
        </el-form-item>
      </el-form>
      <div style="text-align:center">
        <span>已有账号？</span>
        <el-link type="primary" @click="goLogin">去登录</el-link>
      </div>
    </el-card>
  </div>
</template>

<script>
import egg from '@/utils/egg'

export default {
  name: 'UserRegister',
  data() {
    return {
      form: {
        username: '',
        email: '',
        password: '',
        age: 18,
        gender: 1
      }
    }
  },
  methods: {
    async submit() {
      try {
        // 设置默认值
        if (this.form.age === null) this.form.age = 18
        if (this.form.gender === null) this.form.gender = 1

        const res = await egg.post('/user/register', this.form)
        this.$message.success(res.message || '注册成功')
        this.$router.push('/login')
      }catch (e) {
        // 已有拦截器处理弹窗，这里无需写东西
      }
    },

    goLogin() {
      this.$router.push('/login')
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
