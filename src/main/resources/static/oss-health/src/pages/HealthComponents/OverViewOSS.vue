<template>
    <div class="overView">
        <h1 id="overViewTop">Health Overview</h1>
        <div>
            <Line3ChartDemo />
        </div>
        <h2>First-level Dimension Health Overview</h2>
        <div class="chartRow">
            <BoxChartDemo />
            <BoxChartDemo />
            <BoxChartDemo />
        </div>
        <div class="chartRow">
            <div>
                <p>Software</p>
            </div>
            <div>
                <p>Community</p>
            </div>
            <div>
                <p>Market</p>
            </div>
        </div>
        <h2>Second-level Dimension Health Overview</h2>
        <h3>Software</h3>
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
        <h3>Community</h3>
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
        <h3>Market</h3>
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
    </div>
</template>

<script>
import BoxChartDemo from '@/components/BoxChartDemo.vue';
import Line3ChartDemo from '@/components/Line3ChartDemo.vue';
import RadarChartDemo from '@/components/RadarChartDemo.vue';

export default {
  name: 'OverViewOSS',
  components: {
    BoxChartDemo,
    RadarChartDemo,
    Line3ChartDemo,
  },
};
</script>

<style>
.chartRow{
    display: flex;
    flex-direction: row;
    align-items: center;
}
.chartRow > div {
    flex: 1;
}
</style>