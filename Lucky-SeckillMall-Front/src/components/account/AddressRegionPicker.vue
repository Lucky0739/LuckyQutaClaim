<template>
  <div class="address-region-picker">
    <ElCascader
      v-model="selectedCodes"
      :options="regionOptions"
      :props="cascaderProps"
      clearable
      filterable
      placeholder="请选择省 / 市 / 区"
      class="address-region-cascader"
      :class="{ 'address-region-cascader--error': !!error }"
      @change="handleChange"
    />
    <p v-if="error" class="form-field-error">{{ error }}</p>
  </div>
</template>

<script setup lang="ts">
import { ElCascader } from 'element-plus'
import type { CascaderOption, CascaderValue } from 'element-plus'
import 'element-plus/es/components/cascader/style/css'
import { ref, watch } from 'vue'
import { codesToRegion, regionData, regionToCodes } from '@/utils/region'

const props = defineProps<{
  province: string
  city: string
  area: string
  error?: string
}>()

const emit = defineEmits<{
  'update:province': [value: string]
  'update:city': [value: string]
  'update:area': [value: string]
}>()

const regionOptions = regionData as CascaderOption[]
const selectedCodes = ref<string[]>([])

const cascaderProps = {
  value: 'value',
  label: 'label',
  children: 'children',
}

watch(
  () => [props.province, props.city, props.area] as const,
  ([province, city, area]) => {
    selectedCodes.value = regionToCodes(province, city, area)
  },
  { immediate: true },
)

function handleChange(value: CascaderValue | null | undefined) {
  if (!Array.isArray(value) || value.length !== 3) {
    emit('update:province', '')
    emit('update:city', '')
    emit('update:area', '')
    return
  }

  const codes = value.map(String)
  const region = codesToRegion(codes)
  emit('update:province', region.province)
  emit('update:city', region.city)
  emit('update:area', region.area)
}
</script>

<style scoped>
.address-region-picker {
  width: 100%;
}

.address-region-cascader {
  width: 100%;
}

:deep(.address-region-cascader .el-input__wrapper) {
  min-height: 30px;
  border-radius: 4px;
  box-shadow: 0 0 0 1px #d9d9d9 inset;
}

:deep(.address-region-cascader .el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--accent) inset;
}

:deep(.address-region-cascader .el-input.is-focus .el-input__wrapper) {
  box-shadow: 0 0 0 1px var(--accent) inset;
}

:deep(.address-region-cascader--error .el-input__wrapper) {
  box-shadow: 0 0 0 1px #f5222d inset;
}
</style>
