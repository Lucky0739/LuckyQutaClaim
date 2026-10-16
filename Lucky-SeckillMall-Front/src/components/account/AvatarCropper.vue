<template>
  <div v-if="visible" class="avatar-cropper-mask" @click.self="handleCancel">
    <div class="avatar-cropper-dialog">
      <h3 class="avatar-cropper-title">{{ title }}</h3>
      <p class="avatar-cropper-desc">{{ description }}</p>

      <div class="avatar-cropper-body">
        <img ref="imageRef" :src="imageSrc" alt="" class="avatar-cropper-image" />
      </div>

      <div class="avatar-cropper-actions">
        <button type="button" class="account-btn" @click="handleCancel">取消</button>
        <button type="button" class="account-btn account-btn-primary" :disabled="processing" @click="handleConfirm">
          {{ processing ? '处理中...' : confirmText }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import Cropper from 'cropperjs'
import 'cropperjs/dist/cropper.css'
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'

const props = withDefaults(
  defineProps<{
    visible: boolean
    imageSrc: string
    title?: string
    description?: string
    confirmText?: string
    fileName?: string
  }>(),
  {
    title: '裁剪头像',
    description: '拖动或缩放图片，选择合适区域作为头像',
    confirmText: '确定',
    fileName: 'avatar.jpg',
  },
)

const emit = defineEmits<{
  confirm: [file: File]
  cancel: []
}>()

const imageRef = ref<HTMLImageElement | null>(null)
const processing = ref(false)
let cropper: Cropper | null = null

function destroyCropper() {
  cropper?.destroy()
  cropper = null
}

async function initCropper() {
  await nextTick()
  if (!props.visible || !imageRef.value) return

  destroyCropper()
  cropper = new Cropper(imageRef.value, {
    aspectRatio: 1,
    viewMode: 1,
    dragMode: 'move',
    autoCropArea: 0.85,
    background: false,
    responsive: true,
    guides: true,
    center: true,
    highlight: false,
    cropBoxMovable: true,
    cropBoxResizable: true,
  })
}

watch(
  () => [props.visible, props.imageSrc] as const,
  async ([visible]) => {
    if (visible) {
      await initCropper()
    } else {
      destroyCropper()
    }
  },
)

onBeforeUnmount(() => {
  destroyCropper()
})

function handleCancel() {
  emit('cancel')
}

function handleConfirm() {
  if (!cropper || processing.value) return

  processing.value = true
  try {
    const canvas = cropper.getCroppedCanvas({
      width: 200,
      height: 200,
      imageSmoothingEnabled: true,
      imageSmoothingQuality: 'high',
    })

    canvas.toBlob(
      (blob) => {
        processing.value = false
        if (!blob) {
          return
        }
        const file = new File([blob], props.fileName, { type: 'image/jpeg' })
        emit('confirm', file)
      },
      'image/jpeg',
      0.92,
    )
  } catch {
    processing.value = false
  }
}
</script>

<style scoped>
.avatar-cropper-mask {
  position: fixed;
  inset: 0;
  z-index: 1100;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.55);
  padding: 20px;
}

.avatar-cropper-dialog {
  width: 100%;
  max-width: 520px;
  background: #fff;
  border-radius: 8px;
  padding: 24px 24px 20px;
}

.avatar-cropper-title {
  margin: 0 0 8px;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.avatar-cropper-desc {
  margin: 0 0 16px;
  font-size: 13px;
  color: #999;
}

.avatar-cropper-body {
  width: 100%;
  height: 320px;
  overflow: hidden;
  background: #f5f5f5;
  border-radius: 4px;
}

.avatar-cropper-image {
  display: block;
  max-width: 100%;
}

.avatar-cropper-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
}
</style>
