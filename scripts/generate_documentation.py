#!/usr/bin/env python3
"""Generate polished, source-driven README.html and docs/api.html from comments.

Requires Python 3.10+ and a JDK (javac/java). No project dependencies, Maven build,
network requests, Docker containers, or scanner executions are needed.
"""
from __future__ import annotations
import base64
import html
from pathlib import Path
import re
import subprocess
import tempfile
from collections import defaultdict
from xml.etree import ElementTree

ROOT = Path(__file__).resolve().parents[1]


def java_entries():
    """Use javac's Java syntax parser, not fragile signature regexes, to find methods."""
    with tempfile.TemporaryDirectory(prefix="owasp-javadoc-") as tmp:
        subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp,
                        str(ROOT / "scripts/MethodExtractor.java")], check=True)
        result = subprocess.run(["java", "-cp", tmp, "MethodExtractor", str(ROOT)],
                                check=True, capture_output=True, text=True)
    entries = []
    for line in result.stdout.splitlines():
        values = line.split("\t")
        if len(values) != 9:
            raise ValueError("Unexpected Java parser output: " + line[:80])
        path, owner, name, ret, params = [base64.b64decode(v).decode() for v in values[:5]]
        offset = int(values[5])
        source_file = Path(path)
        source = source_file.read_text(encoding="utf-8")
        docs = list(re.finditer(r"/\*\*[\s\S]*?\*/", source[:offset]))
        if not docs:
            raise ValueError(f"Missing Javadoc for {source_file}:{name}")
        doc = docs[-1]
        intervening = source[doc.end():offset]
        if len(intervening) > 450 or re.search(r"[;{}]", intervening):
            raise ValueError(f"Javadoc not attached to {source_file}:{name}")
        parsed = parse_doc(doc.group())
        if not parsed["summary"]:
            raise ValueError(f"Empty Javadoc for {source_file}:{name}")
        rel = source_file.relative_to(ROOT).as_posix()
        package_match = re.search(r"(?m)^\s*package\s+([\w.]+);", source)
        package = package_match.group(1) if package_match else "(default package)"
        signature = f'{owner}.{owner.split(".")[-1] if name == "<init>" else name}({params.replace(";", ", ")})'
        if ret:signature += " : " + ret
        entries.append({"path": rel, "package": package, "owner": owner,
                        "name": name, "returnType": ret, "params": params,
                        "signature": signature, "line": source.count("\n", 0, offset) + 1, "doc": parsed})
    entries.sort(key=lambda v: (v["path"].lower(), v["owner"], v["name"], v["params"]))
    return entries


def parse_doc(raw):
    """Parse a Javadoc/JSDoc block into its prose description and structured tags."""
    raw = raw.removeprefix("/**").removesuffix("*/")
    lines = [re.sub(r"^\s*\* ?", "", line).strip() for line in raw.splitlines()]
    description, tags = [], []
    for line in lines:
        if not line:continue
        if line.startswith("@"):
            kind, _, content = line.partition(" ")
            tags.append((kind, content))
        elif tags:
            kind, content = tags[-1];tags[-1] = (kind, content + " " + line)
        else:description.append(line)
    summary = next((re.sub(r"<[^>]+>", "", line) for line in description if line and not line.startswith("<p>")), "")
    if not summary and description:summary=re.sub(r"<[^>]+>", "", description[0])
    return {"summary": summary, "description": description, "tags": tags}


