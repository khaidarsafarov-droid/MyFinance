#!/usr/bin/env python3
"""Fail if line coverage for critical classes or packages drops below floors.

Floors are integer percentages and are meant as regression gates, not targets.
They were set from a Kover XML report on this branch.

Usage:
  python3 scripts/check_kover_floors.py path/to/reportDebug.xml
"""
from __future__ import annotations

import sys
import xml.etree.ElementTree as ET
from dataclasses import dataclass

# Percent floors for LINE counters. Packages use the aggregated package counter.
# Floors were taken from a Kover debug report on this branch, then stepped
# down a few points so a one-line change does not trip CI.
PACKAGE_FLOORS = {
    "com/truckerload/domain/parser": 85,
    "com/truckerload/domain/import/parser": 70,
    "com/truckerload/data/backup": 35,
}

CLASS_FLOORS = {
    "com/truckerload/domain/report/PeriodReportBuilder": 95,
    "com/truckerload/utils/PeriodReportPdf": 95,
    "com/truckerload/domain/share/ShareImportDecider": 90,
}


@dataclass(frozen=True)
class Coverage:
    covered: int
    missed: int

    @property
    def total(self) -> int:
        return self.covered + self.missed

    @property
    def percent(self) -> float:
        if self.total == 0:
            return 100.0
        return 100.0 * self.covered / self.total


def line_counter(element: ET.Element) -> Coverage | None:
    for counter in element.findall("counter"):
        if counter.get("type") == "LINE":
            covered = int(counter.get("covered", "0"))
            missed = int(counter.get("missed", "0"))
            return Coverage(covered=covered, missed=missed)
    return None


def load_report(path: str) -> tuple[dict[str, Coverage], dict[str, Coverage]]:
    root = ET.parse(path).getroot()
    packages: dict[str, Coverage] = {}
    classes: dict[str, Coverage] = {}
    for package in root.findall("package"):
        name = package.get("name") or ""
        cov = line_counter(package)
        if cov is not None:
            packages[name] = cov
        for cls in package.findall("class"):
            cname = cls.get("name") or ""
            ccov = line_counter(cls)
            if ccov is not None:
                classes[cname] = ccov
    return packages, classes


def check_named(
    kind: str,
    floors: dict[str, int],
    found: dict[str, Coverage],
) -> list[str]:
    failures: list[str] = []
    for name, floor in floors.items():
        cov = found.get(name)
        if cov is None:
            failures.append(f"{kind} {name}: missing from Kover report")
            continue
        pct = cov.percent
        print(
            f"{kind} {name}: {pct:.1f}% "
            f"({cov.covered}/{cov.total} lines), floor {floor}%"
        )
        if pct + 1e-9 < floor:
            failures.append(
                f"{kind} {name}: {pct:.1f}% is below the {floor}% floor "
                f"({cov.covered}/{cov.total} lines)"
            )
    return failures


def main(argv: list[str]) -> int:
    if len(argv) != 2:
        print(__doc__.strip(), file=sys.stderr)
        return 2
    packages, classes = load_report(argv[1])
    failures = check_named("package", PACKAGE_FLOORS, packages)
    failures += check_named("class", CLASS_FLOORS, classes)
    if failures:
        print("Coverage floor failures:", file=sys.stderr)
        for item in failures:
            print(f"  {item}", file=sys.stderr)
        return 1
    print("OK: all coverage floors were met")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
