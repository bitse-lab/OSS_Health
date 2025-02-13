<template>
    <div class="text-box">
      vuejs/core
    </div>
  </template>
  
  <script>
  export default {
    name: 'LineUp',
  };
  </script>
  
  <style scoped>
  .text-box {
    display: flex;
    align-items: center; /* 垂直居中文本 */
    justify-content: flex-start; /* 左对齐 */
    padding: 10px;
    background-color: rgba(144, 238, 144, 0.2); /* 浅绿色背景，透明度20% */
    color: black; /* 黑色字体 */
    font-size: 16px;
    border-radius: 5px; /* 可选，设置圆角 */
    width: 100%; /* 设置宽度 */
    height: 40px; /* 设置高度 */
  }
  </style>
  