def js_entries():
    """Read the source-adjacent JSDoc for each named JavaScript function."""
    paths = [ROOT / "src/test/java/utilities/reporting/spa/assets/app.js",
             ROOT / "src/test/java/utilities/reporting/assets/security-assessment.js",
             ROOT / "src/test/resources/presentation/overlay.js"]
    pattern = re.compile(r"(?m)^\s*(?:async\s+)?function\s+([\w$]+)\s*\(([^)]*)\)"
                         r"|^\s*const\s+([\w$]+)\s*=\s*(?:\(([^)]*)\)|([\w$]+))\s*=>")
    items = []
    for path in paths:
        source = path.read_text(encoding="utf-8")
        for match in pattern.finditer(source):
            name = match.group(1) or match.group(3)
            params = match.group(2) or match.group(4) or match.group(5) or ""
            before = source[:match.start()]
            docs = list(re.finditer(r"/\*\*[\s\S]*?\*/", before))
            if not docs or len(before)-docs[-1].end()>150:
                raise ValueError(f"Missing JSDoc: {path}:{name}")
            items.append({"path":path.relative_to(ROOT).as_posix(),
                          "name":name,"signature":f"{name}({params})",
                          "line": source.count("\n", 0, match.start()) + 1,
                          "doc":parse_doc(docs[-1].group())})
    return items


def strip_java_markup(value):
    """Render Java/JSDoc inline code without trusting arbitrary HTML from a comment."""
    escaped = html.escape(value)
    escaped = re.sub(r"\{@code (.*?)\}",lambda m:'<code>'+m.group(1)+'</code>',escaped)
    escaped = escaped.replace("&lt;p&gt;", "<p>").replace("&lt;/p&gt;", "</p>")
    return escaped


def anchor(entry, prefix="java"):
    """Create a stable HTML fragment identifier for a documented declaration."""
    import hashlib
    token = entry["path"]+"/"+entry.get("owner","")+"/"+entry["signature"]
    return prefix+"-"+hashlib.sha256(token.encode()).hexdigest()[:15]


def class_doc(path):
    """Return the Javadoc summary attached to the primary type in a Java source file."""
    source = (ROOT / path).read_text(encoding="utf-8")
    name = Path(path).stem
    match = re.search(r"\b(?:class|interface|enum|record)\s+"+re.escape(name)+r"\b",source)
    if not match:return name
    comments = list(re.finditer(r"/\*\*[\s\S]*?\*/",source[:match.start()]))
    if not comments:return name
    last = comments[-1]
    if len(source[last.end():match.start()])>250:return name
    return parse_doc(last.group())["summary"] or name



def section_for_source(path):
    """Group documented source files by their project responsibility."""
    if path.startswith("scripts/"): return "Documentation tooling"
    if path.startswith("src/main/"): return "Application entry point"
    if "/automation/" in path: return "Browser and ZAP automation"
    if "/pom/Dvwa/" in path: return "DVWA page objects"
    if "/pom/JuiceShop/" in path: return "Juice Shop page objects"
    if "/tests/Dvwa/" in path: return "DVWA test suites"
    if "/tests/JuiceShop/" in path: return "Juice Shop test suites"
    if "/tests/ZapScan/" in path: return "Dedicated active scanners"
    if "/utilities/Docker/" in path: return "Container environments"
    if "/utilities/reporting/" in path: return "Reporting and SPA backend"
    if "/utilities/" in path: return "Shared test utilities"
    return "Other source files"


def method_tags_html(tags):
    """Format parsed documentation tags as readable terms and explanations."""
    if not tags: return ""
    result = ['<dl class="method-doc-tags">']
    for kind, description in tags:
        heading = kind
        if kind in ("@param", "@throws", "@exception"):
            symbol, sep, remainder = description.partition(" ")
            if sep:
                heading += " " + symbol
                description = remainder
        result.append('<div><dt>' + html.escape(heading) + '</dt><dd>' +
                      strip_java_markup(description) + '</dd></div>')
    result.append('</dl>')
    return ''.join(result)


