import { createRouter, createWebHistory } from 'vue-router'
import { buildNotifyRoutes, getDefaultNotifyPath } from '@/constants/notify'
import { isAdmin, isLoggedIn, isMerchant } from '@/utils/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'Home',
      component: () => import('@/views/Home.vue'),
    },
    {
      path: '/search',
      name: 'Search',
      component: () => import('@/views/Search.vue'),
      meta: { title: '搜索' },
    },
    {
      path: '/category',
      name: 'CategoryGoods',
      component: () => import('@/views/CategoryGoods.vue'),
      meta: { title: '分类商品' },
    },
    {
      path: '/goods/detail/:id',
      name: 'GoodsDetail',
      component: () => import('@/views/GoodsDetail.vue'),
      meta: { title: '商品详情' },
    },
    {
      path: '/seckill/:activityId?',
      name: 'Seckill',
      component: () => import('@/views/Seckill.vue'),
      meta: { title: '限时秒杀' },
    },
    {
      path: '/shop/:id',
      name: 'ShopDetail',
      component: () => import('@/views/ShopDetail.vue'),
      meta: { title: '店铺详情' },
    },
    {
      path: '/orders',
      name: 'Orders',
      component: () => import('@/views/Orders.vue'),
      meta: { title: '我的订单', requiresAuth: true },
    },
    {
      path: '/cart',
      name: 'Cart',
      component: () => import('@/views/Cart.vue'),
      meta: { title: '购物车', requiresAuth: true },
    },
    {
      path: '/pay/result',
      name: 'PayResult',
      component: () => import('@/views/PayResult.vue'),
      meta: { title: '支付结果', requiresAuth: true },
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login.vue'),
      meta: { title: '登录' },
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/Register.vue'),
      meta: { title: '注册' },
    },
    {
      path: '/account',
      component: () => import('@/views/account/AccountLayout.vue'),
      meta: { requiresAuth: true },
      redirect: '/account/profile',
      children: [
        {
          path: 'profile',
          name: 'AccountProfile',
          component: () => import('@/views/account/AccountProfile.vue'),
          meta: { title: '个人资料', requiresAuth: true },
        },
        {
          path: 'address',
          name: 'AccountAddress',
          component: () => import('@/views/account/AccountAddress.vue'),
          meta: { title: '收货地址', requiresAuth: true },
        },
        {
          path: 'balance',
          name: 'AccountBalance',
          component: () => import('@/views/account/AccountBalance.vue'),
          meta: { title: '账户余额', requiresAuth: true },
        },
        {
          path: 'password',
          name: 'AccountPassword',
          component: () => import('@/views/account/AccountPassword.vue'),
          meta: { title: '修改密码', requiresAuth: true },
        },
        {
          path: 'cancel',
          name: 'AccountCancel',
          component: () => import('@/views/account/AccountCancel.vue'),
          meta: { title: '注销账号', requiresAuth: true },
        },
      ],
    },
    {
      path: '/shop/create',
      name: 'CreateShop',
      component: () => import('@/views/CreateShop.vue'),
      meta: { title: '免费开店', requiresAuth: true },
    },
    {
      path: '/merchant/shop',
      name: 'MerchantShop',
      component: () => import('@/views/merchant/MerchantShopList.vue'),
      meta: {
        title: '我的店铺',
        requiresAuth: true,
      },
    },
    {
      path: '/merchant/goods',
      name: 'MerchantGoods',
      component: () => import('@/views/merchant/MerchantSection.vue'),
      meta: {
        title: '商品管理',
        requiresAuth: true,
        merchantOnly: true,
      },
    },
    {
      path: '/merchant/goods/detail/:id',
      name: 'MerchantGoodsDetail',
      component: () => import('@/views/merchant/MerchantGoodsDetail.vue'),
      meta: {
        title: '我的商品详情',
        requiresAuth: true,
        merchantOnly: true,
      },
    },
    {
      path: '/merchant/goods/publish',
      name: 'MerchantGoodsPublish',
      component: () => import('@/views/merchant/MerchantGoodsPublish.vue'),
      meta: {
        title: '发布商品',
        requiresAuth: true,
        merchantOnly: true,
      },
    },
    {
      path: '/merchant/orders',
      name: 'MerchantOrders',
      component: () => import('@/views/merchant/MerchantOrderList.vue'),
      meta: {
        title: '订单管理',
        requiresAuth: true,
        merchantOnly: true,
      },
    },
    {
      path: '/merchant/seckill',
      name: 'MerchantSeckill',
      component: () => import('@/views/merchant/MerchantSeckillApply.vue'),
      meta: {
        title: '秒杀商品申请',
        requiresAuth: true,
        merchantOnly: true,
      },
    },
    {
      path: '/favorites',
      component: () => import('@/views/favorites/FavoritesLayout.vue'),
      meta: { requiresAuth: true },
      redirect: '/favorites/goods',
      children: [
        {
          path: 'goods',
          name: 'FavoriteGoods',
          component: () => import('@/views/favorites/FavoriteGoodsList.vue'),
          meta: { title: '收藏商品', favoriteType: 'goods', requiresAuth: true },
        },
        {
          path: 'shops',
          name: 'FavoriteShops',
          component: () => import('@/views/favorites/FavoriteShopList.vue'),
          meta: { title: '收藏店铺', favoriteType: 'shop', requiresAuth: true },
        },
      ],
    },
    {
      path: '/messages',
      component: () => import('@/views/messages/NotifyLayout.vue'),
      meta: { requiresAuth: true },
      redirect: () => getDefaultNotifyPath(),
      children: buildNotifyRoutes(),
    },
    {
      path: '/admin',
      component: () => import('@/views/admin/AdminLayout.vue'),
      meta: { requiresAuth: true, adminOnly: true },
      redirect: '/admin/users',
      children: [
        {
          path: 'users',
          name: 'AdminUsers',
          component: () => import('@/views/admin/AdminUserList.vue'),
          meta: {
            title: '用户管理',
            requiresAuth: true,
            adminOnly: true,
          },
        },
        {
          path: 'banners',
          name: 'AdminBanners',
          component: () => import('@/views/admin/AdminBannerList.vue'),
          meta: {
            title: '背景图管理',
            requiresAuth: true,
            adminOnly: true,
          },
        },
        {
          path: 'shops',
          name: 'AdminShops',
          component: () => import('@/views/admin/AdminShopList.vue'),
          meta: {
            title: '店铺管理',
            requiresAuth: true,
            adminOnly: true,
          },
        },
        {
          path: 'activities',
          name: 'AdminActivities',
          component: () => import('@/views/admin/AdminActivityList.vue'),
          meta: {
            title: '秒杀活动管理',
            requiresAuth: true,
            adminOnly: true,
          },
        },
        {
          path: 'seckill-goods',
          name: 'AdminSeckillGoods',
          component: () => import('@/views/admin/AdminSeckillGoodsList.vue'),
          meta: {
            title: '秒杀商品审核',
            requiresAuth: true,
            adminOnly: true,
          },
        },
        {
          path: 'goods',
          name: 'AdminGoods',
          component: () => import('@/views/admin/AdminGoodsList.vue'),
          meta: {
            title: '商品管理',
            requiresAuth: true,
            adminOnly: true,
          },
        },
        {
          path: 'categories',
          name: 'AdminCategories',
          component: () => import('@/views/admin/AdminCategoryList.vue'),
          meta: {
            title: '分类管理',
            requiresAuth: true,
            adminOnly: true,
          },
        },
        {
          path: 'failed-orders',
          name: 'AdminFailedOrders',
          component: () => import('@/views/admin/AdminFailedOrders.vue'),
          meta: {
            title: '失败订单处理',
            requiresAuth: true,
            adminOnly: true,
          },
        },
      ],
    },
  ],
})

router.beforeEach((to) => {
  if (to.meta.requiresAuth && !isLoggedIn()) {
    return {
      path: '/login',
      query: { redirect: to.fullPath },
    }
  }

  if (to.meta.adminOnly && !isAdmin()) {
    return { path: '/' }
  }

  if (to.meta.merchantOnly && !isMerchant()) {
    return { path: '/' }
  }
})

router.afterEach((to) => {
  const title = to.meta.title as string | undefined
  document.title = title ? `${title} - 秒杀商城` : '秒杀商城'
})

export default router
