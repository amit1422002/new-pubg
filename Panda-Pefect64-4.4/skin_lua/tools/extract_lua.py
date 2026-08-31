import sys
from pathlib import Path
if sys.argv[1] == "-":
    text = sys.stdin.read()
else:
    text = Path(sys.argv[1]).read_text(encoding="utf-8", errors="replace")
s = text.index('R"lua(') + len('R"lua(')
e = text.rindex(')lua"')
lua = text[s:e]
out = Path(sys.argv[2])
out.write_text(lua, encoding="utf-8")
print(len(lua), "chars", lua.count("\n")+1, "lines")
