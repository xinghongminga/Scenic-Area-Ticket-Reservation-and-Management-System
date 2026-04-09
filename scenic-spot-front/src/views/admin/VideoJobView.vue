<script setup>
import { onMounted, reactive, computed, ref } from 'vue';
import { request } from '../../api/http';
import { ElMessage } from 'element-plus';

const enterForm = reactive({ scenicId: 1, videoPath: '', areaCode: 'GATE_IN', sampleMs: 1000 });
const exitForm = reactive({ scenicId: 1, videoPath: '', areaCode: 'GATE_OUT', sampleMs: 1000 });
const state = reactive({ list: [], running: null, deleting: null, uploading: '' });
const pager = reactive({ enterPage: 1, enterPageSize: 10, exitPage: 1, exitPageSize: 10 });
const enterFileRef = ref(null);
const exitFileRef = ref(null);

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
    const res = await request(`/api/admin/video-jobs/${id}/run`, { method: 'POST' });
    ElMessage.success(`执行成功，写入 ${res.pointsWritten} 条样本，${res.minutesAggregated} 分钟`);
  } catch (e) {
    ElMessage.error(e.message || '执行失败');
  } finally {
    state.running = null;
    await load();
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

onMounted(load);
</script>

<template>
  <div class="page">
    <el-card shadow="never" class="module-card">
      <template #header>
        <span class="module-title enter">入园人头计数</span>
        <span class="module-desc">检测到入园人头时，入园累计 +1，在园人数 +1</span>
      </template>

      <div class="form-row">
        <el-input-number v-model.number="enterForm.scenicId" :min="1" controls-position="right" placeholder="景区ID" style="width: 100px" />
        <el-input v-model="enterForm.videoPath" placeholder="请选择视频文件" style="flex: 1" readonly />
        <input ref="enterFileRef" type="file" accept="video/*" style="display: none" @change="onPickFile($event, 'ENTER')" />
        <el-button :loading="state.uploading === 'ENTER'" @click="triggerPick('ENTER')">选择视频</el-button>
        <el-input v-model="enterForm.areaCode" placeholder="区域编码" style="width: 120px" />
        <el-input-number v-model.number="enterForm.sampleMs" :min="200" :step="200" controls-position="right" placeholder="采样ms" style="width: 120px" />
        <el-button type="primary" @click="createJob('ENTER')">创建任务</el-button>
      </div>

      <el-table :data="pagedEnterJobs" border stripe class="job-table" size="small">
        <el-table-column prop="id" label="ID" width="60" align="center" />
        <el-table-column prop="videoPath" label="视频路径" min-width="200" show-overflow-tooltip />
        <el-table-column prop="areaCode" label="区域" width="100" align="center" />
        <el-table-column prop="sampleMs" label="采样(ms)" width="90" align="right" />
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
        <span class="module-title exit">出园人头计数</span>
        <span class="module-desc">检测到出园人头时，在园人数 -1，出园累计 +1</span>
      </template>

      <div class="form-row">
        <el-input-number v-model.number="exitForm.scenicId" :min="1" controls-position="right" placeholder="景区ID" style="width: 100px" />
        <el-input v-model="exitForm.videoPath" placeholder="请选择视频文件" style="flex: 1" readonly />
        <input ref="exitFileRef" type="file" accept="video/*" style="display: none" @change="onPickFile($event, 'EXIT')" />
        <el-button :loading="state.uploading === 'EXIT'" @click="triggerPick('EXIT')">选择视频</el-button>
        <el-input v-model="exitForm.areaCode" placeholder="区域编码" style="width: 120px" />
        <el-input-number v-model.number="exitForm.sampleMs" :min="200" :step="200" controls-position="right" placeholder="采样ms" style="width: 120px" />
        <el-button type="danger" @click="createJob('EXIT')">创建任务</el-button>
      </div>

      <el-table :data="pagedExitJobs" border stripe class="job-table" size="small">
        <el-table-column prop="id" label="ID" width="60" align="center" />
        <el-table-column prop="videoPath" label="视频路径" min-width="200" show-overflow-tooltip />
        <el-table-column prop="areaCode" label="区域" width="100" align="center" />
        <el-table-column prop="sampleMs" label="采样(ms)" width="90" align="right" />
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
.page { display: flex; flex-direction: column; gap: 16px; }
.module-card { border-radius: 14px; }
.module-title { font-size: 15px; font-weight: 600; margin-right: 10px; }
.module-title.enter { color: #409eff; }
.module-title.exit { color: #f56c6c; }
.module-desc { font-size: 12px; color: #64748b; }
.form-row { display: flex; gap: 8px; align-items: center; margin-bottom: 12px; flex-wrap: wrap; }
.job-table { margin-top: 4px; }
.pager { display: flex; justify-content: center; margin-top: 12px; }
</style>


