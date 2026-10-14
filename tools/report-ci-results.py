#!/usr/bin/env python3
"""Expose concise Gradle XML results through check annotations; never masks the build exit code."""
from pathlib import Path
import xml.etree.ElementTree as ET


def annotate(level, message):
    escaped = message.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")
    print(f"::{level}::{escaped}")


lint = Path("app/build/reports/lint-results-debug.xml")
if lint.exists():
    issues = ET.parse(lint).getroot().findall("issue")
    errors = [issue for issue in issues if issue.get("severity") in ("Error", "Fatal")]
    annotate("notice", f"Lint report: {len(errors)} errors, {len(issues) - len(errors)} other findings")
    for issue in errors:
        location = issue.find("location")
        where = "" if location is None else f"{location.get('file')}:{location.get('line')}"
        annotate("error", f"{issue.get('id')} {where}: {issue.get('message')}")

reports = list(Path("app/build/test-results/testDebugUnitTest").glob("TEST-*.xml"))
if reports:
    totals = {key: 0 for key in ("tests", "failures", "errors", "skipped")}
    for report in reports:
        suite = ET.parse(report).getroot()
        for key in totals:
            totals[key] += int(suite.get(key, "0"))
    annotate("notice", f"Gradle JVM test results ({len(reports)} suites): {totals}")
