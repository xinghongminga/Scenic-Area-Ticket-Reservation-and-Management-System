<script setup>
import { onMounted, onBeforeUnmount, reactive, computed, ref } from 'vue';
import { request } from '../../api/http';
import { ElMessage } from 'element-plus';

const AREA_OPTIONS = {
  GATE_IN: { label: '入园监控点', type: 'success' },
  GATE_OUT: { label: '出园监控点', type: 'danger' }
};

const AREA_CODE_OPTIONS = [
  { label: AREA_OPTIONS.GATE_IN.label, value: 'GATE_IN' },
  { label: AREA_OPTIONS.GATE_OUT.label, value: 'GATE_OUT' }
];

const enterForm = reactive({ scenicId: 1, videoPath: '', areaCode: localStorage.getItem('video-job-enter-areaCode') || 'GATE_IN', sampleMs: 1000 });
const exitForm = reactive({ scenicId: 1, videoPath: '', areaCode: localStorage.getItem('video-job-exit-areaCode') || 'GATE_OUT', sampleMs: 1000 });
const state = reactive({ list: [], running: null, deleting: null, uploading: '' });
const pager = reactive({ enterPage: 1, enterPageSize: 10, exitPage: 1, exitPageSize: 10 });
const enterFileRef = ref(null);
const exitFileRef = ref(null);
const pollingJobs = new Map();

async function load() {
  state.list = await request('/api/admin/video-jobs');
  pager.enterPage = 1;
  pager.exitPage = 1;
}

async function createJob(direction) {
  const form = direction === 'ENTER' ? enterForm : exitForm;
  if (!form.videoPath) {
    ElMessage.warning('请先选择视频文件');
    return;
  }
  await request('/api/admin/video-jobs', { method: 'POST', body: { ...form, direction } });
  ElMessage.success(direction === 'ENTER' ? '入园任务已创建' : '出园任务已创建');
  await load();
}

function triggerPick(direction) {
  if (direction === 'ENTER') {
    enterFileRef.value?.click();
  } else {
    exitFileRef.value?.click();
  }
}

async function onPickFile(e, direction) {
  const file = e.target.files?.[0];
  if (!file) return;
  const fd = new FormData();
  fd.append('file', file);
  state.uploading = direction;
  try {
    const data = await request('/api/admin/files/video/upload', { method: 'POST', body: fd });
    if (direction === 'ENTER') {
      enterForm.videoPath = data.path;
    } else {
      exitForm.videoPath = data.path;
    }
    ElMessage.success('视频已上传并自动填充路径');
  } catch (err) {
    ElMessage.error(err.message || '视频上传失败');
  } finally {
    state.uploading = '';
    e.target.value = '';
  }
}

async function runJob(id) {
  state.running = id;
  try {
    await request(`/api/admin/video-jobs/${id}/run`, { method: 'POST', timeoutMs: 30 * 1000 });
    ElMessage.success('任务已提交，后台正在执行');
    startPollingJob(id);
  } catch (e) {
    ElMessage.error(e.message || '执行失败');
  } finally {
    state.running = null;
    await load();
  }
}

function startPollingJob(id) {
  if (pollingJobs.has(id)) return;
  const startedAt = Date.now();
  const timer = setInterval(async () => {
    try {
      await load();
      const job = state.list.find((j) => j.id === id);
      if (!job) {
        clearPollingJob(id);
        return;
      }
      if (job.status === 'SUCCESS') {
        ElMessage.success(`任务 ${id} 执行完成`);
        clearPollingJob(id);
        return;
      }
      if (job.status === 'FAILED') {
        ElMessage.error(job.errorMsg || `任务 ${id} 执行失败`);
        clearPollingJob(id);
        return;
      }
      if (Date.now() - startedAt > 20 * 60 * 1000) {
        ElMessage.warning(`任务 ${id} 轮询超时，请稍后手动刷新查看状态`);
        clearPollingJob(id);
      }
    } catch {
      // Ignore transient polling errors; next round will retry.
    }
  }, 3000);
  pollingJobs.set(id, timer);
}

