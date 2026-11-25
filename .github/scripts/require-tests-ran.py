#!/usr/bin/env python3
"""Fail unless a test class ran, was not skipped, and has at least N passing cases.

usage: require-tests-ran.py <results-dir> <fully.qualified.TestClass> <min-cases>

The SQLCipher tests cannot load the Android native library on a Linux JVM, so their
Robolectric twins skip on CI. The instrumented class covers them on an emulator; this
guard stops that coverage from silently turning into skips or an empty run.
"""
import pathlib
import sys
import xml.etree.ElementTree as ET


def main() -> int:
    root, cls, minimum = pathlib.Path(sys.argv[1]), sys.argv[2], int(sys.argv[3])
    ran = skipped = failed = 0
    for xml in root.rglob("*.xml"):
        try:
            tree = ET.parse(xml)
        except ET.ParseError:
            continue
        for case in tree.iter("testcase"):
            if case.get("classname") != cls:
                continue
            if case.find("skipped") is not None:
                skipped += 1
            elif case.find("failure") is not None or case.find("error") is not None:
                failed += 1
            else:
                ran += 1
    print(f"{cls}: passed={ran} skipped={skipped} failed={failed} (need >= {minimum} passed)")
    if skipped or failed or ran < minimum:
        print("::error::required tests did not all run and pass", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
