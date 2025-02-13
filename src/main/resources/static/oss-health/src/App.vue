<template>
  <div id="app">
    <notifications></notifications>
    <router-view :key="$route.fullPath"></router-view>
  </div>
</template>

<script>
</script>

<style lang="scss">
#app {
  // font-family: "Avenir", Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-align: center;
  color: #2c3e50;
}
</style>
