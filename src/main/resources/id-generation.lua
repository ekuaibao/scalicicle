local count = tonumber(KEYS[1])
local time = redis.call('TIME')
time = tonumber(time[1]) * 1000 + tonumber(time[2]) / 1000
--[[
Here's the fun bit-shifting. The purpose of this is to get a 64-bit ID of the following format:

ACCCCCCCCCCBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBDDDDDDDDDDDD

Where:
 * A is the reserved signed bit of a Java long.
 * C is the logical shard ID, 10 bits in total.
 * B is the timestamp in milliseconds since custom epoch bits, 41 in total.
 * D is the sequence, 12 bits in total.
--]]
local shard = redis.call('GET', 'scalicicle-shard-id')
if shard then
  shard = tonumber(shard)
else
  shard = 0
end
local start_seq = bit.bor(bit.lshift(shard, 22), bit.lshift(time - 1401277473, 12))
local last_seq = toNumber(redis.call('GET', 'scalicicle-counter'))
start_seq = math.max(last_seq, start_seq)
redis.call('SET', 'scalicicle-counter', start_seq + count)

return start_seq