<script setup>
import { computed, onMounted, reactive } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { download, request } from '../../api/http';

function formatTicketType(value) {
  const map = {
    SINGLE: '单人票',
    FAMILY: '家庭票',
    CHILD: '儿童票',
    STUDENT: '学生票',
    SENIOR: '老人票',
    单人票: '单人票',
    家庭票: '家庭票',
    儿童票: '儿童票',
    学生票: '学生票',
    老人票: '老人票'
  };
  return map[value] || (value || '未分类');
}

function normalizeTicketType(value) {
  const map = {
    单人票: 'SINGLE',
    家庭票: 'FAMILY',
    儿童票: 'CHILD',
    学生票: 'STUDENT',
    老人票: 'SENIOR'
  };
  return map[value] || value;
}

const defaultForm = () => ({
  scenicId: 1,
  projectIds: [],
  name: '',
  imageUrl: '',
  ticketType: 'SINGLE',
  priceCent: 100,
  stockQty: 200,
  morningStockQty: 100,
  afternoonStockQty: 100,
  morningEnabled: 1,
  afternoonEnabled: 1,
  validDate: '',
  refundRuleId: null
});

const state = reactive({
  list: [],
  page: 1,
  pageSize: 10,
  keyword: '',
  searchKeyword: '',
  inventoryDate: '',
  uploadUrl: '',
  modalTitle: '新增门票',
  modalMode: 'create',
  editingId: null,
  showModal: false,
  form: defaultForm()
});

const projectState = reactive({ list: [] });
const pagedList = computed(() => {
  const start = (state.page - 1) * state.pageSize;
  return state.list.slice(start, start + state.pageSize);
});

