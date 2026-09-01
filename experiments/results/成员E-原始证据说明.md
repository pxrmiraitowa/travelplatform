# 成员 E 原始证据说明

## 1. 交付文件

- 文件：`成员E-最新版原始证据-aece119-20260831.zip`
- 业务源码版本：`aece11988e2364289625c0de2c75b18444c55b8d`
- 大小：34,677,553 字节
- ZIP 条目：79 个
- SHA-256：`279A7B1C9FA94820D9BD86A93D3C2CE788D1ED4737006ACE868CC36566EF4D41`

该文件是 2026-08-31 正式运行结束后生成的原始证据包，不是根据汇总报告重新拼造的数据。压缩包中的少量绝对路径是当次运行环境元数据，不影响结果和校验链；故障脚本没有持久化登录 Token，所有证据均标记 `SecretValuesIncluded: false`。

## 2. 内容对应关系

| 目录 | 原始内容 | 用于验证 |
| --- | --- | --- |
| `monolith-build-*` | Maven 完整日志、139 项测试明细、镜像构建记录和当次 JAR | 单体版本真实构建和测试 |
| `microservices-build-*` | 五个 Java 服务和前端的测试、打包及镜像日志 | 微服务版本真实构建和测试 |
| `comparison-*` | 固定源码、镜像、数据、资源预算和部署状态 | 性能对比条件一致 |
| `hpa-*` | k6 summary、Pod/CPU 时间线 CSV、HPA 事件和完整报告 | 1 到 5 再回到 1 的扩缩容过程 |
| `dependency-fault-*` | 故障前、故障中、恢复后的结果和脱敏服务日志 | 8 项故障隔离与恢复检查 |
| `formal-comparison-*` | 六轮 k6 summary、六轮资源 CSV、metadata 和汇总报告 | 单体与微服务各 3 次性能结果 |

## 3. 校验和解压

在仓库根目录运行：

```powershell
$archive = "experiments/results/成员E-最新版原始证据-aece119-20260831.zip"
(Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash
Expand-Archive -LiteralPath $archive -DestinationPath "artifacts/member-e-review-aece119"
```

第一条命令必须输出：

```text
279A7B1C9FA94820D9BD86A93D3C2CE788D1ED4737006ACE868CC36566EF4D41
```

## 4. 逐项复核

1. 打开 `hpa-*/hpa-report.json`，确认 `Status` 为 `Verified`、扩容被观察到且最后恢复单副本；用 `timeline.csv` 和 `hpa-events.json` 复核变化时间。
2. 打开 `dependency-fault-*/fault-experiment.json`，确认 `Status` 为 `Passed`、8 项检查全部通过，再对照 `before-*`、`during-*`、`after-*` 日志。
3. 打开 `formal-comparison-*/metadata.json`，确认六轮状态完整；分别检查六份 `run-*-summary.json` 和 `run-*-metrics.csv`。
4. 打开 `formal-comparison-*/comparison-report.json`，核对三轮平均值及文件哈希，确认其与 `latest-evidence-index.json` 和报告图一致。

## 5. 结果摘要

- HPA：30 个虚拟用户、90 秒负载，商品服务 Ready Pod 最高 5 个，停止负载后回到 1 个；HTTP 和业务错误率为 0%。
- 故障：商品服务缩为 0 后返回“商品服务暂不可用，请稍后重试”，其他四个服务保持健康；恢复后业务成功，8/8 检查通过。
- 性能：单体和微服务各 3 轮。单体平均 311.06 QPS、52.56 ms、P95 283.93 ms；微服务平均 324.37 QPS、49.33 ms、P95 260.69 ms。微服务平均 CPU 和内存更高。

这些结论只适用于本次源码、机器、数据和协议，不能推导为所有场景下微服务都更快。
