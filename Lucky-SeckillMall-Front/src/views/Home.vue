<template>
  <div class="page">
    <Header />

    <section class="hero-section">
      <div class="hero-inner">
        <CategorySidebar
          class="hero-category"
          :selected="categoryFilter"
          @select="handleCategorySelect"
          @clear="handleCategoryClear"
        />
        <BannerCarousel class="hero-carousel" />
        <aside class="hero-promos" aria-label="活动推荐">
          <template v-for="card in promoCards" :key="card.title">
            <RouterLink
              v-if="card.to"
              :to="card.to"
              class="promo-card"
              :class="`promo-card--${card.tone}`"
            >
              <span class="promo-title">{{ card.title }}</span>
              <strong class="promo-subtitle">{{ card.subtitle }}</strong>
              <span class="promo-visual">{{ card.visual }}</span>
            </RouterLink>
            <a
              v-else-if="card.categoryChildName"
              href="#"
              class="promo-card"
              :class="`promo-card--${card.tone}`"
              @click.prevent="goCategoryPromo(card.categoryChildName)"
            >
              <span class="promo-title">{{ card.title }}</span>
              <strong class="promo-subtitle">{{ card.subtitle }}</strong>
              <span class="promo-visual">{{ card.visual }}</span>
            </a>
            <a
              v-else
              href="#"
              class="promo-card"
              :class="`promo-card--${card.tone}`"
              @click.prevent
            >
              <span class="promo-title">{{ card.title }}</span>
              <strong class="promo-subtitle">{{ card.subtitle }}</strong>
              <span class="promo-visual">{{ card.visual }}</span>
            </a>
          </template>
        </aside>
      </div>
    </section>

    <section ref="goodsFeedSectionRef" class="home-main">
      <div class="home-main-inner">
        <GoodsFeed
          :category-filter="categoryFilter"
          @clear-category="handleCategoryClear"
        />
      </div>
    </section>

    <HomeFooter />
    <HomeFloatSidebar />
  </div>
</template>

<script setup lang="ts">
import { nextTick, ref } from 'vue'
import { useRouter } from 'vue-router'
import Header from '@/components/Header.vue'
import BannerCarousel from '@/components/home/BannerCarousel.vue'
import CategorySidebar from '@/components/home/CategorySidebar.vue'
import GoodsFeed from '@/components/home/GoodsFeed.vue'
import HomeFloatSidebar from '@/components/home/HomeFloatSidebar.vue'
import HomeFooter from '@/components/home/HomeFooter.vue'
import type { GoodsCategorySelection } from '@/types/goods'
import type { RouteLocationRaw } from 'vue-router'
import {
  buildCategoryGoodsRoute,
  IPHONE_PROMO_CATEGORY_CHILD_NAME,
  resolveCategoryByName,
  SPORTS_OUTDOOR_PROMO_CATEGORY_NAME,
  TABLET_PROMO_CATEGORY_NAME,
} from '@/utils/categoryNavigation'

interface PromoCard {
  title: string
  subtitle: string
  visual: string
  tone: 'red' | 'pink' | 'coral' | 'orange'
  to?: RouteLocationRaw
  /** 点击后按该分类名（父/子分类）跳转商品搜索页 */
  categoryChildName?: string
}

const router = useRouter()
const categoryFilter = ref<GoodsCategorySelection | null>(null)
const promoNavigating = ref(false)
const goodsFeedSectionRef = ref<HTMLElement | null>(null)

function scrollToGoodsFeed() {
  void nextTick(() => {
    goodsFeedSectionRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  })
}

function handleCategorySelect(filter: GoodsCategorySelection) {
  categoryFilter.value = filter
  scrollToGoodsFeed()
}

function handleCategoryClear() {
  categoryFilter.value = null
  scrollToGoodsFeed()
}

async function goCategoryPromo(categoryName: string) {
  if (promoNavigating.value) return
  promoNavigating.value = true
  try {
    const filter = await resolveCategoryByName(categoryName)
    if (filter) {
      await router.push(buildCategoryGoodsRoute(filter))
      return
    }
    await router.push({
      path: '/search',
      query: { type: 'goods', q: categoryName },
    })
  } finally {
    promoNavigating.value = false
  }
}

