const { defineConfig } = require('@vue/cli-service')
module.exports = defineConfig({
  transpileDependencies: true,
  devServer: {
    port: 8081  // 设置端口号为 8081，修改为你需要的端口
  }
})
