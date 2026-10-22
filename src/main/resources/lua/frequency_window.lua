-- KEYS[1] 频率统计 key（ZSET）
-- ARGV[1] 当前时间戳 ms
-- ARGV[2] 滑动窗口长度 ms
-- ARGV[3] 本次交易唯一成员（transactionId）
-- 原子执行：写入本次记录 -> 清理窗口外记录 -> 返回窗口内计数
redis.call('ZADD', KEYS[1], ARGV[1], ARGV[3])
redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, tonumber(ARGV[1]) - tonumber(ARGV[2]))
return redis.call('ZCARD', KEYS[1])
