<template>
    <div class="software">
        <h1 id="softwareTop">Software</h1>
        <div>
            <BoxChartDemo />
        </div>
        <h2>Software</h2>
        <div class="chartRow">
            <RadarChartDemo />
            <RadarChartDemo />
            <RadarChartDemo />
        </div>
        <div class="chartRow">
            <div>
                <p>quality</p>
            </div>
            <div>
                <p>robustness</p>
            </div>
            <div>
                <p>productivity</p>
            </div>
        </div>
        <h3 id="softwareQuality">quality</h3>
        <div class="rowTwoMost">
            <LineChartDemo/>
            <LineChartDemo/>
        </div>
        <h3 id="softwareRoubstness">robustness</h3>
        <div class="rowTwoMost">
            <LineChartDemo/>
            <LineChartDemo/>
            <LineChartDemo/>
        </div>
        <h3 id="softwareProductivity">productivity</h3>
        <div class="rowTwoMost">
            <LineChartDemo/>
            <LineChartDemo/>
        </div>
    </div>
</template>

<script>
import BoxChartDemo from '@/components/BoxChartDemo.vue';
import RadarChartDemo from '@/components/RadarChartDemo.vue';
import LineChartDemo from '@/components/LineChartDemo.vue';

export default {
  name: 'OverViewOSS',
  components: {
    BoxChartDemo,
    RadarChartDemo,
    LineChartDemo,
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