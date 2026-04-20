-- 在这个 Lua 脚本里需要进行操作如下：
--      1. 判断优惠券是否还有库存
--      2. 判断是否还在优惠券抢购时间
--      3. 判断用户是否已经购买过优惠券
--      4. 扣减优惠券库存
--      5. 将用用户添加到购买用户Set

-- 参数解释
-- KEYS:
-- KEYS[1]: 优惠券相关信息 key
-- KEYS[2]: 优惠券购买用户Set集合 key

-- ARGV
-- ARGV[1]: 现在时间戳 now
-- ARGV[2]: 用户id

-- 返回结果
-- 0：下单成功
-- 1: 优惠券库存不足
-- 2: 秒杀活动未开始
-- 3: 秒杀活动已结束
-- 4: 该用户已经下过单
-- 5: 优惠券不存在

local begin_time = tonumber(redis.call('hget', KEYS[1], 'beginTime'))
local end_time = tonumber(redis.call('hget', KEYS[1], 'endTime'))
local now = tonumber(ARGV[1]) -- 确保传入的也是数字
local stock = tonumber(redis.call('hget', KEYS[1], 'stock'))

if stock == nil or begin_time == nil or end_time == nil then
    return 5
end

if (stock <= 0) then
    -- 优惠券库存不足
    return 1
end
if (begin_time > now) then
    -- 秒杀活动未开始
    return 2
end
if (end_time < now) then
    -- 秒杀活动已结束
    return 3
end

if (redis.call('sismember', KEYS[2], ARGV[2]) == 1) then
    -- 该用户已经下过单
    return 4
end

-- 扣减优惠券库存
redis.call('hincrby', KEYS[1], 'stock', -1)
-- 将用户id添加到set中
redis.call('sadd', KEYS[2], ARGV[2])

-- 下单成功
return 0