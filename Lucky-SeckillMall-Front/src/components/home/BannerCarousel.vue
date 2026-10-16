<template>
  <div
    class="banner-carousel"
    @mouseenter="pauseAutoPlay"
    @mouseleave="resumeAutoPlay"
  >
    <div v-if="loading" class="carousel-placeholder">
      <span class="carousel-placeholder-text">加载中...</span>
    </div>

    <div v-else-if="loadError" class="carousel-placeholder carousel-placeholder--error">
      <span class="carousel-placeholder-text">{{ loadError }}</span>
    </div>

    <div v-else-if="!bannerList.length" class="carousel-placeholder carousel-placeholder--promo">
      <div class="promo-copy">
        <span class="promo-label">618抢先购</span>
        <h2>爆款购物清单<br />跟买不出错</h2>
        <p>官方立减商品8.5折起</p>
      </div>
      <div class="promo-art" aria-hidden="true">
        <span class="promo-fire">🔥</span>
        <span class="promo-bag">SUMMER<br />SALE</span>
        <span class="promo-tag">SALE</span>
      </div>
      <ul class="carousel-dots carousel-dots--static">
        <li v-for="index in 5" :key="index">
          <span class="carousel-dot" :class="{ active: index === 1 }"></span>
        </li>
      </ul>
    </div>

    <template v-else>
      <div class="carousel-viewport">
        <div
          class="carousel-track"
          :style="{ transform: `translateX(-${currentIndex * 100}%)` }"
        >
          <a
            v-for="(url, index) in bannerList"
            :key="`${url}-${index}`"
            href="#"
            class="carousel-slide"
            @click.prevent
          >
            <img
              :src="url"
              alt=""
              class="slide-image"
              referrerpolicy="no-referrer"
            />
          </a>
        </div>
      </div>

      <button
        v-if="bannerList.length > 1"
        type="button"
        class="carousel-arrow carousel-arrow--prev"
        aria-label="上一张"
        @click="prev"
      >
        ‹
      </button>
      <button
        v-if="bannerList.length > 1"
        type="button"
        class="carousel-arrow carousel-arrow--next"
        aria-label="下一张"
        @click="next"
      >
        ›
      </button>

      <ul v-if="bannerList.length > 1" class="carousel-dots">
        <li v-for="(_, index) in bannerList" :key="index">
          <button
            type="button"
            class="carousel-dot"
            :class="{ active: currentIndex === index }"
            :aria-label="`第 ${index + 1} 张`"
            @click="goTo(index)"
          />
        </li>
      </ul>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { getBannerList } from '@/api/banner'
import { resolveFileUrl } from '@/utils/file'

const bannerUrls = ref<string[]>([])
const loading = ref(true)
const loadError = ref('')
const currentIndex = ref(0)
const AUTO_PLAY_INTERVAL = 4000

const bannerList = computed(() =>
  bannerUrls.value.map((url) => resolveFileUrl(url)).filter(Boolean),
)

let timer: ReturnType<typeof setInterval> | null = null

function goTo(index: number) {
  if (!bannerList.value.length) return
  currentIndex.value = index
}

function next() {
  if (bannerList.value.length <= 1) return
  currentIndex.value = (currentIndex.value + 1) % bannerList.value.length
}

function prev() {
  if (bannerList.value.length <= 1) return
  currentIndex.value =
    (currentIndex.value - 1 + bannerList.value.length) % bannerList.value.length
}

function startAutoPlay() {
  stopAutoPlay()
  if (bannerList.value.length <= 1) return
  timer = setInterval(next, AUTO_PLAY_INTERVAL)
}

function stopAutoPlay() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

function pauseAutoPlay() {
  stopAutoPlay()
}

function resumeAutoPlay() {
  startAutoPlay()
}

async function loadBanners() {
  loading.value = true
  loadError.value = ''
  try {
    const list = await getBannerList()
    bannerUrls.value = Array.isArray(list) ? list : []
    currentIndex.value = 0
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '加载轮播图失败'
    bannerUrls.value = []
  } finally {
    loading.value = false
    startAutoPlay()
  }
}

onMounted(() => {
  loadBanners()
})

onUnmounted(() => {
  stopAutoPlay()
})
</script>

<style scoped>
.banner-carousel {
  position: relative;
  height: 100%;
  border-radius: var(--home-card-radius);
  overflow: hidden;
  background: var(--home-surface);
  border: 1px solid var(--home-border);
  box-shadow: var(--home-card-shadow);
}

