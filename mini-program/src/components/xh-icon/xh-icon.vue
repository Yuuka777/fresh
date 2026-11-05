<script setup>
/* 图标：读取 static/icons 下的 PNG，尺寸与配色由 props 控制
   tone: ink（默认）| teal（选中/强调）| white（深色底上） */
const props = defineProps({
  name: { type: String, required: true },
  tone: { type: String, default: 'ink' },
  size: { type: Number, default: 32 }
})
</script>

<template>
  <image
    class="xh-icon"
    :src="`/static/icons/${tone}/${name}.png`"
    mode="aspectFit"
    :style="`width:${size}rpx;height:${size}rpx`"
  />
</template>

<style lang="scss" scoped>
.xh-icon {
  display: block;
  flex-shrink: 0;
}
</style>
