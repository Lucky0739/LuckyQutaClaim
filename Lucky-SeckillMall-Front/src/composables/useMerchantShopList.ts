import { onMounted, ref } from 'vue'
import { deleteMyShop, getMyShopList } from '@/api/shop'
import type { ShopApplyVO } from '@/types/shop'

export function useMerchantShopList() {
  const shops = ref<ShopApplyVO[]>([])
  const total = ref(0)
  const loading = ref(false)
  const deletingId = ref('')
  const errorMsg = ref('')
  const actionMsg = ref('')
  const actionMsgType = ref<'success' | 'error' | ''>('')

  async function loadList() {
    loading.value = true
    errorMsg.value = ''

    try {
      const page = await getMyShopList()
      shops.value = page.records ?? []
      total.value = Number(page.total ?? shops.value.length)
    } catch (err) {
      errorMsg.value = err instanceof Error ? err.message : '加载店铺列表失败'
      shops.value = []
      total.value = 0
    } finally {
      loading.value = false
    }
  }

  async function deleteShop(shop: ShopApplyVO) {
    if (!shop.id || deletingId.value) return false

    const confirmed = window.confirm(
      `确定删除店铺「${shop.shopName}」吗？\n删除后不可恢复。若仍有上架商品，请先下架全部商品后再删除。`,
    )
    if (!confirmed) return false

    deletingId.value = shop.id
    actionMsg.value = ''
    actionMsgType.value = ''
    try {
      await deleteMyShop(shop.id)
      shops.value = shops.value.filter((item) => item.id !== shop.id)
      total.value = shops.value.length
      actionMsg.value = '店铺已删除'
      actionMsgType.value = 'success'
      window.setTimeout(() => {
        if (actionMsg.value === '店铺已删除') {
          actionMsg.value = ''
          actionMsgType.value = ''
        }
      }, 2000)
      return true
    } catch (err) {
      actionMsg.value = err instanceof Error ? err.message : '删除店铺失败'
      actionMsgType.value = 'error'
      return false
    } finally {
      deletingId.value = ''
    }
  }

  onMounted(() => {
    loadList()
  })

  return {
    shops,
    total,
    loading,
    deletingId,
    errorMsg,
    actionMsg,
    actionMsgType,
    loadList,
    deleteShop,
  }
}