def methods_section(entries, js, link_prefix=""):
    """Generate the expandable, searchable method reference from source comments."""
    categories = [
        "Application entry point", "Browser and ZAP automation",
        "DVWA page objects", "Juice Shop page objects", "DVWA test suites",
        "Juice Shop test suites", "Dedicated active scanners", "Container environments",
        "Shared test utilities", "Reporting and SPA backend", "Documentation tooling",
        "Other source files", "JavaScript and presentation"
    ]
    grouped = defaultdict(lambda: defaultdict(list))
    for entry in entries:
        grouped[section_for_source(entry["path"])][entry["path"]].append((entry, "java"))
    for entry in js:
        grouped["JavaScript and presentation"][entry["path"]].append((entry, "js"))

    total = len(entries) + len(js)
    pieces = [
        '<section class="doc-section" id="method-reference">',
        '<div class="section-kicker">10 / Source API index</div>',
        '<h2>Searchable method reference</h2>',
        '<p class="section-intro">Generated from the Javadoc and JSDoc directly above declarations in the project. '
        'The source comments are authoritative: edit those comments and rerun '
        '<code>python3 scripts/generate_documentation.py</code> to update this guide.</p>',
        '<div class="reference-intro">',
        '<input type="search" id="methodSearch" class="ref-search" '
        'aria-label="Search methods, classes, and descriptions" '
        'placeholder="Search method, class, file, or purpose…">',
        f'<span id="methodCount" class="ref-count">{total} / {total} declarations</span>',
        '<button type="button" class="action" id="clearSearch" '
        'style="border:0;cursor:pointer">Clear</button>',
        '<button type="button" id="expandAll" '
        'style="padding:9px;border:1px solid #c8d7e4;border-radius:8px;background:white;cursor:pointer">Expand all</button>',
        '<button type="button" id="collapseAll" '
        'style="padding:9px;border:1px solid #c8d7e4;border-radius:8px;background:white;cursor:pointer">Collapse all</button>',
        '</div>'
    ]
    for category in categories:
        files = grouped.get(category)
        if not files: continue
        number = sum(len(x) for x in files.values())
        pieces.append('<div class="ref-section"><div class="ref-section-heading">'
                      f'<h3>{html.escape(category)}</h3><span>{number} declarations</span></div>')
        for path, declarations in sorted(files.items()):
            import hashlib
            fid = "file-" + hashlib.sha256(path.encode()).hexdigest()[:12]
            filename = Path(path).name
            source_description = class_doc(path) if path.endswith(".java") else "JavaScript source functions"
            pieces.append(f'<details class="source-group" id="{fid}">'
                          '<summary>'
                          f'<span class="source-title">{html.escape(filename)}</span>'
                          f'<span class="source-meta">{html.escape(path)} · {len(declarations)} declarations</span>'
                          '<span class="chevron" aria-hidden="true">⌄</span>'
                          '</summary><div class="method-stack">')
            if path.endswith(".java"):
                pieces.append(f'<p class="method-source-info">{strip_java_markup(source_description)}</p>')
            for entry, language in declarations:
                doc = entry["doc"]
                sig = entry["signature"]
                description = doc["summary"]
                all_doc = " ".join(doc["description"]) + " "+" ".join(
                    tag+" "+value for tag,value in doc["tags"])
                searchable = " ".join([path, entry.get("owner", ""), entry["name"],
                                       sig, all_doc]).lower()
                label = "JavaScript function" if language == "js" else (
                    "Constructor" if entry["name"] == "<init>" else "Java method")
                pieces.append(f'<article class="method-entry" id="{anchor(entry, language)}" '
                              f'data-search="{html.escape(searchable,quote=True)}">'
                              '<div class="method-header">'
                              f'<code>{html.escape(sig)}</code>'
                              f'<span class="method-type">{label}</span></div>'
                              f'<p class="method-desc">{strip_java_markup(description)}</p>')
                for line in doc["description"][1:]:
                    pieces.append(f'<p class="method-desc-more">{strip_java_markup(line)}</p>')
                pieces.append(method_tags_html(doc["tags"]))
                line = entry["line"]
                pieces.append('<div class="method-foot">'
                              f'<span>{html.escape(entry.get("owner", Path(path).stem))}</span>'
                              f'<a href="{html.escape(link_prefix+path,quote=True)}" title="Open source file">'
                              f'{html.escape(path)} · line {line}</a></div></article>')
            pieces.append('</div></details>')
        pieces.append('</div>')
    pieces.append('</section>')
    return "\n".join(pieces)


