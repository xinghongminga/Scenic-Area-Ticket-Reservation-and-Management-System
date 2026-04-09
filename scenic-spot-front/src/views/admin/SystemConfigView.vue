<script setup>
import { computed, reactive, ref } from 'vue';
import { request } from '../../api/http';
import { ElMessage, ElMessageBox } from 'element-plus';

const activeTab = ref('timeslot');
const timeslotDialog = ref(false);
const refundDialog = ref(false);
const isEditTimeslot = ref(false);
const isEditRefund = ref(false);

const state = reactive({
  timeslots: [],
  refunds: [],
  scenicId: 1,
  timeslotForm: { id: 0, scenicId: 1, startTime: '08:00', endTime: '18:00', status: 1 },
  refundForm: { id: 0, scenicId: 1, freeRefundHours: 24, allowReschedule: 1 }
});

const pager = reactive({
  timeslotPage: 1,
  timeslotPageSize: 10,
  refundPage: 1,
  refundPageSize: 10
});

const pagedTimeslots = computed(() => {
  const start = (pager.timeslotPage - 1) * pager.timeslotPageSize;
  return state.timeslots.slice(start, start + pager.timeslotPageSize);
});

const pagedRefunds = computed(() => {
  const start = (pager.refundPage - 1) * pager.refundPageSize;
  return state.refunds.slice(start, start + pager.refundPageSize);
});

async function loadTimeslots() {
  try {
    state.timeslots = await request(`/api/timeslots?scenicId=${state.scenicId}`);
    pager.timeslotPage = 1;
  } catch (e) {
    ElMessage.error('加载失败');
  }
}

async function loadRefunds() {
  try {
    state.refunds = await request(`/api/refund-rules?scenicId=${state.scenicId}`);
    pager.refundPage = 1;
  } catch (e) {
    ElMessage.error('加载失败');
  }
}

function openCreateTimeslot() {
  isEditTimeslot.value = false;
  state.timeslotForm = { id: 0, scenicId: state.scenicId, startTime: '08:00', endTime: '18:00', status: 1 };
  timeslotDialog.value = true;
}

function openEditTimeslot(row) {
  isEditTimeslot.value = true;
  state.timeslotForm = { ...row, scenicId: state.scenicId };
  timeslotDialog.value = true;
}

async function saveTimeslot() {
  if (!state.timeslotForm.startTime || !state.timeslotForm.endTime) {
    ElMessage.warning('开始和结束时间必填');
    return;
  }
  try {
    const body = {
      scenicId: state.scenicId,
      name: `${state.timeslotForm.startTime}-${state.timeslotForm.endTime}`,
      startTime: `${state.timeslotForm.startTime}:00`,
      endTime: `${state.timeslotForm.endTime}:00`
    };
    if (isEditTimeslot.value) {
      await request(`/api/admin/timeslots/${state.timeslotForm.id}`, { method: 'PUT', body });
      ElMessage.success('更新成功');
    } else {
      await request('/api/admin/timeslots', { method: 'POST', body });
      ElMessage.success('创建成功');
    }
    timeslotDialog.value = false;
    await loadTimeslots();
  } catch (e) {
    ElMessage.error('保存失败');
  }
}

async function deleteTimeslot(id) {
  ElMessageBox.confirm('确认删除?', '警告', { type: 'warning' })
    .then(async () => {
      try {
        await request(`/api/admin/timeslots/${id}`, { method: 'DELETE' });
        ElMessage.success('删除成功');
        await loadTimeslots();
      } catch (e) {
        ElMessage.error('删除失败');
      }
    })
    .catch(() => {});
}

function openCreateRefund() {
  isEditRefund.value = false;
  state.refundForm = { id: 0, scenicId: state.scenicId, freeRefundHours: 24, allowReschedule: 1 };
  refundDialog.value = true;
}

function openEditRefund(row) {
  isEditRefund.value = true;
  state.refundForm = { ...row, scenicId: state.scenicId };
  refundDialog.value = true;
}

