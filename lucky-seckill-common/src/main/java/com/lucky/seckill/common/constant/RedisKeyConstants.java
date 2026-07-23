package com.lucky.seckill.common.constant;

/**
 * Redis Key 常量（跨服务共享）
 */
public final class RedisKeyConstants {

    private RedisKeyConstants() {
    }

    /**
     * 用户资料缓存（Hash）：user:profile:{userId}
     */
    public static final String USER_PROFILE_KEY_PREFIX = "user:profile:";

    /**
     * 用户角色缓存：user:roles:{userId}
     */
    public static final String USER_ROLES_KEY_PREFIX = "user:roles:";

    public static String userProfileKey(Long userId) {
        return USER_PROFILE_KEY_PREFIX + userId;
    }

    /**
     * 用户已读消息 ZSet：notify:read:{userId}:{notifyType}，member=消息ID，score=已读时间戳
     */
    public static final String NOTIFY_READ_KEY_PREFIX = "notify:read:";

    public static String notifyReadKey(Long userId, Integer notifyType) {
        return NOTIFY_READ_KEY_PREFIX + userId + ":" + notifyType;
    }

    /**
     * 秒杀商品缓存：seckill:goods:cache:{seckillGoodsId}
     */
    public static final String SECKILL_GOODS_CACHE_KEY_PREFIX = "seckill:goods:cache:";

    /**
     * 秒杀活动缓存：seckill:activity:cache:{activityId}
     */
    public static final String SECKILL_ACTIVITY_CACHE_KEY_PREFIX = "seckill:activity:cache:";

    /** 秒杀商品/活动缓存 TTL（分钟） */
    public static final long SECKILL_CACHE_TTL_MINUTES = 120;

    /**
     * 秒杀库存：seckill:stock:{activityId}:{goodsId}:{skuId}
     */
    public static final String SECKILL_STOCK_KEY_PREFIX = "seckill:stock:";

    /**
     * 用户已购数量：seckill:bought:{activityId}:{userId}:{goodsId}:{skuId}
     */
    public static final String SECKILL_BOUGHT_KEY_PREFIX = "seckill:bought:";

    /**
     * 秒杀分布式锁：seckill:lock:{activityId}:{goodsId}:{skuId}
     */
    public static final String SECKILL_LOCK_KEY_PREFIX = "seckill:lock:";

    /**
     * 活动预热标记：seckill:warmup:{activityId}
     */
    public static final String SECKILL_WARMUP_KEY_PREFIX = "seckill:warmup:";

    /**
     * 活动预热分布式锁：seckill:warmup:lock:{activityId}
     */
    public static final String SECKILL_WARMUP_LOCK_KEY_PREFIX = "seckill:warmup:lock:";

    /**
     * 活动预热重试计数：seckill:warmup:retry:{activityId}
     * <p>
     * 每次预热失败 +1，超过上限后停止重试，预热成功后清除。
     * TTL 10 分钟，覆盖合理重试窗口后可自动清理。
     */
    public static final String SECKILL_WARMUP_RETRY_KEY_PREFIX = "seckill:warmup:retry:";

    /** 预热重试计数 TTL（分钟），超过此时间未成功则自动清除 */
    public static final long SECKILL_WARMUP_RETRY_TTL_MINUTES = 10;

    /**
     * 秒杀回滚幂等标记：seckill:rollback:flag:{orderNo}
     */
    public static final String SECKILL_ROLLBACK_FLAG_KEY_PREFIX = "seckill:rollback:flag:";

    public static String seckillGoodsCacheKey(Long seckillGoodsId) {
        return SECKILL_GOODS_CACHE_KEY_PREFIX + seckillGoodsId;
    }

    public static String seckillActivityCacheKey(Long activityId) {
        return SECKILL_ACTIVITY_CACHE_KEY_PREFIX + activityId;
    }

    public static String seckillStockKey(Long activityId, Long goodsId, Long skuId) {
        return SECKILL_STOCK_KEY_PREFIX + activityId + ":" + goodsId + ":" + skuId;
    }

    public static String seckillBoughtKey(Long activityId, Long userId, Long goodsId, Long skuId) {
        return SECKILL_BOUGHT_KEY_PREFIX + activityId + ":" + userId + ":" + goodsId + ":" + skuId;
    }

    public static String seckillLockKey(Long activityId, Long goodsId, Long skuId) {
        return SECKILL_LOCK_KEY_PREFIX + activityId + ":" + goodsId + ":" + skuId;
    }

    public static String seckillWarmupKey(Long activityId) {
        return SECKILL_WARMUP_KEY_PREFIX + activityId;
    }

    public static String seckillWarmupLockKey(Long activityId) {
        return SECKILL_WARMUP_LOCK_KEY_PREFIX + activityId;
    }

    public static String seckillWarmupRetryKey(Long activityId) {
        return SECKILL_WARMUP_RETRY_KEY_PREFIX + activityId;
    }

    public static String seckillRollbackFlagKey(String orderNo) {
        return SECKILL_ROLLBACK_FLAG_KEY_PREFIX + orderNo;
    }

    /**
     * 商品收藏记录：collect:goods:record:{userId}:{goodsId}
     */
    public static final String GOODS_COLLECT_RECORD_KEY_PREFIX = "collect:goods:record:";

    /**
     * 商品收藏计数：collect:goods:count:{goodsId}
     */
    public static final String GOODS_COLLECT_COUNT_KEY_PREFIX = "collect:goods:count:";

