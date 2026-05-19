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
const pwdForm = reactive({ role: 'ADMIN', username: 'admin', password: '123456' });

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
      <div class="hero">
        <span class="badge">Scenic Console</span>
        <h1>景区票务与客流管控后台</h1>
        <p>请选择身份后登录，进入门票、订单、售后与监控管理工作台</p>
      </div>

      <el-form label-position="top" class="form">
        <el-form-item label="身份">
          <el-select v-model="pwdForm.role" placeholder="请选择身份" popper-class="login-select-popper">
            <el-option v-for="item in roleOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="pwdForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="pwdForm.password" placeholder="请输入密码" type="password" show-password />
        </el-form-item>
        <el-button type="primary" :loading="loading" class="submit" @click="loginByPassword">登录系统</el-button>
      </el-form>
    </section>
  </main>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background:
    radial-gradient(circle at top left, rgba(255, 255, 255, 0.8), transparent 28%),
    radial-gradient(circle at bottom right, rgba(255, 255, 255, 0.35), transparent 32%),
    linear-gradient(140deg, #edf2f7 0%, #dbeafe 48%, #eef2ff 100%);
  font-family: 'Segoe UI', 'Microsoft YaHei', sans-serif;
}
.login-card {
  width: min(560px, 92vw);
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(18px);
  border: 1px solid rgba(255, 255, 255, 0.55);
  border-radius: 24px;
  padding: 32px;
  box-shadow: 0 18px 60px rgba(15, 23, 42, 0.16);
}
.hero {
  display: grid;
  gap: 10px;
  margin-bottom: 22px;
}
.badge {
  width: fit-content;
  padding: 6px 12px;
  border-radius: 999px;
  background: linear-gradient(90deg, #1d4ed8, #2563eb);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}
.hero h1 {
  margin: 0;
  color: #0f172a;
  font-size: 28px;
  line-height: 1.2;
}
.hero p {
  margin: 0;
  color: #475569;
  font-size: 15px;
  line-height: 1.7;
}
.form {
  display: grid;
  gap: 14px;
}
.submit {
  width: 100%;
  margin-top: 6px;
  height: 48px;
  border: none;
  border-radius: 14px;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.04em;
  background: linear-gradient(90deg, #2563eb, #1d4ed8);
  box-shadow: 0 12px 24px rgba(37, 99, 235, 0.28);
}

:deep(.el-form-item__label) {
  font-size: 14px;
  color: #334155;
  padding-bottom: 6px;
}

:deep(.el-form-item) {
  margin-bottom: 0;
}

:deep(.el-input__wrapper),
:deep(.el-select__wrapper) {
  min-height: 46px;
  border-radius: 14px;
  box-shadow: inset 0 0 0 1px #dbe3ef;
  background: rgba(255, 255, 255, 0.95);
  transition: box-shadow 0.2s ease, transform 0.2s ease, background 0.2s ease;
}

:deep(.el-input__wrapper.is-focus),
:deep(.el-select__wrapper.is-focused) {
  box-shadow: inset 0 0 0 1px #3b82f6, 0 0 0 4px rgba(59, 130, 246, 0.12);
  background: #fff;
}

:deep(.el-input__inner) {
  font-size: 15px;
  color: #0f172a;
}

:deep(.el-select__selected-item),
:deep(.el-select__placeholder),
:deep(.el-input__placeholder) {
  font-size: 15px;
}

:deep(.el-select-dropdown__item) {
  border-radius: 10px;
  margin: 4px 8px;
  font-size: 14px;
}

:deep(.el-select-dropdown__item.selected) {
  color: #2563eb;
  font-weight: 600;
}

:deep(.el-button--primary:hover) {
  transform: translateY(-1px);
}

:deep(.el-button--primary.is-loading) {
  opacity: 0.92;
}

:deep(.el-select) {
  width: 100%;
}

@media (max-width: 640px) {
  .login-card { padding: 24px; border-radius: 20px; }
  .hero h1 { font-size: 24px; }
  .hero p { font-size: 14px; }
}
</style>
