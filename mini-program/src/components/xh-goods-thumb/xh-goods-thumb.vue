<script setup>
/* 商品/组合箱图片位
   素材约束：商品图初始为空，故用「名称首字 + 温区配色」色块占位，不依赖任何图片素材 */
import { computed } from 'vue'
import { zoneTone } from '@/common/dict'

const props = defineProps({
  name: { type: String, default: '' },
  image: { type: String, default: '' },
  zone: { type: Number, default: 1 },
  size: { type: Number, default: 160 },
  radius: { type: Number, default: 8 },
  /* 调用方拿不到温区时（如购物车行）关掉角标，免得标出错误的温区 */
  showZone: { type: Boolean, default: true }
})

const tone = computed(() => zoneTone(props.zone))
const initial = computed(() => (props.name || '鲜').trim().slice(0, 1))
</script>

<template>
  <view
    class="xh-thumb"
    :style="{
      width: size + 'rpx',
      height: size + 'rpx',
      borderRadius: radius + 'rpx'
    }"
  >
    <image v-if="image" class="thumb__img" :src="image" mode="aspectFill" />
    <text
      v-else
      class="xh-thumb__initial"
      :style="{ background: tone.bg, color: tone.fg, fontSize: size * 0.42 + 'rpx' }"
    >
      {{ initial }}
    </text>
    <text v-if="showZone" class="thumb__zone" :style="{ background: tone.bg, color: tone.fg }">
      {{ tone.short }}
    </text>
  </view>
</template>

<style lang="scss" scoped>
.thumb__img {
  width: 100%;
  height: 100%;
}
.xh-thumb__initial {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.thumb__zone {
  position: absolute;
  right: 0;
  bottom: 0;
  font-size: $xh-text-xs;
  line-height: 24rpx;
  padding: 0 6rpx;
  border-top-left-radius: $xh-radius-xs;
}
</style>
