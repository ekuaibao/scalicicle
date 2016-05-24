# scalicicle
A fork of Icicle written in scala
> [https://github.com/intenthq/icicle](https://github.com/intenthq/icicle)

## Changes

* Remove dependencies.
* Modified structure of id.
* Always generate sequenced ids, no retry, no failure.

## Structure

Nearly as same as the original structure. Just swap Sequence and Shard ID.
Like this:

Sign | Time   | Sequence | Shard ID
---- | ------ | -------- | --------
1bit | 41bits | 12bits   | 10bits

But, it uses a different method to generate ids.

***It allows sequence bigger than 12bits. If it happens, just take the high bits of sequence plus to time field.***
