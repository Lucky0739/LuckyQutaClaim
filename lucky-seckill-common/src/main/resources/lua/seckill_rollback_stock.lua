-- 回滚秒杀库存
-- KEYS[1]: 库存 Key
-- KEYS[2]: 用户已购 Key
-- ARGV[1]: 回滚数量
-- 返回值: 1=成功

local rollbackNum = tonumber(ARGV[1])
redis.call('INCRBY', KEYS[1], rollbackNum)
local bought = tonumber(redis.call('GET', KEYS[2]) or '0')
if bought >= rollbackNum then
    redis.call('DECRBY', KEYS[2], rollbackNum)
else
    redis.call('SET', KEYS[2], 0)
end
return 1