function clearPollingJob(id) {
  const timer = pollingJobs.get(id);
  if (timer) {
    clearInterval(timer);
    pollingJobs.delete(id);
  }
}

async function deleteJob(id) {
  state.deleting = id;
  try {
    await request(`/api/admin/video-jobs/${id}`, { method: 'DELETE' });
    ElMessage.success('任务已删除');
    await load();
  } catch (e) {
    ElMessage.error(e.message || '删除失败');
  } finally {
    state.deleting = null;
  }
}

const enterJobs = computed(() => state.list.filter((j) => !j.direction || j.direction === 'ENTER'));
const exitJobs = computed(() => state.list.filter((j) => j.direction === 'EXIT'));

const pagedEnterJobs = computed(() => {
  const start = (pager.enterPage - 1) * pager.enterPageSize;
  return enterJobs.value.slice(start, start + pager.enterPageSize);
});

const pagedExitJobs = computed(() => {
  const start = (pager.exitPage - 1) * pager.exitPageSize;
  return exitJobs.value.slice(start, start + pager.exitPageSize);
});

function areaText(areaCode) {
  return AREA_OPTIONS[areaCode]?.label || areaCode || '未设置';
}

function areaTagType(areaCode) {
  return AREA_OPTIONS[areaCode]?.type || 'info';
}

function handleAreaChange(direction, value) {
  const key = direction === 'ENTER' ? 'video-job-enter-areaCode' : 'video-job-exit-areaCode';
  localStorage.setItem(key, value || '');
}

onMounted(load);

onBeforeUnmount(() => {
  pollingJobs.forEach((timer) => clearInterval(timer));
  pollingJobs.clear();
});
</script>

