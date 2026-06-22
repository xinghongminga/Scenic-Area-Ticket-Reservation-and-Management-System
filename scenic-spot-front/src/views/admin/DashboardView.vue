<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import VChart from 'vue-echarts';
import { use } from 'echarts/core';
import { CanvasRenderer } from 'echarts/renderers';
import { LineChart, BarChart } from 'echarts/charts';
import { GridComponent, TooltipComponent, TitleComponent, LegendComponent } from 'echarts/components';
import { request } from '../../api/http';

use([CanvasRenderer, LineChart, BarChart, GridComponent, TooltipComponent, TitleComponent, LegendComponent]);

const activeTab = ref('daily');
const viewMode = ref('chart');
const form = reactive({ scenicId: 1, minutes: 120 });
const state = reactive({ data: null, loading: false, dailyChart: {}, monthlyChart: {} });
const pager = reactive({
  dailyPage: 1,
  dailyPageSize: 10,
  monthlyPage: 1,
  monthlyPageSize: 10
});

const pagedDailyTrend = computed(() => {
  const list = state.data?.trend || [];
  const start = (pager.dailyPage - 1) * pager.dailyPageSize;
  return list.slice(start, start + pager.dailyPageSize);
});

const pagedMonthlyTrend = computed(() => {
  const list = state.data?.monthlyTrend || [];
  const start = (pager.monthlyPage - 1) * pager.monthlyPageSize;
  return list.slice(start, start + pager.monthlyPageSize);
});

async function load() {
  state.loading = true;
  try {
    state.data = await request(`/api/admin/flow/dashboard?scenicId=${form.scenicId}&minutes=${form.minutes}`);
    pager.dailyPage = 1;
    pager.monthlyPage = 1;
    buildCharts();
  } catch (e) {
    ElMessage.error(e.message || '加载看板失败');
  } finally {
    state.loading = false;
  }
}

function buildCharts() {
  if (state.data?.trend?.length) {
    const times = state.data.trend.map((p) => p.statMinute);
    state.dailyChart = {
      title: { text: '日客流趋势（分钟级）', left: 'center', textStyle: { fontSize: 14 } },
      tooltip: { trigger: 'axis' },
      legend: { data: ['入园', '出园', '在园'], bottom: 0 },
      grid: { left: '3%', right: '4%', bottom: '10%', top: '15%', containLabel: true },
      xAxis: { type: 'category', data: times, axisLabel: { rotate: 30, fontSize: 11 } },
      yAxis: { type: 'value' },
      series: [
        { name: '入园', type: 'line', data: state.data.trend.map((p) => p.inCount || 0), smooth: true, itemStyle: { color: '#409eff' } },
        { name: '出园', type: 'line', data: state.data.trend.map((p) => p.outCount || 0), smooth: true, itemStyle: { color: '#f56c6c' } },
        { name: '在园', type: 'line', data: state.data.trend.map((p) => p.inParkCount || 0), smooth: true, itemStyle: { color: '#67c23a' } }
      ]
    };
  }

  if (state.data?.monthlyTrend?.length) {
    const days = state.data.monthlyTrend.map((d) => d.statDate);
    state.monthlyChart = {
      title: { text: '月客流趋势（日级）', left: 'center', textStyle: { fontSize: 14 } },
      tooltip: { trigger: 'axis' },
      legend: { data: ['入园', '出园'], bottom: 0 },
      grid: { left: '3%', right: '4%', bottom: '10%', top: '15%', containLabel: true },
      xAxis: { type: 'category', data: days, axisLabel: { rotate: 30, fontSize: 11 } },
      yAxis: { type: 'value' },
      series: [
        { name: '入园', type: 'bar', data: state.data.monthlyTrend.map((d) => d.inCount || 0), itemStyle: { color: '#409eff' } },
        { name: '出园', type: 'bar', data: state.data.monthlyTrend.map((d) => d.outCount || 0), itemStyle: { color: '#f56c6c' } }
      ]
    };
  }
}