function formatDate(date = new Date()) {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

function attachInventoryRow(ticket, rows) {
  const morning = (rows || []).find((r) => Number(r.timeslotId) === 1);
  const afternoon = (rows || []).find((r) => Number(r.timeslotId) === 2);
  return {
    ...ticket,
    morningRemain: morning ? Number(morning.remainQty || 0) : 0,
    afternoonRemain: afternoon ? Number(afternoon.remainQty || 0) : 0
  };
}

async function refreshInventoryForList() {
  const date = state.inventoryDate || formatDate();
  const rows = await Promise.all(
    state.list.map(async (item) => {
      try {
        const inv = await request(`/api/tickets/${item.id}/inventory?date=${date}`);
        return attachInventoryRow(item, inv || []);
      } catch {
        return attachInventoryRow(item, []);
      }
    })
  );
  state.list = rows;
}

async function onInventoryDateChange() {
  await refreshInventoryForList();
}

function mustValidForm() {
  if (!state.form.name || !state.form.name.trim()) throw new Error('请填写门票名称');
  if (!state.form.ticketType || !state.form.ticketType.trim()) throw new Error('请填写票种');
  if (!state.form.priceCent || Number(state.form.priceCent) <= 0) throw new Error('价格必须大于 0');
  if (state.form.stockQty === '' || Number(state.form.stockQty) < 0) throw new Error('库存不能小于 0');
  if (state.form.morningStockQty === '' || Number(state.form.morningStockQty) < 0) throw new Error('上午库存不能小于 0');
  if (state.form.afternoonStockQty === '' || Number(state.form.afternoonStockQty) < 0) throw new Error('下午库存不能小于 0');
  if (!state.form.morningEnabled && !state.form.afternoonEnabled) throw new Error('至少开放一个场次');
  if (!Array.isArray(state.form.projectIds) || state.form.projectIds.length === 0) throw new Error('请至少选择一个景区项目');
  if (!state.form.validDate) {
    throw new Error('请选择有效日期');
  }
}

async function loadProjects() {
  try {
    const list = await request('/api/admin/scenic');
    projectState.list = (list || []).map((item) => ({
      id: item.id,
      name: item.name,
      status: item.status
    }));
  } catch (e) {
    ElMessage.error(e.message || '加载项目列表失败');
  }
}

async function load() {
  try {
    const params = new URLSearchParams({ scenicId: '1' });
    if (state.inventoryDate) params.set('date', state.inventoryDate);
    if (state.searchKeyword) params.set('keyword', state.searchKeyword);
    const list = await request(`/api/admin/tickets?${params.toString()}`);
    state.list = (list || []).map((item) => ({ ...item, morningRemain: 0, afternoonRemain: 0 }));
    state.page = 1;
    await refreshInventoryForList();
  } catch (e) {
    ElMessage.error(e.message || '加载门票失败');
  }
}

function openCreateModal() {
  state.modalTitle = '新增门票';
  state.modalMode = 'create';
  state.editingId = null;
  state.form = {
    ...defaultForm(),
    validDate: state.inventoryDate || formatDate()
  };
  state.uploadUrl = '';
  state.showModal = true;
}

function openEditModal(item) {
  const morningBaseQty = item.morningStockQty ?? item.morningRemain ?? 0;
  const afternoonBaseQty = item.afternoonStockQty ?? item.afternoonRemain ?? 0;
  state.modalTitle = '编辑门票';
  state.modalMode = 'edit';
  state.editingId = item.id;
  state.form = {
    scenicId: item.scenicId || 1,
    projectIds: Array.isArray(item.projectIds) ? [...item.projectIds] : [],
    name: item.name || '',
    imageUrl: item.imageUrl || '',
    ticketType: normalizeTicketType(item.ticketType || 'SINGLE'),
    priceCent: (item.priceCent || 10000) / 100,
    stockQty: item.stockQty ?? 0,
    morningStockQty: morningBaseQty,
    afternoonStockQty: afternoonBaseQty,
    morningEnabled: item.morningEnabled ?? 1,
    afternoonEnabled: item.afternoonEnabled ?? 1,
    validDate: item.validDate || state.inventoryDate || formatDate(),
    refundRuleId: item.refundRuleId || null
  };
  state.uploadUrl = item.imageUrl || '';
  state.showModal = true;
}

function closeModal() {
  state.showModal = false;
}

async function submitForm() {
  try {
    mustValidForm();
    const morningStockQty = Number(state.form.morningStockQty || 0);
    const afternoonStockQty = Number(state.form.afternoonStockQty || 0);
    const stockQty = morningStockQty + afternoonStockQty;
    const body = {
      ...state.form,
      name: state.form.name.trim(),
      ticketType: normalizeTicketType(state.form.ticketType.trim()),
      priceCent: Math.round(state.form.priceCent * 100),
      projectIds: Array.isArray(state.form.projectIds) ? state.form.projectIds : [],
      stockQty,
      morningStockQty,
      afternoonStockQty,
      morningEnabled: state.form.morningEnabled ? 1 : 0,
      afternoonEnabled: state.form.afternoonEnabled ? 1 : 0,
      refundRuleId: state.form.refundRuleId || null,
      validDate: state.form.validDate || null
    };

    if (state.modalMode === 'create') {
      await request('/api/admin/tickets', { method: 'POST', body });
      ElMessage.success('新增门票成功');
    } else {
      await request(`/api/admin/tickets/${state.editingId}`, { method: 'PUT', body });
      ElMessage.success('修改门票成功');
    }
    state.showModal = false;
    await load();
  } catch (err) {
    ElMessage.error(err.message || '保存失败');
  }
}

async function onSearch() {
  state.searchKeyword = state.keyword.trim();
  await load();
}

async function onResetSearch() {
  state.keyword = '';
  state.searchKeyword = '';
  await load();
}

async function toggleStatus(item) {
  try {
    await request(`/api/admin/tickets/${item.id}/status`, {
      method: 'PUT',
      body: { status: item.status === 1 ? 0 : 1 }
    });
    ElMessage.success(item.status === 1 ? '门票已下架' : '门票已上架');
    await load();
  } catch (err) {
    ElMessage.error(err.message || '状态更新失败');
  }
}

async function uploadImage(e) {
  try {
    // 仅取第一个文件进行上传（当前表单只维护单张门票图片）
    const file = e.target.files?.[0];
    if (!file) return;

    // 使用 FormData 走 multipart/form-data，后端按 file 字段接收
    const fd = new FormData();
    fd.append('file', file);

    // 上传到后端，再由后端转存 OSS；返回值中携带可访问图片 URL
    const data = await request('/api/admin/files/image/upload', { method: 'POST', body: fd });

    // 同步更新“上传链接展示”和“表单提交字段”，后续保存门票会落库 imageUrl
    state.uploadUrl = data.url;
    state.form.imageUrl = data.url;
  } catch (err) {
    // 统一前端提示，不暴露底层异常细节
    ElMessage.error(err.message || '图片上传失败');
  }
}

async function importExcel(e) {
  try {
    const file = e.target.files?.[0];
    if (!file) return;
    const fd = new FormData();
    fd.append('file', file);
    await request('/api/admin/tickets/import', { method: 'POST', body: fd });
    ElMessage.success('导入成功');
    await load();
    e.target.value = '';
  } catch (err) {
    ElMessage.error(err.message || '导入失败');
    e.target.value = '';
  }
}

async function deleteTicket(item) {
  try {
    await ElMessageBox.confirm(`确定要删除门票「${item.name}」吗？此操作不可恢复。`, '删除确认', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    });
    await request(`/api/admin/tickets/${item.id}`, { method: 'DELETE' });
    ElMessage.success('删除成功');
    await load();
  } catch (err) {
    if (err !== 'cancel') ElMessage.error(err.message || '删除失败');
  }
}

