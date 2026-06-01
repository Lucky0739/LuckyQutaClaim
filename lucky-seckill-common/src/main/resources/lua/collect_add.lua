-- 收藏（Lua 保证原子性）
-- KEYS[1]: 用户收藏记录 Key（collect:goods:record:{userId}:{goodsId} 或 collect:shop:record:{userId}:{shopId}）
-- KEYS[2]: 收藏计数 Key（collect:goods:count:{goodsId} 或 collect:shop:count:{shopId}）
-- KEYS[3]: 待同步 Set Key（collect:goods:sync:set 或 collect:shop:sync:set）
-- ARGV[1]: 目标 ID（goodsId 或 shopId，写入同步 Set）
-- 返回值: 1=收藏成功, -1=已收藏

if redis.call('EXISTS', KEYS[1]) == 1 then
    return -1
end

redis.call('SET', KEYS[1], '1')
redis.call('INCR', KEYS[2])
redis.call('SADD', KEYS[3], ARGV[1])
return 1
