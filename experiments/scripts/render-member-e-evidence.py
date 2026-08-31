"""Render reproducible Member-E PNG evidence figures from saved JSON reports."""

from __future__ import annotations

import argparse
import json
from datetime import datetime
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


COLORS = {
    "blue": "#2166AC",
    "orange": "#F58518",
    "green": "#1B9E77",
    "red": "#D9534F",
    "grid": "#D9DEE7",
    "text": "#1F2937",
    "muted": "#5F6B7A",
    "background": "#FFFFFF",
    "panel": "#F8FAFC",
}


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
    candidates = [
        Path("C:/Windows/Fonts/msyhbd.ttc" if bold else "C:/Windows/Fonts/msyh.ttc"),
        Path("C:/Windows/Fonts/simhei.ttf"),
        Path("C:/Windows/Fonts/arialbd.ttf" if bold else "C:/Windows/Fonts/arial.ttf"),
    ]
    for candidate in candidates:
        if candidate.exists():
            return ImageFont.truetype(str(candidate), size=size)
    return ImageFont.load_default()


FONTS = {
    "title": font(48, True),
    "subtitle": font(34, True),
    "body": font(26),
    "small": font(22),
    "label": font(24, True),
    "number": font(28, True),
}


def read_json(path: Path) -> dict:
    with path.open("r", encoding="utf-8-sig") as handle:
        return json.load(handle)


def centered(draw: ImageDraw.ImageDraw, xy: tuple[float, float], text: str, font_key: str, fill: str = COLORS["text"]) -> None:
    draw.multiline_text(xy, text, font=FONTS[font_key], fill=fill, anchor="mm", align="center", spacing=8)


def save(image: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path, format="PNG", optimize=True)


def render_hpa(report: dict, output: Path) -> None:
    width, height = 1800, 1000
    image = Image.new("RGB", (width, height), COLORS["background"])
    draw = ImageDraw.Draw(image)
    samples = report["Samples"]
    start = datetime.fromisoformat(samples[0]["Timestamp"])
    minutes = [(datetime.fromisoformat(item["Timestamp"]) - start).total_seconds() / 60 for item in samples]
    desired = [item["Desired"] for item in samples]
    ready = [item["Ready"] for item in samples]
    load_start = (datetime.fromisoformat(report["LoadStartedAt"]) - start).total_seconds() / 60
    load_end = (datetime.fromisoformat(report["LoadFinishedAt"]) - start).total_seconds() / 60

    draw.text((width / 2, 62), f"HPA 自动扩缩容时间线（{report['SourceRevision'][:7]}）", font=FONTS["title"], fill=COLORS["text"], anchor="mm")
    left, top, right, bottom = 150, 155, 1720, 790
    draw.rectangle((left, top, right, bottom), fill=COLORS["panel"], outline="#AEB8C5", width=2)
    max_minute = max(minutes)

    def x(value: float) -> float:
        return left + value / max_minute * (right - left)

    def y(value: float) -> float:
        return bottom - (value - 0.5) / 5 * (bottom - top)

    draw.rectangle((x(load_start), top, x(load_end), bottom), fill="#FDEBD2")
    for pod in range(1, 6):
        yy = y(pod)
        draw.line((left, yy, right, yy), fill=COLORS["grid"], width=2)
        draw.text((left - 35, yy), str(pod), font=FONTS["small"], fill=COLORS["muted"], anchor="rm")
    tick_count = 7
    for index in range(tick_count + 1):
        value = max_minute * index / tick_count
        xx = x(value)
        draw.line((xx, bottom, xx, bottom + 10), fill=COLORS["muted"], width=2)
        draw.text((xx, bottom + 20), f"{value:.1f}", font=FONTS["small"], fill=COLORS["muted"], anchor="ma")

    step_points: list[tuple[float, float]] = [(x(minutes[0]), y(desired[0]))]
    for index in range(1, len(minutes)):
        step_points.extend([(x(minutes[index]), y(desired[index - 1])), (x(minutes[index]), y(desired[index]))])
    draw.line(step_points, fill=COLORS["blue"], width=7, joint="curve")
    ready_points = [(x(m), y(v)) for m, v in zip(minutes, ready)]
    draw.line(ready_points, fill=COLORS["green"], width=4, joint="curve")
    for xx, yy in ready_points:
        draw.ellipse((xx - 5, yy - 5, xx + 5, yy + 5), fill=COLORS["green"])

    draw.text(((left + right) / 2, 875), "距第一次采样的时间（分钟）", font=FONTS["body"], fill=COLORS["text"], anchor="mm")
    draw.text((55, (top + bottom) / 2), "Pod 数量", font=FONTS["body"], fill=COLORS["text"], anchor="mm")
    legend_y = 120
    draw.line((1180, legend_y, 1245, legend_y), fill=COLORS["blue"], width=7)
    draw.text((1260, legend_y), "期望 Pod 数", font=FONTS["small"], fill=COLORS["text"], anchor="lm")
    draw.line((1435, legend_y, 1500, legend_y), fill=COLORS["green"], width=5)
    draw.text((1515, legend_y), "Ready Pod 数", font=FONTS["small"], fill=COLORS["text"], anchor="lm")
    draw.rectangle((830, legend_y - 14, 875, legend_y + 14), fill="#FDEBD2")
    draw.text((890, legend_y), "k6 负载窗口", font=FONTS["small"], fill=COLORS["text"], anchor="lm")
    centered(draw, (width / 2, 950), f"30 VU × 90 s；最高 5 个 Ready Pod；负载结束后自然回到 1；共 {len(samples)} 个样本", "small", COLORS["muted"])
    save(image, output)