async function downloadTemplate() {
  try {
    const blob = await download('/api/admin/tickets/template');
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'ticket_template.xlsx';
    link.click();
    URL.revokeObjectURL(url);
    ElMessage.success('模板已下载');
  } catch (err) {
    ElMessage.error(err.message || '下载模板失败');
  }
}

async function exportTickets() {
  try {
    const blob = await download('/api/admin/tickets/export?scenicId=1');
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `tickets_${formatDate()}.xlsx`;
    link.click();
    URL.revokeObjectURL(url);
    ElMessage.success('门票信息导出成功');
  } catch (err) {
    ElMessage.error(err.message || '门票信息导出失败');
  }
}

onMounted(async () => {
  state.inventoryDate = formatDate();
  await loadProjects();
  await load();
});
</script>

<template>
  <el-card class="ticket-card" shadow="never">
    <template #header>
      <div class="toolbar">
        <div>
          <h3>门票管理</h3>
          <p>支持按名称搜索、新增、编辑、上下架和图片上传。</p>
        </div>
        <div class="btn-group">
          <el-button type="primary" @click="openCreateModal">新增门票</el-button>
          <el-dropdown>
            <el-button type="success">导入/导出</el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item>
                  <label for="import-file" class="dropdown-label">批量导入 Excel</label>
                  <input id="import-file" type="file" accept=".xlsx, .xls" @change="importExcel" style="display: none" />
                </el-dropdown-item>
                <el-dropdown-item @click="exportTickets">导出门票信息</el-dropdown-item>
                <el-dropdown-item @click="downloadTemplate">下载导入模板</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </template>

    <div class="search-row">
      <el-input v-model="state.keyword" placeholder="按门票名称搜索" clearable @keyup.enter="onSearch" />
      <el-date-picker
        v-model="state.inventoryDate"
        type="date"
        value-format="YYYY-MM-DD"
        placeholder="库存日期"
        @change="onInventoryDateChange"
      />
      <el-button type="primary" @click="onSearch">查询</el-button>
      <el-button @click="onResetSearch">重置</el-button>
    </div>

      <el-table :data="pagedList" border stripe class="ticket-table">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column label="图片" width="90">
        <template #default="scope">
          <img v-if="scope.row.imageUrl" :src="scope.row.imageUrl" alt="ticket" class="thumb" />
          <span v-else>无图</span>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="名称" width="150" />
      <el-table-column prop="projectNames" label="景区项目" width="230">
        <template #default="scope">
          <span>{{ scope.row.projectNames || '未绑定' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="票种" width="100">
        <template #default="scope">{{ formatTicketType(scope.row.ticketType) }}</template>
      </el-table-column>
      <el-table-column label="价格(元)" width="90">
        <template #default="scope">{{ (scope.row.priceCent / 100).toFixed(2) }}</template>
      </el-table-column>
      <el-table-column prop="stockQty" label="总库存" width="80" />
      <el-table-column label="上午/下午库存" width="165">
        <template #default="scope">{{ scope.row.morningRemain }} / {{ scope.row.afternoonRemain }}</template>
      </el-table-column>
      <el-table-column label="场次" width="100">
        <template #default="scope">
          <el-tag v-if="scope.row.morningEnabled === 1" type="success" size="small">上午场</el-tag>
          <el-tag v-if="scope.row.afternoonEnabled === 1" type="warning" size="small" style="margin-left:4px">下午场</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" size="small">{{ scope.row.status === 1 ? '上架' : '下架' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="scope">
          <div class="actions">
            <el-button size="small" @click="openEditModal(scope.row)">编辑</el-button>
            <el-button size="small" :type="scope.row.status === 1 ? 'warning' : 'success'" @click="toggleStatus(scope.row)">
              {{ scope.row.status === 1 ? '下架' : '上架' }}
            </el-button>
            <el-button size="small" type="danger" @click="deleteTicket(scope.row)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        v-model:current-page="state.page"
        v-model:page-size="state.pageSize"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        :total="state.list.length"
      />
    </div>

    <el-dialog v-model="state.showModal" :title="state.modalTitle" width="900px" destroy-on-close class="ticket-dialog">
      <p class="modal-sub">填写门票信息后保存，支持上传图片并实时预览。</p>
      <el-form label-position="top" class="form-grid">
        <el-form-item label="门票名称">
          <el-input v-model="state.form.name" placeholder="例如：成人日场票" />
        </el-form-item>
        <el-form-item label="票种">
          <el-input v-model="state.form.ticketType" placeholder="例如：单人票 / 家庭票 / 夜场票" />
        </el-form-item>
        <el-form-item label="景区项目" class="span2">
          <el-select v-model="state.form.projectIds" multiple collapse-tags collapse-tags-tooltip placeholder="请选择景区项目" class="full">
            <el-option
              v-for="item in projectState.list"
              :key="item.id"
              :label="item.name"
              :value="item.id"
              :disabled="item.status !== 1"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="价格(元)">
          <el-input-number v-model="state.form.priceCent" :min="0.01" :step="1" :precision="2" controls-position="right" class="full form-number" />
        </el-form-item>
        <el-form-item label="上午库存">
          <el-input-number v-model="state.form.morningStockQty" :min="0" controls-position="right" class="full" />
        </el-form-item>
        <el-form-item label="下午库存">
          <el-input-number v-model="state.form.afternoonStockQty" :min="0" controls-position="right" class="full" />
        </el-form-item>
        <el-form-item label="总库存(自动汇总)">
          <el-input :model-value="Number(state.form.morningStockQty || 0) + Number(state.form.afternoonStockQty || 0)" disabled />
        </el-form-item>
        <el-form-item label="可预订场次" class="span2">
          <el-checkbox v-model="state.form.morningEnabled" :true-label="1" :false-label="0">上午场</el-checkbox>
          <el-checkbox v-model="state.form.afternoonEnabled" :true-label="1" :false-label="0" style="margin-left: 16px;">下午场</el-checkbox>
        </el-form-item>
        <el-form-item label="退款规则ID(可选)">
          <el-input-number v-model="state.form.refundRuleId" :min="1" controls-position="right" class="full" />
        </el-form-item>
        <el-form-item label="有效日期">
          <el-date-picker v-model="state.form.validDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" class="full" />
        </el-form-item>
        <el-form-item label="图片URL" class="span2">
          <el-input v-model="state.form.imageUrl" placeholder="可手动填写或上传后自动回填" />
        </el-form-item>
      </el-form>

      <div class="uploader">
        <input type="file" accept="image/*" @change="uploadImage" />
        <a v-if="state.uploadUrl" :href="state.uploadUrl" target="_blank">图片URL</a>
      </div>
      <img v-if="state.form.imageUrl" :src="state.form.imageUrl" alt="preview" class="preview" />

      <template #footer>
        <el-button @click="closeModal">取消</el-button>
        <el-button type="primary" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<style scoped>
.ticket-card { border-radius: 14px; font-size: 16px; }
.toolbar { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
.toolbar > div:first-child { flex: 1; }
.toolbar h3 { margin: 0; font-size: 22px; font-weight: 700; color: #0f172a; }
.toolbar p { margin: 8px 0 0; color: #64748b; font-size: 15px; }
.btn-group { display: flex; gap: 8px; align-items: center; }
.dropdown-label { cursor: pointer; }
.search-row { display: flex; gap: 10px; margin-bottom: 12px; }
.search-row .el-input { width: 320px; }
.ticket-table { width: 100%; font-size: 15px; table-layout: fixed; }
.ticket-table :deep(th.el-table__cell) { font-size: 15px; font-weight: 600; color: #334155; }
.ticket-table :deep(td.el-table__cell) { padding-top: 14px; padding-bottom: 14px; }
.ticket-table :deep(.cell) { line-height: 1.5; }
.ticket-table :deep(.el-table__header-wrapper) { background: #f8fafc; }
.ticket-table :deep(.el-table__body tr:hover > td) { background: #f7faff; }
.ticket-table :deep(.el-button--small) { font-size: 14px; padding: 7px 12px; }
.pager { display: flex; justify-content: center; margin-top: 12px; }
.thumb { width: 72px; height: 48px; object-fit: cover; border-radius: 8px; }
.actions { display: flex; gap: 6px; flex-wrap: nowrap; white-space: nowrap; }
.modal-sub { margin: 6px 0 0; font-size: 15px; color: #64748b; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; column-gap: 12px; }
.span2 { grid-column: span 2; }
.full { width: 100%; }
.form-number { min-width: 100%; }
.uploader { margin: 8px 0; display: flex; align-items: center; gap: 10px; }
.preview { display: block; margin-top: 6px; width: 180px; height: 112px; object-fit: cover; border-radius: 10px; border: 1px solid #e5e7eb; }

.ticket-dialog :deep(.el-dialog) {
  border-radius: 18px;
  overflow: hidden;
}
.ticket-dialog :deep(.el-dialog__header) {
  margin: 0;
  padding: 22px 24px 8px;
  background: linear-gradient(135deg, #f8fbff 0%, #ffffff 100%);
  border-bottom: 1px solid #eef2f7;
}
.ticket-dialog :deep(.el-dialog__title) {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
}
.ticket-dialog :deep(.el-dialog__body) {
  padding: 18px 24px 10px;
  background: #fff;
}
.ticket-dialog :deep(.el-dialog__footer) {
  padding: 14px 24px 22px;
  border-top: 1px solid #eef2f7;
  background: #fff;
}
.ticket-dialog :deep(.el-form-item__label) {
  color: #334155;
  font-weight: 600;
}
.ticket-dialog :deep(.el-input__wrapper),
.ticket-dialog :deep(.el-select__wrapper),
.ticket-dialog :deep(.el-input-number__wrapper),
.ticket-dialog :deep(.el-date-editor.el-input__wrapper) {
  border-radius: 12px;
}
.ticket-dialog :deep(.el-button) {
  border-radius: 10px;
  padding-left: 18px;
  padding-right: 18px;
}

@media (max-width: 768px) {
  .toolbar { gap: 10px; flex-direction: column; align-items: stretch; }
  .btn-group { flex-wrap: wrap; }
  .search-row { flex-wrap: wrap; }
  .search-row .el-input { width: 100%; }
  .form-grid { grid-template-columns: 1fr; }
  .span2 { grid-column: span 1; }
}
</style>