const promoCards: PromoCard[] = [
  {
    title: '618清空网下单',
    subtitle: '赢平板',
    visual: 'Pad',
    tone: 'red',
    categoryChildName: TABLET_PROMO_CATEGORY_NAME,
  },
  {
    title: 'iPhone17pro',
    subtitle: '直降千元 领300元券',
    visual: 'Phone',
    tone: 'pink',
    categoryChildName: IPHONE_PROMO_CATEGORY_CHILD_NAME,
  },
  {
    title: '秒杀活动',
    subtitle: '限时爆款 抢完即止',
    visual: 'Dress',
    tone: 'coral',
    to: { name: 'Seckill' },
  },
  {
    title: '运动户外',
    subtitle: '尖货爆款直降',
    visual: 'Sport',
    tone: 'orange',
    categoryChildName: SPORTS_OUTDOOR_PROMO_CATEGORY_NAME,
  },
]
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f6f6f6;
}

.hero-section {
  position: relative;
  z-index: 1;
  background:
    radial-gradient(circle at 50% 0, rgba(255, 68, 117, 0.22), transparent 32%),
    linear-gradient(180deg, #fff0f5 0%, #fff 58%, #f6f6f6 100%);
}

.hero-inner {
  display: flex;
  gap: 14px;
  width: var(--layout-width);
  max-width: 100%;
  height: 350px;
  margin: 0 auto;
  padding: 10px 0 18px;
  overflow: visible;
}

.hero-category {
  width: 214px;
  flex-shrink: 0;
  position: relative;
  z-index: 1;
}

.hero-category:has(.category-item.active) {
  /* 低于顶栏(110)，避免子分类浮层挡住搜索类型下拉 */
  z-index: 40;
}

.hero-carousel {
  flex: 1;
  min-height: 0;
}

.hero-promos {
  display: grid;
  grid-template-rows: repeat(4, 1fr);
  gap: 10px;
  width: 220px;
  flex-shrink: 0;
}

.promo-card {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-height: 0;
  padding: 12px 82px 12px 14px;
  border-radius: 12px;
  overflow: hidden;
  color: #fff;
  text-decoration: none;
  box-shadow: 0 6px 16px rgba(255, 79, 102, 0.16);
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.promo-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 22px rgba(255, 79, 102, 0.22);
}

.promo-card--red {
  background: linear-gradient(135deg, #c6151d 0%, #ee2f3a 100%);
}

.promo-card--pink {
  background: linear-gradient(135deg, #e60033 0%, #ff315c 100%);
}

.promo-card--coral {
  background: linear-gradient(135deg, #ff4c45 0%, #ff775c 100%);
}

.promo-card--orange {
  background: linear-gradient(135deg, #ff9c1b 0%, #ffb235 100%);
}

.promo-title {
  font-size: 13px;
  font-weight: 600;
  line-height: 1.35;
}

.promo-subtitle {
  margin-top: 2px;
  font-size: 15px;
  line-height: 1.35;
}

.promo-visual {
  position: absolute;
  right: 14px;
  top: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.28);
  color: rgba(255, 255, 255, 0.9);
  font-size: 12px;
  font-weight: 700;
  transform: translateY(-50%) rotate(-8deg);
}

.home-main {
  padding: 16px 0 64px;
}

.home-main-inner {
  width: var(--layout-width);
  max-width: 100%;
  margin: 0 auto;
}

@media (max-width: 768px) {
  .hero-inner {
    flex-direction: column;
    height: auto;
    gap: 8px;
    padding: 12px;
  }

  .hero-category,
  .hero-promos {
    width: 100%;
    height: auto;
  }

  .hero-promos {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    grid-template-rows: none;
  }

  .hero-carousel {
    height: 180px;
  }

  .home-main-inner {
    padding: 0 12px;
  }
}
</style>