def bar_panel(draw: ImageDraw.ImageDraw, bounds: tuple[int, int, int, int], title: str, unit: str, values: list[float], labels: list[str], colors: list[str]) -> None:
    left, top, right, bottom = bounds
    draw.rounded_rectangle(bounds, radius=18, fill=COLORS["panel"], outline="#D0D7E2", width=2)
    centered(draw, ((left + right) / 2, top + 50), title, "subtitle")
    plot_left, plot_top, plot_right, plot_bottom = left + 95, top + 105, right - 45, bottom - 90
    max_value = max(values) * 1.22
    for index in range(5):
        value = max_value * index / 4
        yy = plot_bottom - (plot_bottom - plot_top) * index / 4
        draw.line((plot_left, yy, plot_right, yy), fill=COLORS["grid"], width=2)
        draw.text((plot_left - 15, yy), f"{value:.0f}", font=FONTS["small"], fill=COLORS["muted"], anchor="rm")
    count = len(values)
    centers = [plot_left + (plot_right - plot_left) * (index + 1) / (count + 1) for index in range(count)]
    bar_width = 150 if count == 2 else 105
    for center_x, value, label, color in zip(centers, values, labels, colors):
        bar_top = plot_bottom - value / max_value * (plot_bottom - plot_top)
        draw.rectangle((center_x - bar_width / 2, bar_top, center_x + bar_width / 2, plot_bottom), fill=color)
        draw.text((center_x, bar_top - 12), f"{value:.2f}", font=FONTS["number"], fill=COLORS["text"], anchor="mb")
        draw.text((center_x, plot_bottom + 24), label, font=FONTS["body"], fill=COLORS["text"], anchor="ma")
    draw.text((left + 20, top + 52), unit, font=FONTS["small"], fill=COLORS["muted"], anchor="lm")


