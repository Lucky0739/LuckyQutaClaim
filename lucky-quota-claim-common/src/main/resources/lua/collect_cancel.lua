-- 取消收藏（Lua 保证原子性）
-- KEYS[1]: 用户收藏记录 Key
-- KEYS[2]: 收藏计数 Key
-- KEYS[3]: 待同步 Set Key
-- ARGV[1]: 目标 ID（goodsId 或 shopId）
-- 返回值: 1=取消成功, -1=未收藏

if redis.call('EXISTS', KEYS[1]) == 0 then
    return -1
end

redis.call('DEL', KEYS[1])
local count = tonumber(redis.call('GET', KEYS[2]) or '0')
if count > 0 then
    redis.call('DECR', KEYS[2])
end
redis.call('SADD', KEYS[3], ARGV[1])
return 1
