#!/usr/bin/env python3
"""Append a compact JUnit XML report to the GitHub Actions job summary."""

from __future__ import annotations

import argparse
import glob
import os
from pathlib import Path
import sys
import xml.etree.ElementTree as ET


def integer(value: str | None) -> int:
    try:
        return int(value or 0)
    except ValueError:
        return 0


def decimal(value: str | None) -> float:
    try:
        return float(value or 0)
    except ValueError:
        return 0.0


def tag_name(element: ET.Element) -> str:
    return element.tag.rsplit("}", 1)[-1]


def suite_elements(root: ET.Element) -> list[ET.Element]:
    if tag_name(root) == "testsuite":
        return [root]
    return [child for child in root if tag_name(child) == "testsuite"]


def root_totals(root: ET.Element, suites: list[ET.Element]) -> dict[str, float | int]:
    # JUnit aggregators normally expose totals on the root. Fall back to the
    # direct suites for reporters that omit aggregate attributes.
    source = [root] if root.get("tests") is not None else suites
    return {
        "tests": sum(integer(item.get("tests")) for item in source),
        "failures": sum(integer(item.get("failures")) for item in source),
        "errors": sum(integer(item.get("errors")) for item in source),
        "skipped": sum(integer(item.get("skipped")) for item in source),
        "time": sum(decimal(item.get("time")) for item in source),
    }


def failed_cases(root: ET.Element) -> list[str]:
    failures: list[str] = []
    for case in root.iter():
        if tag_name(case) != "testcase":
            continue
        failed = any(tag_name(child) in {"failure", "error"} for child in case)
        if not failed:
            continue
        class_name = case.get("classname")
        test_name = case.get("name", "Unnamed test")
        failures.append(f"{class_name}.{test_name}" if class_name else test_name)
    return failures


def escape_markdown(value: str) -> str:
    return value.replace("\\", "\\\\").replace("`", "\\`").replace("|", "\\|")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--title", required=True)
    parser.add_argument("--reports", action="append", required=True)
    parser.add_argument("--output")
    args = parser.parse_args()

    report_paths = sorted(
        {
            Path(path)
            for pattern in args.reports
            for path in glob.glob(pattern, recursive=True)
        }
    )

    totals: dict[str, float | int] = {
        "tests": 0,
        "failures": 0,
        "errors": 0,
        "skipped": 0,
        "time": 0.0,
    }
    suites = 0
    failures: list[str] = []
    unreadable: list[str] = []

    for report_path in report_paths:
        try:
            root = ET.parse(report_path).getroot()
        except (OSError, ET.ParseError):
            unreadable.append(str(report_path))
            continue

        report_suites = suite_elements(root)
        suites += len(report_suites)
        report_totals = root_totals(root, report_suites)
        for key in totals:
            totals[key] += report_totals[key]
        failures.extend(failed_cases(root))

    total = int(totals["tests"])
    failed = int(totals["failures"])
    errors = int(totals["errors"])
    skipped = int(totals["skipped"])
    passed = max(total - failed - errors - skipped, 0)
    icon = "✅" if report_paths and failed == 0 and errors == 0 and not unreadable else "❌"

    lines = [f"# {args.title}", "", "## Summary", ""]
    if not report_paths:
        lines.append("⚠️ No JUnit XML reports were generated.")
    else:
        lines.extend(
            [
                f"- Test suites: {icon} {suites} total",
                f"- Test results: ✅ {passed} passed · ❌ {failed} failed · "
                f"💥 {errors} errors · ⏭️ {skipped} skipped · {total} total",
                f"- Duration: {float(totals['time']):.2f}s",
            ]
        )

    if failures:
        lines.extend(["", "## Failed tests", ""])
        lines.extend(f"- `{escape_markdown(name)}`" for name in failures[:20])
        if len(failures) > 20:
            lines.append(f"- …and {len(failures) - 20} more")

    if unreadable:
        lines.extend(["", "## Report warnings", ""])
        lines.extend(f"- Could not parse `{escape_markdown(path)}`" for path in unreadable)

    lines.extend(["", "_Job summary generated from JUnit XML at run-time._", ""])
    summary = "\n".join(lines)
    output = args.output or os.environ.get("GITHUB_STEP_SUMMARY")
    if output:
        with Path(output).open("a", encoding="utf-8") as stream:
            stream.write(summary)
    else:
        if hasattr(sys.stdout, "reconfigure"):
            sys.stdout.reconfigure(encoding="utf-8")
        print(summary, end="")


if __name__ == "__main__":
    main()
