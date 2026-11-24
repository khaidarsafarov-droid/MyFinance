#!/usr/bin/env python3
"""Reject native libraries whose PT_LOAD segments align below 16 KB.

Play requires 16 KB page-size alignment for native code. This script inspects
every .so packed in an APK or AAB (or a loose .so) and fails if any PT_LOAD
segment has p_align < 16384.

Usage:
  python3 scripts/check_elf_alignment.py path/to/app-release.apk path/to/app-release.aab
"""
from __future__ import annotations

import struct
import sys
import zipfile
from pathlib import Path

MIN_ALIGN = 16384
PT_LOAD = 1
ELF_MAGIC = b"\x7fELF"
# Play's 16 KB page-size requirement applies to 64-bit ABIs. 32-bit libraries
# (armeabi-v7a, x86) may still ship 4 KB LOAD alignment and are not rejected.
SIXTEEN_KB_ABIS = frozenset({"arm64-v8a", "x86_64"})


def load_alignments(data: bytes) -> list[int]:
    if len(data) < 64 or data[:4] != ELF_MAGIC:
        raise ValueError("not an ELF file")
    little = data[5] == 1
    is64 = data[4] == 2
    endian = "<" if little else ">"
    if is64:
        e_phoff = struct.unpack_from(endian + "Q", data, 32)[0]
        e_phentsize, e_phnum = struct.unpack_from(endian + "HH", data, 54)
    else:
        e_phoff = struct.unpack_from(endian + "I", data, 28)[0]
        e_phentsize, e_phnum = struct.unpack_from(endian + "HH", data, 42)
    aligns: list[int] = []
    for i in range(e_phnum):
        off = e_phoff + i * e_phentsize
        if off + e_phentsize > len(data):
            raise ValueError("truncated program header")
        p_type = struct.unpack_from(endian + "I", data, off)[0]
        if p_type != PT_LOAD:
            continue
        if is64:
            p_align = struct.unpack_from(endian + "Q", data, off + 48)[0]
        else:
            p_align = struct.unpack_from(endian + "I", data, off + 28)[0]
        aligns.append(int(p_align))
    return aligns


def abi_from_zip_path(name: str) -> str | None:
    parts = name.replace("\\", "/").split("/")
    if "lib" not in parts:
        return None
    idx = parts.index("lib")
    if idx + 1 < len(parts):
        return parts[idx + 1]
    return None


def is_native_lib(name: str) -> bool:
    lower = name.replace("\\", "/")
    if not lower.endswith(".so"):
        return False
    if "/lib/" not in lower and not lower.startswith("lib/"):
        return False
    abi = abi_from_zip_path(lower)
    return abi in SIXTEEN_KB_ABIS


def collect_from_zip(path: Path) -> list[tuple[str, int]]:
    bad: list[tuple[str, int]] = []
    found = 0
    with zipfile.ZipFile(path) as zf:
        for info in zf.infolist():
            if not is_native_lib(info.filename):
                continue
            found += 1
            data = zf.read(info)
            for align in load_alignments(data):
                if align < MIN_ALIGN:
                    bad.append((f"{path.name}:{info.filename}", align))
    if found == 0:
        raise FileNotFoundError(
            f"no 64-bit .so files (arm64-v8a or x86_64) under lib/ in {path}"
        )
    return bad


def collect_from_so(path: Path) -> list[tuple[str, int]]:
    data = path.read_bytes()
    return [(path.name, align) for align in load_alignments(data) if align < MIN_ALIGN]


def main(argv: list[str]) -> int:
    if len(argv) < 2:
        print(__doc__.strip(), file=sys.stderr)
        return 2
    bad: list[tuple[str, int]] = []
    errors: list[str] = []
    for raw in argv[1:]:
        path = Path(raw)
        if not path.is_file():
            errors.append(f"missing file: {path}")
            continue
        try:
            suffix = path.suffix.lower()
            if suffix in {".apk", ".aab", ".zip", ".jar"}:
                bad.extend(collect_from_zip(path))
            elif suffix == ".so":
                bad.extend(collect_from_so(path))
            else:
                errors.append(f"unsupported file type: {path}")
        except Exception as exc:  # noqa: BLE001 — report and fail the gate
            errors.append(f"{path}: {exc}")
    if errors:
        for item in errors:
            print(f"ERROR: {item}", file=sys.stderr)
    if bad:
        print("Native libraries with PT_LOAD alignment below 16 KB:", file=sys.stderr)
        for name, align in bad:
            print(f"  {name}: p_align={align}", file=sys.stderr)
    if errors or bad:
        return 1
    print(f"OK: all PT_LOAD segments align to at least {MIN_ALIGN} bytes")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
