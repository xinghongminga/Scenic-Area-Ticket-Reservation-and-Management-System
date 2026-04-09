<script setup>
import { reactive } from 'vue';
import { ElMessage } from 'element-plus';
import { request } from '../../api/http';

const form = reactive({ mode: 'code', verifyCode: '', qrCode: '', method: 'QR' });
const result = reactive({ text: '' });

async function submit() {
  try {
    const data = form.mode === 'code'
      ? await request('/api/verify/byCode', { method: 'POST', body: { verifyCode: form.verifyCode, method: form.method } })
      : await request('/api/verify/byQr', { method: 'POST', body: { qrCode: form.qrCode, method: form.method } });
    result.text = JSON.stringify(data);
    ElMessage.success('核验成功');
  } catch (e) {
    result.text = e.message;
    ElMessage.error(e.message || '核验失败');
  }
}
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <div class="head">
        <h3>核验入园（模拟）</h3>
        <p>支持核验码和二维码两种模式。</p>
      </div>
    </template>

    <div class="inline">
      <el-select v-model="form.mode" placeholder="模式">
        <el-option label="核验码" value="code" />
        <el-option label="二维码" value="qr" />
      </el-select>
      <el-input v-if="form.mode==='code'" v-model="form.verifyCode" placeholder="verifyCode" />
      <el-input v-else v-model="form.qrCode" placeholder="qrCode" />
      <el-select v-model="form.method" placeholder="方式">
        <el-option label="QR" value="QR" />
        <el-option label="ID_CARD" value="ID_CARD" />
        <el-option label="MANUAL" value="MANUAL" />
      </el-select>
      <el-button type="primary" @click="submit">核验</el-button>
    </div>
    <pre>{{ result.text }}</pre>
  </el-card>
</template>

<style scoped>
.head h3 { margin: 0; }
.head p { margin: 8px 0 0; color: #64748b; font-size: 13px; }
.inline { display: grid; grid-template-columns: 140px 1fr 180px 100px; gap: 8px; }
pre { margin-top: 10px; background: #0f172a; color: #d1fae5; padding: 10px; border-radius: 8px; }

@media (max-width: 768px) {
  .inline { grid-template-columns: 1fr; }
}
</style>
