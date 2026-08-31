# 成员 E 实验结果

原始结果默认不进入 Git；给组员或答辩提交时需要同时携带独立证据压缩包。完成事务、最终结果和结果分析见[成员 E 第一、二阶段工作总结](../../docs/成员E-第一二阶段完成报告-20260831.md)。

## 当前最终结果（2026-08-31）

- 源码：`aece11988e2364289625c0de2c75b18444c55b8d`，与远程 `codex/microservices-ci-integration` 一致。
- 构建：单体 139/139；微服务 Java 13/13；前端 6 个文件、16/16 项测试及生产构建通过。
- HPA：`artifacts/member-e/aece119/hpa-20260831-150941-2bb28d/`，真实观察 1→5→4→3→2→1。
- 依赖故障：`artifacts/member-e/aece119/dependency-fault-20260831-152825-244f7932/`，8/8 检查通过，含正常、故障、恢复日志。
- 正式性能：`artifacts/member-e/aece119/formal-comparison-20260831-151647-bc02e426/`，单体和微服务各 3 轮，6 轮 HTTP/业务错误率均为 0%。
- 三轮平均：单体 311.06 QPS / 52.56 ms / P95 283.93 ms；微服务 324.37 QPS / 49.33 ms / P95 260.69 ms。
- 平均资源：单体 1121.03m CPU / 794.40Mi；微服务 1302.50m CPU / 1272.73Mi。

`latest-evidence-index.json` 是可提交的机器可读索引。旧日期文档和旧 `artifacts/member-e/a69fa53`、`5bbc3bb` 目录只保留历史定位价值，不能当作当前最新版结果。
