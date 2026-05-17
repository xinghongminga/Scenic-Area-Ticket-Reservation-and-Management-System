<script setup>
import { reactive, ref } from 'vue';
import { ElMessageBox } from 'element-plus';
import { request } from '../../api/http';

const form = reactive({ mode: 'qr', verifyCode: '', qrCode: '', method: '二维码' });
const uploading = ref(false);

function buildResultText(data) {
  return `订单号：${data?.orderNo || '-'}\n票状态：${data?.ticketStatus || '-'}\n订单状态：${data?.orderStatus || '-'}`;
}

async function submit() {
  try {
    const data = form.mode === 'code'
      ? await request('/api/verify/byCode', { method: 'POST', body: { verifyCode: form.verifyCode, method: form.method } })
      : await request('/api/verify/byQr', { method: 'POST', body: { qrCode: form.qrCode, method: form.method } });
    await ElMessageBox.alert(buildResultText(data), '核验成功', { type: 'success' });
  } catch (e) {
    await ElMessageBox.alert(e.message || '核验失败', '核验失败', { type: 'error' });
  }
}

async function onQrImageChange(uploadFile) {
  const rawFile = uploadFile?.raw;
  if (!rawFile) {
    return;
  }

  const body = new FormData();
  body.append('file', rawFile);
  body.append('method', form.method || '二维码图片');

  uploading.value = true;
  try {
    const data = await request('/api/verify/byQrImage', { method: 'POST', body });
    await ElMessageBox.alert(buildResultText(data), '核验成功', { type: 'success' });
  } catch (e) {
    await ElMessageBox.alert(e.message || '核验失败', '核验失败', { type: 'error' });
  } finally {
    uploading.value = false;
  }
}
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <div class="head">
        <h3>核验入园</h3>
        <p>支持核验码、二维码文本和二维码图片三种方式。</p>
      </div>
    </template>

    <div class="inline">
      <el-select v-model="form.mode" placeholder="模式">
        <el-option label="核验码" value="code" />
        <el-option label="二维码" value="qr" />
      </el-select>
      <el-input v-if="form.mode==='code'" v-model="form.verifyCode" placeholder="请输入核验码" />
      <el-input v-else v-model="form.qrCode" placeholder="请输入二维码内容" />
      <el-select v-model="form.method" placeholder="方式">
        <el-option label="二维码" value="二维码" />
        <el-option label="人工核验" value="人工核验" />
        <el-option label="二维码图片" value="二维码图片" />
      </el-select>
      <el-button type="primary" @click="submit">核验</el-button>
    </div>
    <div class="upload-row" v-if="form.mode==='qr'">
      <el-upload
        :auto-upload="false"
        :show-file-list="false"
        accept="image/*"
        :on-change="onQrImageChange"
      >
        <el-button :loading="uploading" type="success">上传二维码图片检票</el-button>
      </el-upload>
      <span class="tips">选择二维码图片后会自动识别并核验。</span>
    </div>
  </el-card>
</template>

<style scoped>
.el-card {
  border-radius: 16px;
}
.head { display: flex; align-items: flex-start; justify-content: space-between; }
.head h3 { margin: 0; font-size: 22px; font-weight: 700; color: #0f172a; }
.head p { margin: 8px 0 0; color: #64748b; font-size: 15px; }
.inline { display: grid; grid-template-columns: 140px 1fr 180px 100px; gap: 8px; margin-top: 4px; }
.upload-row { display: flex; gap: 12px; align-items: center; margin-top: 12px; }
.tips { color: #64748b; font-size: 13px; }

@media (max-width: 768px) {
  .inline { grid-template-columns: 1fr; }
  .upload-row { align-items: flex-start; flex-direction: column; }
}
</style>