.carousel-placeholder {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  background: #fafafa;
}

.carousel-placeholder--promo {
  justify-content: space-between;
  padding: 42px 58px;
  overflow: hidden;
  background:
    radial-gradient(circle at 76% 42%, rgba(255, 255, 255, 0.5), transparent 0 88px, transparent 89px),
    linear-gradient(135deg, #2ab884 0%, #31c695 100%);
  color: #fff;
}

.carousel-placeholder--error {
  background: #fff2f0;
}

.carousel-placeholder-text {
  font-size: 14px;
  color: #999;
}

.carousel-placeholder--error .carousel-placeholder-text {
  color: #f5222d;
}

.promo-copy {
  position: relative;
  z-index: 1;
}

.promo-label {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 10px;
  border-radius: 999px;
  background: #ff2f5f;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
}

.promo-copy h2 {
  margin: 22px 0 12px;
  font-size: 34px;
  line-height: 1.25;
  letter-spacing: 1px;
}

.promo-copy p {
  margin: 0;
  font-size: 15px;
  opacity: 0.9;
}

.promo-art {
  position: relative;
  width: 310px;
  height: 220px;
  z-index: 1;
}

.promo-fire {
  position: absolute;
  left: 24px;
  top: 16px;
  font-size: 44px;
  filter: drop-shadow(0 8px 16px rgba(0, 0, 0, 0.12));
}

.promo-bag {
  position: absolute;
  left: 92px;
  top: 42px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 150px;
  height: 118px;
  border-radius: 18px 18px 28px 28px;
  background: linear-gradient(135deg, #f6f0bd 0%, #d2e698 100%);
  color: rgba(255, 255, 255, 0.78);
  font-size: 24px;
  font-weight: 800;
  line-height: 1.05;
  text-align: center;
  transform: rotate(8deg);
  box-shadow: 0 18px 35px rgba(18, 102, 73, 0.24);
}

.promo-bag::before {
  content: '';
  position: absolute;
  top: -26px;
  left: 34px;
  width: 74px;
  height: 44px;
  border: 10px solid rgba(255, 255, 255, 0.65);
  border-bottom: none;
  border-radius: 50px 50px 0 0;
}

.promo-tag {
  position: absolute;
  right: 22px;
  bottom: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 68px;
  height: 82px;
  border-radius: 8px 8px 16px 16px;
  background: #5ed0cf;
  color: rgba(255, 255, 255, 0.85);
  font-size: 16px;
  font-weight: 800;
  transform: rotate(-18deg);
  box-shadow: 0 14px 28px rgba(18, 102, 73, 0.2);
}

.carousel-viewport {
  height: 100%;
  overflow: hidden;
}

.carousel-track {
  display: flex;
  height: 100%;
  transition: transform 0.5s ease;
}

.carousel-slide {
  flex: 0 0 100%;
  display: block;
  height: 100%;
  min-height: 0;
}

.slide-image {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.carousel-arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 40px;
  height: 40px;
  border: none;
  background: rgba(255, 255, 255, 0.92);
  color: var(--text-primary);
  font-size: 22px;
  line-height: 1;
  cursor: pointer;
  opacity: 0;
  border-radius: 50%;
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.12);
  transition: opacity 0.2s, background 0.2s, transform 0.2s;
}

.banner-carousel:hover .carousel-arrow {
  opacity: 1;
}

.carousel-arrow:hover {
  background: #fff;
  transform: translateY(-50%) scale(1.05);
}

.carousel-arrow--prev {
  left: 12px;
}

.carousel-arrow--next {
  right: 12px;
}

.carousel-dots {
  position: absolute;
  left: 50%;
  bottom: 14px;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0;
  padding: 6px 12px;
  list-style: none;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.18);
  backdrop-filter: blur(4px);
}

.carousel-dots--static {
  left: 58px;
  bottom: 18px;
  transform: none;
  padding: 0;
  background: transparent;
  backdrop-filter: none;
}

.carousel-dots li {
  display: flex;
  align-items: center;
  justify-content: center;
}

.carousel-dot {
  display: block;
  width: 8px;
  height: 8px;
  padding: 0;
  border: none;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.55);
  cursor: pointer;
  transition: background 0.25s ease, transform 0.25s ease, box-shadow 0.25s ease;
}

.carousel-dot.active {
  background: #fff;
  box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.35);
  transform: scale(1.2);
}

.carousel-dot:hover:not(.active) {
  background: rgba(255, 255, 255, 0.85);
}
</style>
