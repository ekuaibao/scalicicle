# scalicicle
A fork of Icicle written in scala
> [https://github.com/intenthq/icicle](https://github.com/intenthq/icicle)

## Changes

* Remove dependencies.
* Modified structure of id.
* Always generate sequenced ids, no retry, no failure.

## Structure

Nearly same as the original structure. Just swap Time and Shard ID.
Like this:

Sign | Shard ID | Time   | Sequence
---- | -------- | ------ | -------
1bit | 41bits   | 10bits | 12bits

But, it uses a different method to generate ids.

***It allows sequence bigger than 12bits. If it happens, just take the high bits of sequence plus to time field.***
