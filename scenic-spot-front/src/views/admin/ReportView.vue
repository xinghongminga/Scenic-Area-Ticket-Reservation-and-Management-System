<script setup>
import { reactive } from 'vue';
import VChart from 'vue-echarts';
import { use } from 'echarts/core';
import { CanvasRenderer } from 'echarts/renderers';
import { BarChart, LineChart, PieChart } from 'echarts/charts';
import { GridComponent, TooltipComponent, TitleComponent, LegendComponent } from 'echarts/components';
import { download, request } from '../../api/http';
import { ElMessage } from 'element-plus';

use([CanvasRenderer, BarChart, LineChart, PieChart, GridComponent, TooltipComponent, TitleComponent, LegendComponent]);

const form = reactive({ scenicId: 1, start: '2026-03-01 00:00:00', end: '2026-03-31 23:59:59' });
const state = reactive({
  sales: null,
  flow: null,
  heatmap: null,
  salesChart: {},
  flowChart: {},
  heatmapChart: {},
  showSales: false,
  showFlow: false,
  showHeatmap: false
});

async function loadSales() {
  try {
    state.sales = await request(`/api/analyst/report/sales?scenicId=${form.scenicId}&start=${encodeURIComponent(form.start)}&end=${encodeURIComponent(form.end)}`);
    state.showSales = true;
    if (state.sales?.byTicket?.length) {
      const labels = state.sales.byTicket.map(d => d.ticketName || '未命名门票');
      const values = state.sales.byTicket.map(d => d.qty || 0);
      state.salesChart = {
        color: ['#2563eb', '#f59e0b', '#10b981', '#ef4444', '#8b5cf6', '#14b8a6'],
        title: { text: '销量统计', left: 'center', top: 10 },
        legend: {
          top: 40,
          left: 'center',
          data: labels,
          itemWidth: 12,
          itemHeight: 12
        },
        tooltip: { trigger: 'item', formatter: '{b}: {c}张 ({d}%)' },
        series: [{
          name: '销量(张)',
          type: 'pie',
          radius: ['35%', '60%'],
          center: ['50%', '62%'],
          avoidLabelOverlap: true,
          label: { formatter: '{b}\n{d}%' },
          data: labels.map((l, i) => ({ name: l, value: values[i] }))
        }]
      };
    } else {
      state.salesChart = { title: { text: '销量统计（暂无数据）' }, series: [] };
    }
    ElMessage.success('销量报表加载成功');
  } catch (e) {
    ElMessage.error('加载销量报表失败');
  }
}

async function loadFlow() {
  try {
    state.flow = await request(`/api/analyst/report/flow?scenicId=${form.scenicId}&start=${encodeURIComponent(form.start)}&end=${encodeURIComponent(form.end)}`);
    state.showFlow = true;
    if (state.flow?.points?.length) {
      const times = state.flow.points.map(d => d.statMinute);
      const inCounts = state.flow.points.map(d => d.inCount || 0);
      const outCounts = state.flow.points.map(d => d.outCount || 0);
      state.flowChart = {
        title: { text: '客流趋势' },
        tooltip: { trigger: 'axis' },
        grid: { left: '3%', right: '4%', bottom: '3%', top: '15%', containLabel: true },
        xAxis: { type: 'category', data: times },
        yAxis: { type: 'value' },
        series: [
          { name: '入园人数', type: 'line', data: inCounts, smooth: true, areaStyle: {} },
          { name: '出园人数', type: 'line', data: outCounts, smooth: true }
        ]
      };
    } else {
      state.flowChart = { title: { text: '客流趋势（暂无数据）' }, series: [] };
    }
    ElMessage.success('客流报表加载成功');
  } catch (e) {
    ElMessage.error('加载客流报表失败');
  }
}

async function loadHeatmap() {
  try {
    state.heatmap = await request(`/api/analyst/report/heatmap?scenicId=${form.scenicId}&start=${encodeURIComponent(form.start)}&end=${encodeURIComponent(form.end)}`);
    state.showHeatmap = true;
    if (state.heatmap?.points?.length) {
      const areas = [...new Set(state.heatmap.points.map(d => d.areaCode || '未分类'))];
      const times = [...new Set(state.heatmap.points.map(d => d.statMinute))];
      const heatData = state.heatmap.points.map(d => [
        times.indexOf(d.statMinute),
        areas.indexOf(d.areaCode || '未分类'),
        d.crowdCount || 0
      ]);
      const maxVal = heatData.length ? Math.max(...heatData.map(d => d[2])) : 1;
      state.heatmapChart = {
        title: { text: '区域客流热力图' },
        tooltip: { trigger: 'item', formatter: (p) => `${p.data[1] !== undefined ? areas[p.data[1]] : ''} ${times[p.data[0]] || ''}: ${p.data[2]}` },
        grid: { left: '120px', right: '60px', bottom: '60px', top: '40px' },
        xAxis: { type: 'category', data: times, axisLabel: { rotate: 30, fontSize: 10 } },
        yAxis: { type: 'category', data: areas },
        visualMap: { min: 0, max: maxVal, calculable: true, orient: 'horizontal', left: 'center', bottom: '5px' },
        series: [{ name: '客流密度', type: 'heatmap', data: heatData, label: { show: false } }]
      };
    } else {
      state.heatmapChart = { title: { text: '区域热力图（暂无数据）' }, series: [] };
    }
    ElMessage.success('热力图加载成功');
  } catch (e) {
    ElMessage.error('加载热力图失败');
  }
}