<template>
  <div class="page">
    <el-card shadow="never" class="module-card">
      <template #header>
        <div class="module-head">
          <div class="title-wrap">
            <span class="module-dot enter"></span>
            <span class="module-title enter">入园监控</span>
          </div>
          <span class="module-desc">检测到游客入园时，入园累计 +1，在园人数 +1</span>
        </div>
      </template>

      <div class="form-row">
        <el-input v-model="enterForm.videoPath" placeholder="请选择视频文件" style="flex: 1" readonly />
        <input ref="enterFileRef" type="file" accept="video/*" style="display: none" @change="onPickFile($event, 'ENTER')" />
        <el-button :loading="state.uploading === 'ENTER'" @click="triggerPick('ENTER')">选择视频</el-button>
        <el-select v-model="enterForm.areaCode" style="width: 150px" @change="(value) => handleAreaChange('ENTER', value)">
          <el-option v-for="item in AREA_CODE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-input-number v-model.number="enterForm.sampleMs" :min="200" :step="200" controls-position="right" placeholder="采样ms" style="width: 120px" />
        <el-button type="primary" @click="createJob('ENTER')">创建任务</el-button>
      </div>

      <el-table :data="pagedEnterJobs" border stripe class="job-table" size="small">
        <el-table-column prop="id" label="ID" width="60" align="center" />
        <el-table-column prop="videoPath" label="视频路径" min-width="200" show-overflow-tooltip />
        <el-table-column label="区域" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="areaTagType(row.areaCode)" effect="light" round>{{ areaText(row.areaCode) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sampleMs" label="采样(ms)" width="90" align="right" />
        <el-table-column prop="errorMsg" label="失败原因" min-width="220" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : row.status === 'FAILED' ? 'danger' : row.status === 'RUNNING' ? 'warning' : 'info'" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" align="center">
          <template #default="{ row }">
            <el-button type="primary" size="small" :loading="state.running === row.id" @click="runJob(row.id)">执行</el-button>
            <el-button type="danger" size="small" :loading="state.deleting === row.id" @click="deleteJob(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="pager.enterPage"
          v-model:page-size="pager.enterPageSize"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          :total="enterJobs.length"
        />
      </div>
    </el-card>

    <el-card shadow="never" class="module-card">
      <template #header>
        <div class="module-head">
          <div class="title-wrap">
            <span class="module-dot exit"></span>
            <span class="module-title exit">出园监控</span>
          </div>
          <span class="module-desc">检测到游客出园时，在园人数 -1，出园累计 +1</span>
        </div>
      </template>

      <div class="form-row">
        <el-input v-model="exitForm.videoPath" placeholder="请选择视频文件" style="flex: 1" readonly />
        <input ref="exitFileRef" type="file" accept="video/*" style="display: none" @change="onPickFile($event, 'EXIT')" />
        <el-button :loading="state.uploading === 'EXIT'" @click="triggerPick('EXIT')">选择视频</el-button>
        <el-select v-model="exitForm.areaCode" style="width: 150px" @change="(value) => handleAreaChange('EXIT', value)">
          <el-option v-for="item in AREA_CODE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-input-number v-model.number="exitForm.sampleMs" :min="200" :step="200" controls-position="right" placeholder="采样ms" style="width: 120px" />
        <el-button type="danger" @click="createJob('EXIT')">创建任务</el-button>
      </div>

      <el-table :data="pagedExitJobs" border stripe class="job-table" size="small">
        <el-table-column prop="id" label="ID" width="60" align="center" />
        <el-table-column prop="videoPath" label="视频路径" min-width="200" show-overflow-tooltip />
        <el-table-column label="区域" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="areaTagType(row.areaCode)" effect="light" round>{{ areaText(row.areaCode) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sampleMs" label="采样(ms)" width="90" align="right" />
        <el-table-column prop="errorMsg" label="失败原因" min-width="220" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : row.status === 'FAILED' ? 'danger' : row.status === 'RUNNING' ? 'warning' : 'info'" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" align="center">
          <template #default="{ row }">
            <el-button type="primary" size="small" :loading="state.running === row.id" @click="runJob(row.id)">执行</el-button>
            <el-button type="danger" size="small" :loading="state.deleting === row.id" @click="deleteJob(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="pager.exitPage"
          v-model:page-size="pager.exitPageSize"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          :total="exitJobs.length"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.page { display: flex; flex-direction: column; gap: 18px; }
.module-card {
  border-radius: 18px;
  overflow: hidden;
  box-shadow: 0 14px 36px rgba(15, 23, 42, 0.08);
}
.module-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.title-wrap { display: inline-flex; align-items: center; gap: 10px; }
.module-dot { width: 10px; height: 10px; border-radius: 999px; display: inline-block; }
.module-dot.enter { background: #409eff; box-shadow: 0 0 0 4px rgba(64, 158, 255, 0.14); }
.module-dot.exit { background: #f56c6c; box-shadow: 0 0 0 4px rgba(245, 108, 108, 0.14); }
.module-title { font-size: 18px; font-weight: 700; margin-right: 10px; }
.module-title.enter { color: #409eff; }
.module-title.exit { color: #f56c6c; }
.module-desc { font-size: 14px; color: #64748b; }
.form-row { display: flex; gap: 10px; align-items: center; margin-bottom: 14px; flex-wrap: wrap; }
.job-table { margin-top: 4px; }
.job-table :deep(th.el-table__cell) {
  background: #f8fafc;
  color: #334155;
  font-size: 15px;
  font-weight: 600;
}
.job-table :deep(td.el-table__cell) { padding-top: 14px; padding-bottom: 14px; }
.job-table :deep(.cell) { line-height: 1.5; }
.job-table :deep(.el-table__body tr:hover > td) { background: #f7faff; }
.job-table :deep(.el-tag) { border-radius: 999px; padding: 0 10px; height: 26px; line-height: 24px; font-size: 13px; }
.job-table :deep(.el-button--small) { font-size: 14px; }
.pager { display: flex; justify-content: center; margin-top: 12px; }
</style>


