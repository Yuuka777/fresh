<script setup>
import { computed } from 'vue'
import { STORAGE_ZONE_MAP } from '@/constants/dict'

/* 无图占位：取商品名首字 + 温区配色，不依赖任何图片素材 */
const props = defineProps({
  name: { type: String, default: '' },
  image: { type: String, default: null },
  zone: { type: Number, default: 1 },
  size: { type: Number, default: 44 }
})

const tone = computed(() => STORAGE_ZONE_MAP[props.zone] || STORAGE_ZONE_MAP[1])
const initial = computed(() => (props.name || '鲜').trim().slice(0, 1))
</script>

<template>
  <div
    class="thumb"
    :style="{ width: size + 'px', height: size + 'px' }"
    :title="image ? name : `${name} · 暂无图片`"
  >
    <img v-if="image" :src="image" :alt="name" class="thumb__img" />
    <span
      v-else
      class="thumb__initial"
      :style="{ background: tone.bg, color: tone.fg, fontSize: Math.round(size * 0.42) + 'px' }"
    >
      {{ initial }}
    </span>
    <span v-if="image" class="thumb__zone" :style="{ background: tone.bg, color: tone.fg }">
      {{ tone.short }}
    </span>
  </div>
</template>

<style scoped>
.thumb {
  position: relative;
  flex: none;
  border-radius: var(--xh-radius-xs);
  overflow: hidden;
  border: 1px solid var(--xh-line);
  background: var(--xh-surface-2);
}
.thumb__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.thumb__initial {
  display: grid;
  place-items: center;
  width: 100%;
  height: 100%;
  font-family: var(--xh-font-display);
  font-weight: 600;
}
.thumb__zone {
  position: absolute;
  right: 0;
  bottom: 0;
  padding: 0 3px;
  font-size: 10px;
  line-height: 14px;
  font-family: var(--xh-font-mono);
}
</style>
