"""Prints a text file as GitHub workflow annotations (readable through the checks API).
Usage: annotate.py <file> <title> [level] [lines-per-annotation] [max-annotations]"""
import sys

path, title = sys.argv[1], sys.argv[2]
level = sys.argv[3] if len(sys.argv) > 3 else "warning"
per = int(sys.argv[4]) if len(sys.argv) > 4 else 40
limit = int(sys.argv[5]) if len(sys.argv) > 5 else 10
lines = open(path, encoding="utf-8", errors="replace").read().splitlines() or ["(empty)"]
for i in range(0, min(len(lines), per * limit), per):
    msg = "%0A".join(line.replace("%", "%25").replace("\r", "")[:300] for line in lines[i:i + per])
    print(f"::{level} title={title}::{msg}")