    /**
     * 商品收藏数待同步 Set：collect:goods:sync:set
     */
    public static final String GOODS_COLLECT_SYNC_SET_KEY = "collect:goods:sync:set";

    /**
     * 首页商品收藏排行 ZSet：member=goodsId，score=收藏数
     */
    public static final String GOODS_RANK_COLLECT_ZSET = "goods:rank:collect";

    /**
     * 首页商品销量排行 ZSet：member=goodsId，score=已售数量（SKU 销量之和）
     */
    public static final String GOODS_RANK_SALES_ZSET = "goods:rank:sales";

    /**
     * 首页综合权重排行 ZSet：score = 收藏权重×收藏数 + 销量权重×已售数
     */
    public static final String GOODS_RANK_WEIGHT_ZSET = "goods:rank:weight";

    /**
     * 店铺收藏记录：collect:shop:record:{userId}:{shopId}
     */
    public static final String SHOP_COLLECT_RECORD_KEY_PREFIX = "collect:shop:record:";

    /**
     * 店铺收藏计数：collect:shop:count:{shopId}
     */
    public static final String SHOP_COLLECT_COUNT_KEY_PREFIX = "collect:shop:count:";

    /**
     * 店铺收藏数待同步 Set：collect:shop:sync:set
     */
    public static final String SHOP_COLLECT_SYNC_SET_KEY = "collect:shop:sync:set";

    public static String goodsCollectRecordKey(Long userId, Long goodsId) {
        return GOODS_COLLECT_RECORD_KEY_PREFIX + userId + ":" + goodsId;
    }

    public static String goodsCollectCountKey(Long goodsId) {
        return GOODS_COLLECT_COUNT_KEY_PREFIX + goodsId;
    }

    public static String shopCollectRecordKey(Long userId, Long shopId) {
        return SHOP_COLLECT_RECORD_KEY_PREFIX + userId + ":" + shopId;
    }

    public static String shopCollectCountKey(Long shopId) {
        return SHOP_COLLECT_COUNT_KEY_PREFIX + shopId;
    }

    /**
     * 商品分页缓存：goods:page:{queryHash}
     */
    public static final String GOODS_PAGE_KEY_PREFIX = "goods:page:";

    /**
     * 商品基础信息缓存：goods:info:{goodsId}
     */
    public static final String GOODS_INFO_KEY_PREFIX = "goods:info:";

    /**
     * 商品详情缓存：goods:detail:{goodsId}
     */
    public static final String GOODS_DETAIL_KEY_PREFIX = "goods:detail:";

    /**
     * 商品分页缓存重建锁：goods:lock:page:{queryHash}
     */
    public static final String GOODS_PAGE_LOCK_KEY_PREFIX = "goods:lock:page:";

    /**
     * 商品基础信息缓存重建锁：goods:lock:info:{goodsId}
     */
    public static final String GOODS_INFO_LOCK_KEY_PREFIX = "goods:lock:info:";

    /**
     * 商品详情缓存重建锁：goods:lock:detail:{goodsId}
     */
    public static final String GOODS_DETAIL_LOCK_KEY_PREFIX = "goods:lock:detail:";

    public static String goodsPageKey(String queryHash) {
        return GOODS_PAGE_KEY_PREFIX + queryHash;
    }

    public static String goodsPageLockKey(String queryHash) {
        return GOODS_PAGE_LOCK_KEY_PREFIX + queryHash;
    }

    public static String goodsInfoKey(Long goodsId) {
        return GOODS_INFO_KEY_PREFIX + goodsId;
    }

    public static String goodsInfoLockKey(Long goodsId) {
        return GOODS_INFO_LOCK_KEY_PREFIX + goodsId;
    }

    public static String goodsDetailKey(Long goodsId) {
        return GOODS_DETAIL_KEY_PREFIX + goodsId;
    }

    public static String goodsDetailLockKey(Long goodsId) {
        return GOODS_DETAIL_LOCK_KEY_PREFIX + goodsId;
    }

    /**
     * 秒杀订单异步处理状态（仅Redis，不落库）：seckill:order:status:{userId}:{orderNo}
     * <p>
     * 值：-1=处理中，0=待支付（创建成功），-2=创建失败（MQ/DLQ 重试全部失败）
     */
    public static final String SECKILL_ORDER_STATUS_PREFIX = "seckill:order:status:";

    /** 秒杀订单处理状态 TTL（分钟），超时后前端可认为订单创建失败 */
    public static final long SECKILL_ORDER_STATUS_TTL_MINUTES = 30;

    /** Redis 临时状态：订单处理中（MQLua扣库存成功，等待消费者创建订单） */
    public static final int ORDER_STATUS_PROCESSING = -1;

    /** Redis 临时状态：订单创建失败（DLQ 重试全部失败，库存已回滚） */
    public static final int ORDER_STATUS_FAILED = -2;

    public static String seckillOrderStatusKey(Long userId, String orderNo) {
        return SECKILL_ORDER_STATUS_PREFIX + userId + ":" + orderNo;
    }

    /**
     * AI 客服会话记忆：ai:chat:memory:{chatId}
     */
    public static final String AI_CHAT_MEMORY_KEY_PREFIX = "ai:chat:memory:";

    public static String aiChatMemoryKey(String chatId) {
        return AI_CHAT_MEMORY_KEY_PREFIX + chatId;
    }
}
