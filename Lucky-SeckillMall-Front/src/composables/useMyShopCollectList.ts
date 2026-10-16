import { onActivated, onMounted, onUnmounted, ref, shallowRef } from 'vue'
import { getMyShopCollectList } from '@/api/shop'
import type { ShopCollectVO } from '@/types/shop'
import { AUTH_CHANGED_EVENT } from '@/utils/auth'
import { SHOP_COLLECT_CHANGED_EVENT } from '@/utils/collectEvent'

const shopList = shallowRef<ShopCollectVO[]>([])
const loading = ref(false)
const errorMsg = ref('')
let subscriberCount = 0

async function loadList() {
  loading.value = true
  errorMsg.value = ''
  try {
    shopList.value = await getMyShopCollectList()
  } catch (err) {
    errorMsg.value = err instanceof Error ? err.message : '加载收藏店铺失败'
    shopList.value = []
  } finally {
    loading.value = false
  }
}

function removeByShopId(shopId: string) {
  shopList.value = shopList.value.filter((row) => row.shopId !== shopId)
}

export function useMyShopCollectList() {
  onMounted(() => {
    subscriberCount += 1
    if (subscriberCount === 1) {
      void loadList()
      window.addEventListener(SHOP_COLLECT_CHANGED_EVENT, loadList)
      window.addEventListener(AUTH_CHANGED_EVENT, loadList)
    }
  })

  onActivated(() => {
    void loadList()
  })

  onUnmounted(() => {
    subscriberCount -= 1
    if (subscriberCount === 0) {
      window.removeEventListener(SHOP_COLLECT_CHANGED_EVENT, loadList)
      window.removeEventListener(AUTH_CHANGED_EVENT, loadList)
    }
  })

  return {
    shopList,
    loading,
    errorMsg,
    loadList,
    removeByShopId,
  }
}
