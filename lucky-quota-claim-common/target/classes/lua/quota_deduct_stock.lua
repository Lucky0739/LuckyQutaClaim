-- 秒杀扣减库存（Lua + 分布式锁保证原子性）
-- KEYS[1]: 库存 Key
-- KEYS[2]: 用户已购 Key
-- KEYS[3]: 分布式锁 Key
-- ARGV[1]: 购买数量
-- ARGV[2]: 每人限购数量
-- ARGV[3]: 锁过期秒数
-- ARGV[4]: 锁标识
-- 返回值: 1=成功, -1=获取锁失败, -2=超过限购, -3=库存不足

local lockResult = redis.call('SET', KEYS[3], ARGV[4], 'NX', 'EX', tonumber(ARGV[3]))
if not lockResult then
    return -1
end

local buyNum = tonumber(ARGV[1])
local limitNum = tonumber(ARGV[2])
local bought = tonumber(redis.call('GET', KEYS[2]) or '0')
if bought + buyNum > limitNum then
    redis.call('DEL', KEYS[3])
    return -2
end

local stock = tonumber(redis.call('GET', KEYS[1]) or '0')
if stock < buyNum then
    redis.call('DEL', KEYS[3])
    return -3
end

redis.call('DECRBY', KEYS[1], buyNum)
redis.call('INCRBY', KEYS[2], buyNum)
redis.call('DEL', KEYS[3])
return 1