async function saveRefund() {
  try {
    const body = {
      scenicId: state.scenicId,
      name: '默认规则',
      freeRefundHours: state.refundForm.freeRefundHours,
      allowReschedule: state.refundForm.allowReschedule
    };
    if (isEditRefund.value) {
      await request(`/api/admin/refund-rules/${state.refundForm.id}`, { method: 'PUT', body });
      ElMessage.success('更新成功');
    } else {
      await request('/api/admin/refund-rules', { method: 'POST', body });
      ElMessage.success('创建成功');
    }
    refundDialog.value = false;
    await loadRefunds();
  } catch (e) {
    ElMessage.error('保存失败');
  }
}

async function deleteRefund(id) {
  ElMessageBox.confirm('确认删除?', '警告', { type: 'warning' })
    .then(async () => {
      try {
        await request(`/api/admin/scenic/refund/delete/${id}`, { method: 'DELETE' });
        ElMessage.success('删除成功');
        await loadRefunds();
      } catch (e) {
        ElMessage.error('删除失败');
      }
    })
    .catch(() => {});
}

function onScenicChange() {
  if (activeTab.value === 'timeslot') {
    loadTimeslots();
  } else {
    loadRefunds();
  }
}

loadTimeslots();
</script>

<template>
  <div class="card">
    <div class="header">
      <h3>系统配置</h3>
      <div>
        <span>选择景区:</span>
        <el-input-number v-model.number="state.scenicId" @change="onScenicChange" :min="1" style="width: 100px" />
      </div>
    </div>

    <el-tabs v-model="activeTab" @tab-change="onScenicChange">
      <el-tab-pane label="时间段配置" name="timeslot">
        <div style="margin-bottom: 12px">
          <el-button @click="openCreateTimeslot" type="primary">+ 添加时间段</el-button>
        </div>
        <el-table :data="pagedTimeslots" stripe border>
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="startTime" label="开始时间" width="100" />
          <el-table-column prop="endTime" label="结束时间" width="100" />
          <el-table-column prop="status" label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <el-button @click="openEditTimeslot(row)" link type="primary" size="small">编辑</el-button>
              <el-button @click="deleteTimeslot(row.id)" link type="danger" size="small">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pager">
          <el-pagination
            v-model:current-page="pager.timeslotPage"
            v-model:page-size="pager.timeslotPageSize"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            :total="state.timeslots.length"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="退款规则" name="refund">
        <div style="margin-bottom: 12px">
          <el-button @click="openCreateRefund" type="primary">+ 添加规则</el-button>
        </div>
        <el-table :data="pagedRefunds" stripe border>
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="freeRefundHours" label="免费退款时限(小时)" width="160" />
          <el-table-column prop="allowReschedule" label="允许改期" width="100">
            <template #default="{ row }">
              <el-tag :type="row.allowReschedule === 1 ? 'success' : 'danger'">{{ row.allowReschedule === 1 ? '是' : '否' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <el-button @click="openEditRefund(row)" link type="primary" size="small">编辑</el-button>
              <el-button @click="deleteRefund(row.id)" link type="danger" size="small">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pager">
          <el-pagination
            v-model:current-page="pager.refundPage"
            v-model:page-size="pager.refundPageSize"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            :total="state.refunds.length"
          />
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="timeslotDialog" :title="isEditTimeslot ? '编辑时间段' : '添加时间段'" width="500px">
      <el-form label-width="100px">
        <el-form-item label="开始时间">
          <el-input v-model="state.timeslotForm.startTime" placeholder="HH:mm" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-input v-model="state.timeslotForm.endTime" placeholder="HH:mm" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="timeslotDialog = false">取消</el-button>
        <el-button @click="saveTimeslot" type="primary">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="refundDialog" :title="isEditRefund ? '编辑规则' : '添加规则'" width="500px">
      <el-form label-width="120px">
        <el-form-item label="免费退款时限(h)">
          <el-input-number v-model.number="state.refundForm.freeRefundHours" :min="0" />
        </el-form-item>
        <el-form-item label="允许改期">
          <el-select v-model.number="state.refundForm.allowReschedule">
            <el-option label="是" :value="1" />
            <el-option label="否" :value="0" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="refundDialog = false">取消</el-button>
        <el-button @click="saveRefund" type="primary">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.card {
  background: #fff;
  border-radius: 14px;
  padding: 16px;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
h3 {
  margin: 0;
  font-size: 18px;
}
.pager { display: flex; justify-content: center; margin-top: 12px; }
</style>

