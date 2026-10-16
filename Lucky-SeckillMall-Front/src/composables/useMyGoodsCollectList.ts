import { onActivated, onMounted, onUnmounted, ref, shallowRef } from 'vue'
import { getMyGoodsCollectList } from '@/api/goods'
import type { GoodsCollectVO } from '@/types/goods'
import { AUTH_CHANGED_EVENT } from '@/utils/auth'
import { GOODS_COLLECT_CHANGED_EVENT } from '@/utils/collectEvent'

const goodsList = shallowRef<GoodsCollectVO[]>([])
const loading = ref(false)
const errorMsg = ref('')
let subscriberCount = 0

async function loadList() {
  loading.value = true
  errorMsg.value = ''
  try {
    goodsList.value = await getMyGoodsCollectList()
  } catch (err) {
    errorMsg.value = err instanceof Error ? err.message : '加载收藏商品失败'
    goodsList.value = []
  } finally {
    loading.value = false
  }
}

function removeByGoodsId(goodsId: string) {
  goodsList.value = goodsList.value.filter(
    (row) => (row.goodsId || row.id) !== goodsId,
  )
}

export function useMyGoodsCollectList() {
  onMounted(() => {
    subscriberCount += 1
    if (subscriberCount === 1) {
      void loadList()
      window.addEventListener(GOODS_COLLECT_CHANGED_EVENT, loadList)
      window.addEventListener(AUTH_CHANGED_EVENT, loadList)
    }
  })

  onActivated(() => {
    void loadList()
  })

  onUnmounted(() => {
    subscriberCount -= 1
    if (subscriberCount === 0) {
      window.removeEventListener(GOODS_COLLECT_CHANGED_EVENT, loadList)
      window.removeEventListener(AUTH_CHANGED_EVENT, loadList)
    }
  })

  return {
    goodsList,
    loading,
    errorMsg,
    loadList,
    removeByGoodsId,
  }
}