def settings_from_source():
    """Read the user-guide's displayed scanner limits from the current settings file."""
    settings = {}
    for raw in (ROOT / "src/test/resources/config.properties").read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith(("#", "!")) or "=" not in line: continue
        key, value = line.split("=", 1)
        settings[key.strip()] = value.strip()
    return settings


def styled_readme_html(entries, js):
    """Apply the supplied polished layout to the current source-generated reference."""
    template = (ROOT / "scripts/templates/readme_template.html").read_text(encoding="utf-8")
    settings = settings_from_source()
    juice = settings.get("zap.active.maxMinutes", "Not configured")
    dvwa = settings.get("zap.dvwa.active.maxMinutes", "Not configured")
    limit_label = (f"{juice} minutes per application" if juice == dvwa else
                   f"{juice} minutes for Juice Shop and {dvwa} for DVWA")
    replacements = {
        "JAVA_SOURCE_FILES": str(len({entry["path"] for entry in entries})),
        "JAVA_DECLARATIONS": str(len(entries)),
        "JS_FUNCTIONS": str(len(js)),
        "JUICE_SCAN_MINUTES": html.escape(juice),
        "DVWA_SCAN_MINUTES": html.escape(dvwa),
        "SCAN_LIMIT_LABEL": html.escape(limit_label),
        "METHOD_REFERENCE": methods_section(entries, js),
    }
    for name, value in replacements.items():
        token = "{{" + name + "}}"
        if token not in template: raise ValueError(f"Missing template marker: {token}")
        template = template.replace(token, value)
    if re.search(r"\{\{[A-Z_]+\}\}", template):
        raise ValueError("Unfilled documentation template markers")
    return template


def styled_api_html(entries, js):
    """Give the independent API reference the same polished appearance."""
    template = (ROOT / "scripts/templates/readme_template.html").read_text(encoding="utf-8")
    style = re.search(r"<style>([\s\S]*?)</style>", template)
    script = re.search(r"<script>([\s\S]*?)</script>", template)
    if not style or not script: raise ValueError("Missing shared docs styles or search behavior")
    content = (
        '<header class="appbar"><div class="brand">'
        '<div class="brand-mark" aria-hidden="true">◈</div><div>'
        '<strong>OWASP Security Automation</strong><small>Source API reference</small>'
        '</div></div><div class="appbar-right"><a href="../README.html">← User guide</a>'
        '</div></header><div class="layout"><nav class="sidebar" aria-label="Documentation">'
        '<div class="side-label">Navigation</div><a href="../README.html">User guide</a>'
        '<a href="#method-reference">Searchable source reference</a></nav>'
        '<main class="main"><div class="hero"><div class="eyebrow">Developer reference</div>'
        '<h1>Explore the documented source.</h1><p>Browse every documented Java declaration '
        'and named JavaScript function. All content comes directly from the source comments.</p>'
        '<div class="hero-links"><a class="action" href="../README.html">← Return to user guide</a>'
        '</div></div>' + methods_section(entries, js, link_prefix="../") +
        '<footer class="footer">Generated directly from the project source. '
        'Edit Javadoc and JSDoc, then run the documentation generator.</footer>'
        '</main></div>'
    )
    return ('<!doctype html><html lang="en"><head><meta charset="utf-8">'
            '<meta name="viewport" content="width=device-width, initial-scale=1">'
            '<title>OWASP Security Automation — Source API reference</title>'
            '<style>'+style.group(1)+'</style></head><body>'+content+
            '<script>'+script.group(1)+'</script></body></html>')


def main():
    """Regenerate HTML documentation from source comments without touching README.md."""
    entries=java_entries();js=js_entries()
    if not entries or not js:raise SystemExit('Source documentation not found; nothing generated')
    (ROOT/'docs').mkdir(exist_ok=True)
    (ROOT/'docs/api.html').write_text(styled_api_html(entries,js),encoding='utf-8')
    (ROOT/'README.html').write_text(styled_readme_html(entries,js),encoding='utf-8')
    print(f'Updated README.html and docs/api.html from {len(entries)} Java declarations in {len(set(e["path"] for e in entries))} files and {len(js)} JavaScript functions.')

if __name__=='__main__':main()
