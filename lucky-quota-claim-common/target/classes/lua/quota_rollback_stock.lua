-- 回滚秒杀库存（分布式锁 + 幂等标记）
-- KEYS[1]: 库存 Key
-- KEYS[2]: 用户已购 Key
-- KEYS[3]: 分布式锁 Key（与扣减脚本同一把锁）
-- KEYS[4]: 幂等标记 Key（seckill:rollback:flag:{orderNo}）
-- ARGV[1]: 回滚数量
-- ARGV[2]: 锁标识
-- ARGV[3]: 锁过期秒数
-- ARGV[4]: 幂等标记过期秒数
-- 返回值: 1=成功, 0=已回滚过(幂等), -1=获取锁失败

-- 幂等检查
if redis.call('EXISTS', KEYS[4]) == 1 then
    return 0
end

-- 加锁
local lockResult = redis.call('SET', KEYS[3], ARGV[2], 'NX', 'EX', tonumber(ARGV[3]))
if not lockResult then
    return -1
end

-- 回滚库存
redis.call('INCRBY', KEYS[1], tonumber(ARGV[1]))

-- 回滚用户已购计数（防负数）
local bought = tonumber(redis.call('GET', KEYS[2]) or '0')
local rollbackNum = tonumber(ARGV[1])
if bought >= rollbackNum then
    redis.call('DECRBY', KEYS[2], rollbackNum)
else
    redis.call('SET', KEYS[2], 0)
end

-- 写幂等标记（与锁不同的 TTL，需要覆盖 MQ 可能的重试窗口）
redis.call('SETEX', KEYS[4], tonumber(ARGV[4]), '1')

-- 释放锁
redis.call('DEL', KEYS[3])
return 1
