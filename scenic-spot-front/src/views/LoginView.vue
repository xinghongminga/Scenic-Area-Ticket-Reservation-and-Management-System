<script setup>
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { request } from '../api/http';
import { saveAuth } from '../utils/auth';

const router = useRouter();
const loading = ref(false);
const roleOptions = [
  { label: '管理员', value: 'ADMIN' },
  { label: '分析员', value: 'ANALYST' },
  { label: '审核员', value: 'AUDITOR' }
];
const pwdForm = reactive({ role: 'ADMIN', username: 'admin', password: 'admin123' });

async function loginByPassword() {
  if (!pwdForm.role) {
    ElMessage.error('请先选择身份');
    return;
  }
  if (!pwdForm.username || !pwdForm.password) {
    ElMessage.error('请输入账号和密码');
    return;
  }
  loading.value = true;
  try {
    const data = await request('/api/auth/loginByPassword', { method: 'POST', body: pwdForm });
    saveAuth(data);
    router.push('/dashboard');
  } catch (e) {
    ElMessage.error(e.message);
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-card el-theme">
      <h1>景区票务与客流管控后台</h1>
      <p>仅支持账号密码登录，登录前请先选择身份</p>

      <el-form label-position="top" class="form">
        <el-form-item label="身份">
          <el-select v-model="pwdForm.role" placeholder="请选择身份">
            <el-option v-for="item in roleOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="pwdForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="pwdForm.password" placeholder="请输入密码" type="password" show-password />
        </el-form-item>
        <el-button type="primary" :loading="loading" class="submit" @click="loginByPassword">登录</el-button>
      </el-form>
    </section>
  </main>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  background: linear-gradient(140deg, #efe9d8, #c8d8e8 60%, #e7b98a);
  font-family: 'Segoe UI', 'Microsoft YaHei', sans-serif;
}
.login-card {
  width: min(560px, 92vw);
  background: rgba(255, 255, 255, 0.94);
  border-radius: 20px;
  padding: 28px;
  box-shadow: 0 12px 40px rgba(41, 49, 78, 0.2);
}
.form {
  display: grid;
  gap: 10px;
}
.submit { width: 100%; margin-top: 2px; }

:deep(.el-form-item) {
  margin-bottom: 12px;
}
</style>
