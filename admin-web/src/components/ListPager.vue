<script setup>
/* 列表分页条
   为什么不用 el-pagination 自带的 sizes：它内部的下拉没有可访问名称，
   axe 会判 critical（Form elements must have labels）。这里改为自绘的
   每页条数下拉 + 只做翻页的内置分页，语义与呈现都可控。 */
defineProps({
  total: { type: Number, default: 0 },
  page: { type: Number, default: 1 },
  pageSize: { type: Number, default: 10 },
  pageSizes: { type: Array, default: () => [10, 20, 50] }
})

const emit = defineEmits(['change', 'size-change'])
</script>

<template>
  <div class="pager">
    <p class="pager__total">
      共 <span class="xh-num">{{ total }}</span> 条
    </p>

    <label class="pager__size">
      <span class="pager__size-text">每页</span>
      <el-select
        :model-value="pageSize"
        aria-label="每页显示条数"
        class="pager__select"
        size="small"
        @change="(v) => emit('size-change', v)"
      >
        <el-option v-for="s in pageSizes" :key="s" :value="s" :label="`${s} 条`" />
      </el-select>
    </label>

    <el-pagination
      :current-page="page"
      :page-size="pageSize"
      :total="total"
      layout="prev, pager, next"
      background
      @current-change="(p) => emit('change', p)"
    />
  </div>
</template>

<style scoped>
.pager {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--xh-4);
  padding: var(--xh-3) var(--xh-5);
  border-top: 1px solid var(--xh-line-soft);
}
.pager__total {
  margin-right: auto;
  font-size: var(--xh-text-xs);
  color: var(--xh-ink-400);
}
.pager__size {
  display: flex;
  align-items: center;
  gap: var(--xh-2);
}
.pager__size-text {
  font-size: var(--xh-text-xs);
  color: var(--xh-ink-400);
}
.pager__select {
  width: 104px;
}
</style>
