<script setup>
import { ref, onMounted } from "vue";
import axios from "axios";

const value = ref(null);

onMounted(async () => {
  try {
    const response = await axios.get("http://localhost:8080/api/complexity");
    const data = response.data; // 假设后端返回的是 List<Map<String, Object>>
    
    if (Array.isArray(data) && data.length === 1) {
      const firstMap = data[0]; 
      const key = Object.keys(firstMap)[0];
      value.value = firstMap[key]; 
    }
    
    console.log("提取的值:", value.value);
  } catch (error) {
    console.error("获取数据失败:", error);
  }
});
</script>

<template>
  <div>
    <div class="title">Complexity:</div>
    <div class="value">{{ value }}</div>
  </div>
</template>

<style scoped>
.title {
  font-weight: bold;  
  font-size: 24px;   
}

.value {
  font-size: 18px; 
}
</style>