function saveBlob(blob, filename) {
  const a = document.createElement('a');
  a.href = URL.createObjectURL(blob);
  a.download = filename;
  a.click();
  URL.revokeObjectURL(a.href);
}

async function exportSales() {
  try {
    const blob = await download(`/api/analyst/report/sales/export?scenicId=${form.scenicId}&start=${encodeURIComponent(form.start)}&end=${encodeURIComponent(form.end)}`);
    saveBlob(blob, 'sales_report.csv');
    ElMessage.success('导出成功');
  } catch (e) {
    ElMessage.error('导出失败');
  }
}

async function exportFlow() {
  try {
    const blob = await download(`/api/analyst/report/flow/export?scenicId=${form.scenicId}&start=${encodeURIComponent(form.start)}&end=${encodeURIComponent(form.end)}`);
    saveBlob(blob, 'flow_report.csv');
    ElMessage.success('导出成功');
  } catch (e) {
    ElMessage.error('导出失败');
  }
}

function closePanel(panel) {
  if (panel === 'sales') {
    state.showSales = false;
    return;
  }
  if (panel === 'flow') {
    state.showFlow = false;
    return;
  }
  if (panel === 'heatmap') {
    state.showHeatmap = false;
  }
}
</script>

<template>
  <div class="card">
    <h3>分析报表</h3>
    <div class="toolbar">
      <el-input-number v-model.number="form.scenicId" :min="1" placeholder="景区ID" />
      <el-input v-model="form.start" placeholder="开始 yyyy-MM-dd HH:mm:ss" />
      <el-input v-model="form.end" placeholder="结束 yyyy-MM-dd HH:mm:ss" />
      <el-button @click="loadSales" type="primary">销量</el-button>
      <el-button @click="loadFlow" type="primary">客流</el-button>
      <el-button @click="loadHeatmap" type="primary">热力</el-button>
      <el-button @click="exportSales" type="success">导出销量</el-button>
      <el-button @click="exportFlow" type="success">导出客流</el-button>
    </div>

    <div v-if="state.sales && state.showSales" class="chart-container">
      <div class="panel-head">
        <h4>销量分布</h4>
        <button class="panel-close" @click="closePanel('sales')">×</button>
      </div>
      <div v-if="!state.sales.byTicket?.length" class="empty-tip">当前时段暂无销售数据</div>
      <VChart v-else class="chart" :option="state.salesChart" autoresize />
    </div>

    <div v-if="state.flow && state.showFlow" class="chart-container">
      <div class="panel-head">
        <h4>客流趋势</h4>
        <button class="panel-close" @click="closePanel('flow')">×</button>
      </div>
      <div v-if="!state.flow.points?.length" class="empty-tip">当前时段暂无客流数据</div>
      <VChart v-else class="chart" :option="state.flowChart" autoresize />
    </div>

    <div v-if="state.heatmap && state.showHeatmap" class="chart-container">
      <div class="panel-head">
        <h4>区域热力</h4>
        <button class="panel-close" @click="closePanel('heatmap')">×</button>
      </div>
      <div v-if="!state.heatmap.points?.length" class="empty-tip">当前时段暂无热力数据</div>
      <VChart v-else class="chart" :option="state.heatmapChart" autoresize />
    </div>
  </div>
</template>

<style scoped>
.card { background: #fff; border-radius: 14px; padding: 16px; }
.toolbar { display: flex; flex-wrap: wrap; gap: 8px; margin: 12px 0; align-items: center; }
.chart-container { margin: 20px 0; }
.panel-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.chart { height: 400px; }
h4 { margin: 8px 0; color: #333; }
.panel-close { border: none; background: #e2e8f0; color: #475569; width: 30px; height: 30px; border-radius: 999px; font-size: 20px; line-height: 30px; cursor: pointer; }
.panel-close:hover { background: #cbd5e1; }
.empty-tip { color: #999; font-size: 14px; text-align: center; padding: 40px 0; border: 1px dashed #e0e0e0; border-radius: 8px; }

@media (max-width: 768px) {
  .toolbar { grid-template-columns: 1fr; }
  .chart { height: 300px; }
}
</style>

