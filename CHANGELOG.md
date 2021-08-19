# Stepwise CHANGELOG

We use [Break Versioning][breakver]. The version numbers follow a `<major>.<minor>.<patch>` scheme with the following intent:

| Bump    | Intent                                                     |
| ------- | ---------------------------------------------------------- |
| `major` | Major breaking changes -- check the changelog for details. |
| `minor` | Minor breaking changes -- check the changelog for details. |
| `patch` | No breaking changes, ever!!                                |

`-SNAPSHOT` versions are preview versions for upcoming releases.

[breakver]: https://github.com/ptaoussanis/encore/blob/master/BREAK-VERSIONING.md

## 0.7.0 (2021-08-19)

* Added feature to [offload large payloads to S3](https://github.com/Motiva-AI/stepwise/pull/8)

## 0.6.0 (2021-06-26)

* Project group is renamed to `ai.motiva/stepwise`
* [Renamed some core functions](https://github.com/Motiva-AI/stepwise/commit/b81abbd6e09ca2351a502363d557a5a75d713242) to suffix with `!` or `!!` to denote (non)-blocking
* `core/await-execution` is [made private](https://github.com/Motiva-AI/stepwise/commit/f718f2250e1c7799310e5509fe8d2894026abb86)
* handler-interceptor passes the `input` [directly to handler-fn](https://github.com/Motiva-AI/stepwise/commit/1f92b0c6799d5db79eed69b6bc7d26df3ba96e77)
* interceptors implementation is [replaced with metosin/siepppari library](https://github.com/Motiva-AI/stepwise/pull/4). note that interceptor `:before` / `:after` are renamed to `:enter` / `:leave`, and data are tucked in `:request` and `:response` of the context.

