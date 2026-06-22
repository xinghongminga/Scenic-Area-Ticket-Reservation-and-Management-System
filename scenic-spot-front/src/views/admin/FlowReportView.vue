<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import VChart from 'vue-echarts';
import { use } from 'echarts/core';
import { CanvasRenderer } from 'echarts/renderers';
import { LineChart } from 'echarts/charts';
import { GridComponent, TooltipComponent, TitleComponent, LegendComponent } from 'echarts/components';
import { download, request } from '../../api/http';
import { ElMessage } from 'element-plus';

use([CanvasRenderer, LineChart, GridComponent, TooltipComponent, TitleComponent, LegendComponent]);

const now = new Date();
const monthStart = new Date(now.getFullYear(), now.getMonth(), 1, 0, 0, 0);
const form = reactive({ scenicId: 1, range: [monthStart, now] });
const state = reactive({ loading: false, data: null, chart: {}, page: 1, pageSize: 10 });
const activeTab = ref('chart');

const pagedPoints = computed(() => {
  const points = state.data?.points || [];
  const start = (state.page - 1) * state.pageSize;
  return points.slice(start, start + state.pageSize);
});

function pad(num) {
  return String(num).padStart(2, '0');
}

function formatDateTime(value) {
  const date = value instanceof Date ? value : new Date(value);
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

function disabledFutureDate(date) {
  return date.getTime() > Date.now();
}

function getQueryRange() {
  if (!form.range || form.range.length !== 2 || !form.range[0] || !form.range[1]) {
    throw new Error('请选择开始和结束时间');
  }
  const startDate = new Date(form.range[0]);
  const endDate = new Date(form.range[1]);
  if (startDate > endDate) {
    throw new Error('开始时间不能晚于结束时间');
  }
  if (endDate > Date.now()) {
    throw new Error('结束时间不能超过当前时间');
  }
  return {
    start: formatDateTime(startDate),
    end: formatDateTime(endDate)
  };
}

async function load() {
  state.loading = true;
  try {
    const range = getQueryRange();
    state.data = await request(`/api/analyst/report/flow?scenicId=${form.scenicId}&start=${encodeURIComponent(range.start)}&end=${encodeURIComponent(range.end)}`);
    state.page = 1;
    if (state.data?.points?.length) {
      const times = state.data.points.map(d => d.statMinute);
      const inCounts = state.data.points.map(d => d.inCount || 0);
      const outCounts = state.data.points.map(d => d.outCount || 0);
      state.chart = {
        title: { text: '客流趋势', left: 'center', textStyle: { fontSize: 14 } },
        tooltip: { trigger: 'axis' },
        legend: { data: ['入园人数', '出园人数'], bottom: 0 },
        grid: { left: '3%', right: '4%', bottom: '10%', top: '15%', containLabel: true },
        xAxis: { type: 'category', data: times, axisLabel: { rotate: 30, fontSize: 11 } },
        yAxis: { type: 'value' },
        series: [
          { name: '入园人数', type: 'line', data: inCounts, smooth: true, areaStyle: {}, itemStyle: { color: '#409eff' } },
          { name: '出园人数', type: 'line', data: outCounts, smooth: true, itemStyle: { color: '#f56c6c' } }
        ]
      };
    } else {
      state.chart = {};
    }
  } catch (e) {
    ElMessage.error(e.message || '加载客流报表失败');
  } finally {
    state.loading = false;
  }
}

async function exportExcel() {
  try {
    const range = getQueryRange();
    const blob = await download(`/api/analyst/report/flow/export?scenicId=${form.scenicId}&start=${encodeURIComponent(range.start)}&end=${encodeURIComponent(range.end)}`);
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = 'flow_report.xlsx';
    a.click();
    URL.revokeObjectURL(a.href);
    ElMessage.success('导出成功');
  } catch (e) {
    ElMessage.error(e.message || '导出失败');
  }
}

onMounted(load);
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <div class="head">
        <div>
          <h3>客流报表</h3>
          <p>按景区和时间范围查看客流趋势数据。</p>
        </div>
      </div>
    </template>

    <div class="toolbar">
      <el-date-picker
        v-model="form.range"
        type="datetimerange"
        range-separator="至"
        start-placeholder="开始时间"
        end-placeholder="结束时间"
        format="YYYY-MM-DD HH:mm:ss"
        :disabled-date="disabledFutureDate"
      />
      <el-button type="primary" :loading="state.loading" @click="load">查询</el-button>
      <el-button type="success" plain @click="exportExcel">导出Excel</el-button>
    </div>

    <div v-if="state.data">
      <el-tabs v-model="activeTab" class="report-tabs">
        <el-tab-pane label="图表" name="chart">
          <div v-if="state.chart?.series" class="chart-container">
            <VChart class="chart" :option="state.chart" autoresize />
          </div>
          <el-empty v-else description="当前时段暂无客流数据" />
        </el-tab-pane>

        <el-tab-pane label="表格" name="table">
          <el-table
            v-if="state.data?.points?.length"
            :data="pagedPoints"
            border stripe class="data-table"
          >
            <el-table-column prop="statMinute" label="时间记录" min-width="180">
              <template #default="{ row }">
                <span class="time-text">{{ row.statMinute }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="inCount" label="入园人数" width="140" align="right">
              <template #default="{ row }">
                <span class="in-text">{{ row.inCount }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="outCount" label="出园人数" width="140" align="right">
              <template #default="{ row }">
                <span class="out-text">{{ row.outCount }}</span>
              </template>
            </el-table-column>
          </el-table>

          <div v-if="state.data?.points?.length" class="pager">
            <el-pagination
              v-model:current-page="state.page"
              v-model:page-size="state.pageSize"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next"
              :total="state.data.points.length"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </el-card>
</template>

<style scoped>
.el-card { border-radius: 16px; }
.head { display: flex; align-items: flex-start; justify-content: space-between; }
.head h3 { margin: 0; font-size: 22px; font-weight: 700; color: #0f172a; }
.head p { margin: 8px 0 0; color: #64748b; font-size: 15px; }

.toolbar { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 12px; align-items: center; }

.chart-container { height: 400px; margin-bottom: 12px; }
.chart { width: 100%; height: 100%; }

.data-table {
  border-radius: 14px;
  overflow: hidden;
  font-size: 15px;
  margin-top: 8px;
}
.data-table :deep(th.el-table__cell) {
  background: #f8fafc;
  color: #334155;
  font-size: 15px;
  font-weight: 600;
}
.data-table :deep(td.el-table__cell) {
  padding-top: 14px;
  padding-bottom: 14px;
}
.data-table :deep(.cell) { line-height: 1.5; }
.data-table :deep(.el-table__body tr:hover > td) { background: #f7faff; }

.time-text { color: #64748b; font-size: 15px; }
.in-text { color: #10b981; font-weight: 600; font-size: 15px; }
.out-text { color: #ef4444; font-weight: 600; font-size: 15px; }

.pager { display: flex; justify-content: center; margin-top: 20px; }

@media (max-width: 768px) {
  .toolbar { flex-direction: column; align-items: flex-start; }
  .chart-container { height: 300px; }
}
</style>
