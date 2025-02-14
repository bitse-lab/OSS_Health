<template>
    <div class="community">
        <h1 id="communityTop">Community</h1>
        <div>
            <BoxChartDemo />
        </div>
        <h2>Community</h2>
        <div class="chartRow">
            <RadarChartDemo />
            <RadarChartDemo />
            <RadarChartDemo />
        </div>
        <div class="chartRow">
            <div>
                <p>organization</p>
            </div>
            <div>
                <p>resilience</p>
            </div>
            <div>
                <p>vigor</p>
            </div>
        </div>
        <h3 id="communityOrganization">organization</h3>
        <div class="rowTwoMost">
            <LineChartDemo/>
            <LineChartDemo/>
        </div>
        <h3 id="communityResilience">resilience</h3>
        <div class="rowTwoMost">
            <LineChartDemo/>
            <LineChartDemo/>
            <LineChartDemo/>
        </div>
        <h3 id="communityVigor">vigor</h3>
        <div class="rowTwoMost">
            <CodeContributorCountLineChart/>
            <LineChartDemo/>
        </div>
    </div>
</template>

<script>
import BoxChartDemo from '@/components/BoxChartDemo.vue';
import RadarChartDemo from '@/components/RadarChartDemo.vue';
import LineChartDemo from '@/components/LineChartDemo.vue';
import CodeContributorCountLineChart from '@/components/oldDesign/CodeContributorCountLineChart.vue';

export default {
  name: 'OverViewOSS',
  components: {
    BoxChartDemo,
    RadarChartDemo,
    LineChartDemo,
    CodeContributorCountLineChart,
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