onMounted(load);
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <div class="head">
        <div>
          <h3>客流实时看板</h3>
          <p>查看在园人数、累计入园及客流趋势，由视频人头计数驱动。</p>
        </div>
      </div>
    </template>

    <div class="toolbar">
      <el-input-number v-model="form.minutes" :min="10" :step="10" controls-position="right" placeholder="分钟范围" style="width: 130px" />
      <el-button type="primary" :loading="state.loading" @click="load">刷新</el-button>
      <el-radio-group v-model="viewMode" class="view-switch">
        <el-radio-button label="chart">图表模式</el-radio-button>
        <el-radio-button label="table">数据表格</el-radio-button>
      </el-radio-group>
    </div>

    <div v-if="state.data" class="stats">
      <el-statistic title="当前在园" :value="state.data.currentInPark || 0" />
      <el-statistic title="今日入园累计" :value="state.data.todayInTotal || 0" />
      <el-statistic title="本月入园累计" :value="state.data.thisMonthInTotal || 0" />
    </div>

    <el-alert
      v-if="state.data?.warnings?.length"
      type="warning"
      :closable="false"
      :title="`预警: ${state.data.warnings.map((w) => w.message).join('；')}`"
      class="warn"
    />

    <el-tabs v-model="activeTab" class="flow-tabs">
      <el-tab-pane label="日客流量" name="daily">
        <template v-if="viewMode === 'chart'">
          <div v-if="state.data?.trend?.length" class="chart-container">
            <VChart class="chart" :option="state.dailyChart" autoresize />
          </div>
          <el-empty v-else description="暂无日客流图表数据" />
        </template>

        <template v-else-if="viewMode === 'table'">
          <el-table v-if="state.data?.trend?.length" :data="pagedDailyTrend" border stripe class="table data-table">
            <el-table-column prop="statMinute" label="分钟" min-width="160" />
            <el-table-column prop="inCount" label="入园人数" width="120" align="right">
              <template #default="{ row }">
                <span class="in-text">{{ row.inCount }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="outCount" label="出园人数" width="120" align="right">
               <template #default="{ row }">
                <span class="out-text">{{ row.outCount }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="inParkCount" label="当前在园" width="120" align="right">
              <template #default="{ row }">
                <span class="park-text">{{ row.inParkCount }}</span>
              </template>
            </el-table-column>
          </el-table>

          <div v-if="state.data?.trend?.length" class="pager">
            <el-pagination
              v-model:current-page="pager.dailyPage"
              v-model:page-size="pager.dailyPageSize"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next"
              :total="state.data.trend.length"
            />
          </div>
          <el-empty v-if="!state.data?.trend?.length" description="暂无日客流表格数据" />
        </template>
      </el-tab-pane>

      <el-tab-pane label="月客流量" name="monthly">
        <template v-if="viewMode === 'chart'">
          <div v-if="state.data?.monthlyTrend?.length" class="chart-container">
            <VChart class="chart" :option="state.monthlyChart" autoresize />
          </div>
          <el-empty v-else description="暂无月客流图表数据" />
        </template>

        <template v-else-if="viewMode === 'table'">
          <el-table v-if="state.data?.monthlyTrend?.length" :data="pagedMonthlyTrend" border stripe class="table data-table">
            <el-table-column prop="statDate" label="日期" min-width="120" />
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

          <div v-if="state.data?.monthlyTrend?.length" class="pager">
            <el-pagination
              v-model:current-page="pager.monthlyPage"
              v-model:page-size="pager.monthlyPageSize"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next"
              :total="state.data.monthlyTrend.length"
            />
          </div>
          <el-empty v-if="!state.data?.monthlyTrend?.length" description="暂无月客流表格数据" />
        </template>
      </el-tab-pane>
    </el-tabs>
  </el-card>
</template>

<style scoped>
.el-card {
  border-radius: 16px;
}
.head { display: flex; align-items: flex-start; justify-content: space-between; }
.head h3 { margin: 0; font-size: 22px; font-weight: 700; color: #0f172a; }
.head p { margin: 8px 0 0; color: #64748b; font-size: 15px; }
.toolbar { display: flex; gap: 10px; margin: 4px 0 14px; align-items: center; }
.stats { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 12px; margin-bottom: 12px; }
.warn { margin-bottom: 12px; }
.flow-tabs { margin-top: 8px; }
.chart-container { height: 360px; margin: 12px 0; }
.chart { width: 100%; height: 100%; }
.table { margin-top: 8px; }
.data-table {
  border-radius: 14px;
  overflow: hidden;
  font-size: 15px;
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
.data-table :deep(.cell) {
  line-height: 1.5;
}
.data-table :deep(.el-table__body tr:hover > td) {
  background: #f7faff;
}
.in-text { color: #10b981; font-weight: 600; font-size: 15px; }
.out-text { color: #ef4444; font-weight: 600; font-size: 15px; }
.park-text { color: #f59e0b; font-weight: 600; font-size: 15px; }
.pager { display: flex; justify-content: center; margin-top: 12px; }
.view-switch { margin-left: auto; }

@media (max-width: 768px) {
  .toolbar { flex-wrap: wrap; }
  .stats { grid-template-columns: 1fr; }
  .chart-container { height: 280px; }
}
</style>

