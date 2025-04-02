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
        <hr class="blue-line">
        <h3 id="softwareQuality" style="color: blue;">quality</h3>
        <div class="row">
            <TechnicalDebtChart/>
            <BugsChart/>
            <CodeSmellsChart/>
            <DuplicatedLinesDensityChart/>
        </div>
        <hr class="blue-line">
        <h3 id="softwareRoubstness" style="color: blue;">robustness</h3>
        <div class="row">
            <ComplexityChart/>
            <CognitiveComplexityChart/>
            <VulnerabilitiesChart/>
            <CommentLinesDensityChart/>
        </div>
        <hr class="blue-line">
        <h3 id="softwareProductivity" style="color: blue;">productivity</h3>
        <div class="rowTwoMost">
            <MonthChangedCodesLineChart/>
            <MonthIssueLineChart/>
            <MonthCommitLineChart/>
            <MonthPRLineChart/>
        </div>
        <hr class="blue-line">
    </div>
</template>

<script>
import BoxChartDemo from '@/components/BoxChartDemo.vue';
import RadarChartDemo from '@/components/RadarChartDemo.vue';
import TechnicalDebtChart from '@/components/oldDesign/TechnicalDebtChart.vue';
import BugsChart from '@/components/oldDesign/BugsChart.vue';
import CodeSmellsChart from '@/components/oldDesign/CodeSmellsChart.vue';
import DuplicatedLinesDensityChart from '@/components/oldDesign/DuplicatedLinesDensityChart.vue';
import ComplexityChart from '@/components/oldDesign/ComplexityChart.vue';
import CognitiveComplexityChart from '@/components/oldDesign/CognitiveComplexityChart.vue';
import VulnerabilitiesChart from '@/components/oldDesign/VulnerabilitiesChart.vue';
import CommentLinesDensityChart from '@/components/oldDesign/CommentLinesDensityChart.vue';
import MonthCommitLineChart from '@/components/oldDesign/MonthCommitLineChart.vue';
import MonthIssueLineChart from '@/components/oldDesign/MonthIssueLineChart.vue';
import MonthPRLineChart from '@/components/oldDesign/MonthPRLineChart.vue';
import MonthChangedCodesLineChart from '@/components/oldDesign/MonthChangedCodesLineChart.vue';

export default {
  name: 'OverViewOSS',
  components: {
    BoxChartDemo,
    RadarChartDemo,
    TechnicalDebtChart,
    BugsChart,
    CodeSmellsChart,
    DuplicatedLinesDensityChart,
    ComplexityChart,
    CognitiveComplexityChart,
    VulnerabilitiesChart,
    CommentLinesDensityChart,
    MonthChangedCodesLineChart,
    MonthCommitLineChart,
    MonthIssueLineChart,
    MonthPRLineChart,
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

.row{
    display: flex;
    flex-direction: row;
    flex-wrap: wrap; /* 允许换行 */
}
.row > div {
    flex: 1;  /* 每列宽度占父容器的50% */
    box-sizing: border-box;  /* 包括padding和border在内的宽度 */
}
.blue-line {
    width: 90%;
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