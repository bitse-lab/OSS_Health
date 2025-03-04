<template>
    <div class="market">
        <h1 id="marketTop">Market</h1>
        <div>
            <BoxChartDemo />
        </div>
        <h2>Market</h2>
        <div class="chartRow">
            <RadarChartDemo />
            <RadarChartDemo />
        </div>
        <div class="chartRow">
            <div>
                <p>competitiveness</p>
            </div>
            <div>
                <p>influence</p>
            </div>
        </div>
        <h3 id="marketCompetitiveness">competitiveness</h3>
        <div class="rowTwoMost">
            <LineChartDemo/>
            <LineChartDemo/>
        </div>
        <h3 id="marketInfluence">influence</h3>
        <div class="rowTwoMost">
            <MonthForkLineChart/>
            <MonthStarLineChart/>
            <LineChartDemo/>
        </div>
    </div>
</template>

<script>
import BoxChartDemo from '@/components/BoxChartDemo.vue';
import RadarChartDemo from '@/components/RadarChartDemo.vue';
import LineChartDemo from '@/components/LineChartDemo.vue';
import MonthForkLineChart from '@/components/oldDesign/MonthForkLineChart.vue';
import MonthStarLineChart from '@/components/oldDesign/MonthStarLineChart.vue';

export default {
  name: 'OverViewOSS',
  components: {
    BoxChartDemo,
    RadarChartDemo,
    LineChartDemo,
    MonthForkLineChart,
    MonthStarLineChart,
  },
};
</script>

<style>
.chartRow{
    display: flex;
    flex-direction: row;
}
.chartRow > div {
    flex: 1;
}

.rowTwoMost{
    display: flex;
    flex-direction: row;
    flex-wrap: wrap; /* 允许换行 */
}
.rowTwoMost > div {
    flex: 0 0 49%;  /* 每列宽度占父容器的50% */
    box-sizing: border-box;  /* 包括padding和border在内的宽度 */
}
</style>