def render_performance(report: dict, output: Path) -> None:
    width, height = 2000, 1500
    image = Image.new("RGB", (width, height), COLORS["background"])
    draw = ImageDraw.Draw(image)
    mono = report["Variants"]["monolith"]
    micro = report["Variants"]["microservices"]
    labels = ["单体", "微服务"]
    colors = ["#4C78A8", COLORS["orange"]]
    centered(draw, (width / 2, 70), f"单体与微服务正式性能对比（{report['SourceRevision'][:7]}，各 3 轮）", "title")
    panels = [(70, 150, 965, 750), (1035, 150, 1930, 750), (70, 810, 965, 1410), (1035, 810, 1930, 1410)]
    bar_panel(draw, panels[0], "吞吐量", "QPS，越高越好", [mono["MeanRequestsPerSecond"], micro["MeanRequestsPerSecond"]], labels, colors)
    bar_panel(
        draw,
        panels[1],
        "平均响应时间与 P95",
        "ms，越低越好",
        [mono["MeanAverageMs"], micro["MeanAverageMs"], mono["MeanOfRunP95Ms"], micro["MeanOfRunP95Ms"]],
        ["单体\n平均", "微服务\n平均", "单体\nP95", "微服务\nP95"],
        ["#4C78A8", COLORS["orange"], "#72A0CF", "#FFAD5A"],
    )
    bar_panel(draw, panels[2], "Namespace 平均 CPU", "millicores", [mono["MeanNamespaceCpuMillicores"], micro["MeanNamespaceCpuMillicores"]], labels, colors)
    bar_panel(draw, panels[3], "Namespace 平均内存", "MiB", [mono["MeanNamespaceMemoryMi"], micro["MeanNamespaceMemoryMi"]], labels, colors)
    centered(draw, (width / 2, 1460), "同一机器、数据、30 VU、固定单副本和相同总资源预算；6 轮 HTTP/业务错误率均为 0%", "small", COLORS["muted"])
    save(image, output)


def render_fault(report: dict, output: Path) -> None:
    width, height = 2000, 850
    image = Image.new("RGB", (width, height), COLORS["background"])
    draw = ImageDraw.Draw(image)
    phases = [
        ("正常", "行程预览成功\n商品 1/1 Ready", "#59A14F"),
        ("注入故障", "商品副本 1 → 0\nEndpoint = 0", "#E15759"),
        ("故障响应", f"业务码 500\n{report['Expected']['Message']}\n{report['During']['Preview']['ElapsedMs']:.2f} ms", "#F28E2B"),
        ("隔离检查", "网关 / 用户 / 订单 / 内容\n四个服务均为 UP", "#4E79A7"),
        ("恢复", "商品恢复 1/1\n行程再次生成成功", "#59A14F"),
    ]
    centered(draw, (width / 2, 70), f"商品依赖故障与恢复实验（{report['SourceRevision'][:7]}，8/8 检查通过）", "title")
    centers = [220, 610, 1000, 1390, 1780]
    cy = 365
    for index, ((title, detail, color), center_x) in enumerate(zip(phases, centers)):
        centered(draw, (center_x, 205), title, "subtitle")
        draw.ellipse((center_x - 72, cy - 72, center_x + 72, cy + 72), fill=color, outline="#FFFFFF", width=5)
        centered(draw, (center_x, cy), str(index + 1), "subtitle", "#FFFFFF")
        centered(draw, (center_x, 560), detail, "body")
        if index < len(phases) - 1:
            start_x, end_x = center_x + 92, centers[index + 1] - 92
            draw.line((start_x, cy, end_x, cy), fill="#6B7280", width=5)
            draw.polygon([(end_x, cy), (end_x - 24, cy - 14), (end_x - 24, cy + 14)], fill="#6B7280")
    centered(draw, (width / 2, 780), "Deployment 与 PVC 身份未变；正常 / 故障 / 恢复日志已脱敏；Secret 和登录令牌未写入证据", "small", COLORS["muted"])
    save(image, output)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--hpa-report", type=Path, required=True)
    parser.add_argument("--performance-report", type=Path, required=True)
    parser.add_argument("--fault-report", type=Path, required=True)
    parser.add_argument("--output-dir", type=Path, required=True)
    args = parser.parse_args()
    args.output_dir.mkdir(parents=True, exist_ok=True)
    render_hpa(read_json(args.hpa_report), args.output_dir / "member-e-hpa-timeline-aece119.png")
    render_performance(read_json(args.performance_report), args.output_dir / "member-e-performance-comparison-aece119.png")
    render_fault(read_json(args.fault_report), args.output_dir / "member-e-dependency-fault-aece119.png")


if __name__ == "__main__":
    main()
