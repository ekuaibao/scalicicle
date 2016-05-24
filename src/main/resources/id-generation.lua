local count = tonumber(ARGV[1])
local time = tonumber(ARGV[2])
local shard = redis.call('GET', 'scalicicle-shard-id')
if shard then
  shard = tonumber(shard)
else
  shard = 0
end
--[[
Here's the fun bit-shifting. The purpose of this is to get a 64-bit ID of the following format:

ABBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBDDDDDDDDDDDDCCCCCCCCCC

Where:
 * A is the reserved signed bit of a Java long.
 * B is the timestamp in milliseconds since custom epoch bits, 41 in total.
 * C is the logical shard ID, 10 bits in total.
 * D is the sequence, 12 bits in total.
--]]
local start_seq = bit.bor(bit.lshift(time - 1401277473000, 22), shard)
local last_seq = toNumber(redis.call('GET', 'scalicicle-counter'))
start_seq = math.max(last_seq, start_seq)
last_seq = start_seq + bit.lshift(count, 10)
redis.call('SET', 'scalicicle-counter', last_seq)

return start_seq