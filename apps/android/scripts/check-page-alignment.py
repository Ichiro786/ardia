#!/usr/bin/env python3
"""Usage: check-page-alignment.py APK [APK ...]

Fails when a native library in an APK can't load on a 16 KB page-size device:
a LOAD segment aligned below 16 KB, or an uncompressed library whose data
doesn't start on a 16 KB boundary inside the APK.
"""
import struct
import sys
import zipfile

PAGE = 16 * 1024
PT_LOAD = 1


def load_alignments(data):
    if data[:4] != b"\x7fELF":
        raise ValueError("not an ELF file")
    is64 = data[4] == 2
    order = "<" if data[5] == 1 else ">"
    if is64:
        phoff, = struct.unpack_from(order + "Q", data, 0x20)
        phentsize, phnum = struct.unpack_from(order + "HH", data, 0x36)
        entry = order + "IIQQQQQQ"
    else:
        phoff, = struct.unpack_from(order + "I", data, 0x1C)
        phentsize, phnum = struct.unpack_from(order + "HH", data, 0x2A)
        entry = order + "IIIIIIII"
    alignments = []
    for index in range(phnum):
        fields = struct.unpack_from(entry, data, phoff + index * phentsize)
        kind = fields[0]
        if kind == PT_LOAD:
            alignments.append(fields[7])
    return alignments


def data_offset(path, info):
    with open(path, "rb") as raw:
        raw.seek(info.header_offset)
        header = raw.read(30)
        name_length, extra_length = struct.unpack_from("<HH", header, 26)
        return info.header_offset + 30 + name_length + extra_length


def check(path):
    problems = []
    checked = 0
    with zipfile.ZipFile(path) as archive:
        for info in archive.infolist():
            if not (info.filename.startswith("lib/") and info.filename.endswith(".so")):
                continue
            checked += 1
            data = archive.read(info)
            low = [align for align in load_alignments(data) if align < PAGE]
            if low:
                problems.append(f"{info.filename}: LOAD segment aligned to {min(low)} bytes")
            if info.compress_type == zipfile.ZIP_STORED and data_offset(path, info) % PAGE:
                problems.append(f"{info.filename}: stored at an offset that isn't 16 KB aligned")
    return checked, problems


def main(paths):
    if not paths:
        print(__doc__.strip())
        return 2
    failed = False
    for path in paths:
        checked, problems = check(path)
        if checked == 0:
            print(f"{path}: no native libraries found")
            failed = True
        for problem in problems:
            print(f"{path}: {problem}")
        if problems:
            failed = True
        else:
            print(f"{path}: {checked} native libraries are 16 KB aligned")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
