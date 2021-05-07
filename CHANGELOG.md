# Stepwise CHANGELOG

We use [Break Versioning][breakver]. The version numbers follow a `<major>.<minor>.<patch>` scheme with the following intent:

| Bump    | Intent                                                     |
| ------- | ---------------------------------------------------------- |
| `major` | Major breaking changes -- check the changelog for details. |
| `minor` | Minor breaking changes -- check the changelog for details. |
| `patch` | No breaking changes, ever!!                                |

`-SNAPSHOT` versions are preview versions for upcoming releases.

[breakver]: https://github.com/ptaoussanis/encore/blob/master/BREAK-VERSIONING.md

## 1.0.0 (TBD)

* Project group is renamed to `ai.motiva/stepwise`
* [Renamed some core functions](https://github.com/Motiva-AI/stepwise/commit/b81abbd6e09ca2351a502363d557a5a75d713242) to suffix with `!` or `!!` to denote (non)-blocking
* Input/Output data are [(de)serialized as Transit](https://github.com/Motiva-AI/stepwise/commit/259fbb31a86a811aff3c10fbd250a9ae8d23d16f)
* `core/await-execution` is [made private](https://github.com/Motiva-AI/stepwise/commit/f718f2250e1c7799310e5509fe8d2894026abb86)

