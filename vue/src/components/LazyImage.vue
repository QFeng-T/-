<template>
  <el-image
    :src="imgSrc"
    :fit="fit"
    :class="['lazy-image', { 'lazy-loaded': isLoaded }]"
    :preview-src-list="previewSrcList"
    :preview-teleported="previewTeleported"
    :initial-index="initialIndex"
    :lazy="true"
    @load="onLoad"
    @error="onError"
  >
    <template #error>
      <div class="image-slot">
        <el-icon><PictureFilled /></el-icon>
      </div>
    </template>
    <template #placeholder>
      <div class="image-slot">
        <el-icon class="loading-icon"><Loading /></el-icon>
      </div>
    </template>
  </el-image>
</template>

<script setup>
import { ref, computed } from 'vue'
import { PictureFilled, Loading } from '@element-plus/icons-vue'

const props = defineProps({
  src: {
    type: String,
    required: true
  },
  fit: {
    type: String,
    default: 'cover'
  },
  previewSrcList: {
    type: Array,
    default: () => []
  },
  previewTeleported: {
    type: Boolean,
    default: true
  },
  initialIndex: {
    type: Number,
    default: 0
  },
  fallbackSrc: {
    type: String,
    default: 'https://via.placeholder.com/100x100?text=Image'
  }
})

const emit = defineEmits(['load', 'error'])

const isLoaded = ref(false)
const imgSrc = computed(() => props.src || props.fallbackSrc)

const onLoad = (e) => {
  isLoaded.value = true
  emit('load', e)
}

const onError = (e) => {
  isLoaded.value = false
  imgSrc.value = props.fallbackSrc
  emit('error', e)
}
</script>

<style scoped>
.lazy-image {
  transition: all 0.3s ease;
}

.lazy-image :deep(.el-image__inner) {
  transition: all 0.3s ease;
}

.lazy-loaded :deep(.el-image__inner) {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.image-slot {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
  background: #f5f7fa;
  color: #909399;
  font-size: 14px;
}

.loading-icon {
  animation: rotate 1s linear infinite;
  font-size: 20px;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
