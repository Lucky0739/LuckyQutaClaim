-- 安全释放分布式锁（仅持有者可释放）
-- KEYS[1]: 锁 Key
-- ARGV[1]: 锁标识
-- 返回值: 1=释放成功, 0=锁不存在或不属于当前持有者

if redis.call('get', KEYS[1]) == ARGV[1] then
    return redis.call('del', KEYS[1])
else
    return 0
end
