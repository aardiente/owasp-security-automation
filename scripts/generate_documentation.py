
#!/usr/bin/env python3
"""Generate polished HTML documentation directly from source comments.

Reads Java Javadoc and JavaScript JSDoc to generate:
    docs/README.html
    docs/api.html

The manually maintained docs/README.md is never modified.

Requires Python 3.10+ and a JDK with javac/java.
No Maven build, Docker containers, or security scans are required.
"""

from __future__ import annotations

import base64
import html
from pathlib import Path
import re
import subprocess
import tempfile
from collections import defaultdict

ROOT = Path(__file__).resolve().parents[1]


def java_entries():
    """Extract documented Java methods using the JDK syntax parser."""
    with tempfile.TemporaryDirectory(prefix="owasp-javadoc-") as tmp:
        subprocess.run(
            [
                "javac", "-encoding", "UTF-8", "-d", tmp,
                str(ROOT / "scripts/MethodExtractor.java")
            ],
            check=True
        )

        result = subprocess.run(
            ["java", "-cp", tmp, "MethodExtractor", str(ROOT)],
            check=True,
            capture_output=True,
            text=True
        )

    entries = []

    for line in result.stdout.splitlines():
        values = line.split("\t")

        if len(values) != 9:
            raise ValueError("Unexpected Java parser output: " + line[:80])

        path, owner, name, ret, params = [
            base64.b64decode(v).decode()
            for v in values[:5]
        ]

        offset = int(values[5])
        source_file = Path(path)
        source = source_file.read_text(encoding="utf-8")

        attached = base64.b64decode(values[8]).decode()
        if not attached.strip():
            raise ValueError(f"Missing Javadoc for {source_file}:{name}")
        parsed = parse_doc(attached)

        if not parsed["summary"]:
            raise ValueError(
                f"Empty Javadoc for {source_file}:{name}"
            )

        rel = source_file.relative_to(ROOT).as_posix()

        package_match = re.search(
            r"(?m)^\s*package\s+([\w.]+);",
            source
        )

        package = (
            package_match.group(1)
            if package_match
            else "(default package)"
        )

        method_name = (
            owner.split(".")[-1]
            if name == "<init>"
            else name
        )

        signature = (
            f'{owner}.{method_name}'
            f'({params.replace(";", ", ")})'
        )

        if ret:
            signature += " : " + ret

        entries.append({
            "path": rel,
            "package": package,
            "owner": owner,
            "name": name,
            "returnType": ret,
            "params": params,
            "signature": signature,
            "line": source.count("\n", 0, offset) + 1,
            "doc": parsed
        })

    entries.sort(
        key=lambda v: (
            v["path"].lower(),
            v["owner"],
            v["name"],
            v["params"]
        )
    )

    return entries


def parse_doc(raw):
    """Parse Javadoc/JSDoc descriptions and structured documentation tags."""
    raw = raw.removeprefix("/**").removesuffix("*/")

    lines = [
        re.sub(r"^\s*\* ?", "", line).strip()
        for line in raw.splitlines()
    ]

    description = []
    tags = []

    for line in lines:
        if not line:
            continue

        if line.startswith("@"):
            kind, _, content = line.partition(" ")
            tags.append((kind, content))
        elif tags:
            kind, content = tags[-1]
            tags[-1] = (kind, content + " " + line)
        else:
            description.append(line)

    summary = next(
        (
            re.sub(r"<[^>]+>", "", line)
            for line in description
            if line and not line.startswith("<p>")
        ),
        ""
    )

    if not summary and description:
        summary = re.sub(r"<[^>]+>", "", description[0])

    return {
        "summary": summary,
        "description": description,
        "tags": tags
    }


def js_entries():
    """Extract named JavaScript functions and their adjacent JSDoc."""
    paths = [
        ROOT / "src/test/java/utilities/reporting/spa/assets/app.js",
        ROOT / "src/test/java/utilities/reporting/assets/security-assessment.js",
        ROOT / "src/test/resources/presentation/overlay.js"
    ]

    pattern = re.compile(
        r"(?m)^\s*(?:async\s+)?function\s+([\w$]+)\s*\(([^)]*)\)"
        r"|^\s*const\s+([\w$]+)\s*=\s*"
        r"(?:\(([^)]*)\)|([\w$]+))\s*=>"
    )

    items = []

    for path in paths:
        source = path.read_text(encoding="utf-8")

        for match in pattern.finditer(source):
            name = match.group(1) or match.group(3)

            params = (
                match.group(2)
                or match.group(4)
                or match.group(5)
                or ""
            )

            before = source[:match.start()]

            docs = list(re.finditer(
                r"/\*\*[\s\S]*?\*/",
                before
            ))

            if not docs or len(before) - docs[-1].end() > 150:
                raise ValueError(
                    f"Missing JSDoc: {path}:{name}"
                )

            items.append({
                "path": path.relative_to(ROOT).as_posix(),
                "name": name,
                "signature": f"{name}({params})",
                "params": params,
                "line": source.count("\n", 0, match.start()) + 1,
                "doc": parse_doc(docs[-1].group())
            })

    return items


def strip_java_markup(value):
    """Safely render supported Javadoc formatting as HTML."""
    escaped = html.escape(value)

    escaped = re.sub(
        r"\{@code (.*?)\}",
        lambda m: "<code>" + m.group(1) + "</code>",
        escaped
    )

    escaped = escaped.replace("&lt;p&gt;", "<p>")
    escaped = escaped.replace("&lt;/p&gt;", "</p>")

    return escaped


def anchor(entry, prefix="java"):
    """Create a stable HTML anchor for a documented method."""
    import hashlib

    token = (
        entry["path"]
        + "/"
        + entry.get("owner", "")
        + "/"
        + entry["signature"]
    )

    return (
        prefix
        + "-"
        + hashlib.sha256(token.encode()).hexdigest()[:15]
    )


def class_doc(path):
    """Find the Javadoc description of a Java source file's primary type."""
    source = (ROOT / path).read_text(encoding="utf-8")
    name = Path(path).stem

    match = re.search(
        r"\b(?:class|interface|enum|record)\s+"
        + re.escape(name)
        + r"\b",
        source
    )

    if not match:
        return {"summary": name, "description": [], "tags": []}

    comments = list(re.finditer(
        r"/\*\*[\s\S]*?\*/",
        source[:match.start()]
    ))

    if not comments:
        return {"summary": name, "description": [], "tags": []}

    last = comments[-1]

    if len(source[last.end():match.start()]) > 250:
        return {"summary": name, "description": [], "tags": []}

    return parse_doc(last.group())


def section_for_source(path):
    """Group source files by their top-level package for the documentation cards."""
    if path.startswith("scripts/"):
        return "Documentation tooling"
    if path.startswith("src/main/"):
        return "Application"
    packages = {
        "automation": "Automation",
        "pom": "POM · Page objects",
        "tests": "Tests",
        "utilities": "Utilities",
    }
    for package, label in packages.items():
        if path.startswith(f"src/test/java/{package}/"):
            return label
    return "Other source files"


def section_anchor(category):
    """Create a readable, stable link to a package card."""
    return "package-" + re.sub(r"[^a-z0-9]+", "-", category.lower()).strip("-")


def parameter_declarations(entry, language):
    """Return parameter names and declared types in signature order."""
    raw = entry.get("params", "").strip()
    if not raw:
        return []
    if language == "java":
        # MethodExtractor separates Java declarations with semicolons, so
        # commas inside generic types are not mistaken for argument separators.
        return [(name, declared_type) for declared_type, name in
                (part.strip().rsplit(None, 1) for part in raw.split(";"))]
    # The existing JavaScript extractor supports simple named arguments.
    return [(part.split("=", 1)[0].strip().removeprefix("..."), None)
            for part in raw.split(",")]


def split_parameter_tag(text):
    """Separate an optional JSDoc type, parameter name and description."""
    match = re.match(r"^(?:\{([^}]*)\}\s+)?(\S+)(?:\s+(.*))?$", text)
    if not match:
        return text, None, ""
    declared_type, name, description = match.groups()
    name = name.removeprefix("...")
    if name.startswith("[") and name.endswith("]"):
        name = name[1:-1].split("=", 1)[0]
    return name, declared_type, description or ""


def method_tags_html(tags, entry=None, language=None):
    """Render uniform definition rows with names and types beside descriptions.

    Java types are read from declarations and JavaScript types from JSDoc.
    Missing descriptions remain explicit, including for undocumented arguments.
    """
    groups = defaultdict(list)
    for kind, description in tags:
        if kind in ("@param", "@arg", "@argument"):
            group = "Parameters"
        elif kind in ("@return", "@returns"):
            group = "Return value"
        elif kind in ("@throws", "@exception"):
            group = "Exceptions"
        else:
            group = "Additional documentation"
        groups[group].append((kind, description))

    def row(heading, description, declared_type=None):
        """Build a definition row with an optional inline type label."""
        label = html.escape(heading)
        if declared_type is not None:
            label += ' : <span class="definition-type">' + html.escape(declared_type) + '</span>'
        return '<div><dt>' + label + '</dt><dd>' + strip_java_markup(description) + '</dd></div>'

    result = []
    for group in ("Parameters", "Return value", "Exceptions", "Additional documentation"):
        mandatory = entry is not None and group in ("Parameters", "Return value")
        if not groups[group] and not mandatory:
            continue
        result.append('<section class="definition-section"><h5>' + group + '</h5>'
                      '<dl class="method-doc-tags">')
        if mandatory and group == "Parameters":
            declared = parameter_declarations(entry, language)
            documented = [split_parameter_tag(text) for _, text in groups[group]]
            if not declared:
                result.append(row("None", "Takes no arguments."))
            used = set()
            for name, source_type in declared:
                matches = [(i, typ, desc) for i, (key, typ, desc) in enumerate(documented) if key == name]
                if matches:
                    for i, doc_type, description in matches:
                        used.add(i)
                        result.append(row(name, description or "No parameter description is documented.",
                                          source_type or doc_type or "Not documented"))
                else:
                    result.append(row(name, "No parameter description is documented.",
                                      source_type or "Not documented"))
            # Preserve type-parameter and additional JSDoc entries without inventing types.
            for i, (name, doc_type, description) in enumerate(documented):
                if i not in used:
                    label_type = "Type parameter" if name.startswith("<") and name.endswith(">") else (doc_type or "Not documented")
                    result.append(row(name, description, label_type))
        elif mandatory and group == "Return value":
            constructor = language == "java" and entry["name"] == "<init>"
            if constructor:
                return_type = entry["owner"]
                fallback = f"An instance of the {return_type} class."
            elif language == "java":
                return_type = entry.get("returnType") or "Not declared"
                fallback = ("Does not return a value." if return_type == "void" else
                            "No return value description is provided in the source comment.")
            else:
                declared = [re.match(r"^\{([^}]+)\}(?:\s|$)", text) for _, text in groups[group]]
                return_type = " / ".join(dict.fromkeys(m.group(1) for m in declared if m)) or "Not documented (JavaScript)"
                fallback = "No return value is documented in the source comment."
            descriptions = [re.sub(r"^\{[^}]+\}\s*", "", text) if language == "js" else text
                            for _, text in groups[group]]
            result.append(row("Returns", " ".join(filter(None, descriptions)) or fallback, return_type))
        else:
            for kind, description in groups[group]:
                heading = kind
                if kind in ("@param", "@arg", "@argument", "@throws", "@exception"):
                    match = re.match(r"^(\{[^}]*\}\s+)?(\S+)(?:\s+(.*))?$", description)
                    if match:
                        heading = (match.group(1) or "") + match.group(2)
                        description = match.group(3) or ""
                elif kind in ("@return", "@returns"):
                    heading = "Returns"
                result.append(row(heading, description))
        result.append('</dl></section>')
    return "".join(result)


def description_html(doc):
    """Render the complete description, including subsequent paragraphs."""
    content = " ".join(doc["description"])
    # Source comments are plain text with optional Javadoc paragraph markers.
    # Split those markers before escaping so HTML paragraphs are never nested.
    paragraphs = re.split(r"</?p\s*>", content, flags=re.IGNORECASE)
    return "".join('<p class="method-desc">' + strip_java_markup(part.strip()) + '</p>'
                   for part in paragraphs if part.strip())


def methods_section(entries, js, link_prefix=""):
    """Generate the searchable source API reference from method comments."""
    categories = [
        "Application", "Automation", "POM · Page objects", "Tests",
        "Utilities", "Documentation tooling", "Other source files",
        "JavaScript and presentation"
    ]

    grouped = defaultdict(lambda: defaultdict(list))

    for entry in entries:
        category = section_for_source(entry["path"])
        grouped[category][entry["path"]].append(
            (entry, "java")
        )

    for entry in js:
        grouped["JavaScript and presentation"][entry["path"]].append(
            (entry, "js")
        )

    total = len(entries) + len(js)

    pieces = [
        '<details class="doc-section guide-section" id="method-reference" open>',
        '<summary class="guide-heading"><div>',
        '<div class="section-kicker">10 / Source API index</div>',
        '<h2>Searchable method reference</h2></div>',
        '<span class="chevron" aria-hidden="true">⌄</span></summary>',
        '<div class="guide-body">',
        '<p class="section-intro">'
        'Generated from the Javadoc and JSDoc directly above declarations '
        'in the project. The source comments are authoritative: edit those '
        'comments and rerun '
        '<code>python3 scripts/generate_documentation.py</code> '
        'to update this guide.</p>',
        '<div class="reference-intro">',
        '<input type="search" id="methodSearch" class="ref-search" '
        'aria-label="Search methods, classes, and descriptions" '
        'placeholder="Search method, class, file, or purpose…">',
        f'<span id="methodCount" class="ref-count">'
        f'{total} / {total} declarations</span>',
        '<button type="button" class="ref-button" id="clearSearch">Clear</button>',
        '<button type="button" class="ref-button" id="expandAll">Expand all</button>',
        '<button type="button" class="ref-button" id="collapseAll">Collapse all</button>',
        '</div>',
        '<nav class="package-nav" aria-label="Source packages">',
        *(
            f'<a href="#{section_anchor(category)}">'
            f'<strong>{html.escape(category)}</strong>'
            f'<span>{len(grouped[category])} files</span></a>'
            for category in categories if grouped.get(category)
        ),
        '</nav>'
    ]

    for category in categories:
        files = grouped.get(category)

        if not files:
            continue

        number = sum(len(x) for x in files.values())

        pieces.append(
            f'<details class="ref-section" id="{section_anchor(category)}">'
            '<summary class="ref-section-heading">'
            '<div><div class="package-kicker">SOURCE PACKAGE</div>'
            f'<h3>{html.escape(category)}</h3></div>'
            f'<span class="package-count">{len(files)} files · {number} declarations</span>'
            '<span class="chevron" aria-hidden="true">⌄</span>'
            '</summary><div class="source-grid">'
        )

        for path, declarations in sorted(files.items()):
            import hashlib

            fid = (
                "file-"
                + hashlib.sha256(path.encode()).hexdigest()[:12]
            )

            filename = Path(path).name

            source_doc = (
                class_doc(path) if path.endswith(".java") else
                {"summary": "JavaScript source functions", "description": [], "tags": []}
            )
            source_description = source_doc["summary"] or filename

            pieces.append(
                f'<details class="source-group" id="{fid}">'
                '<summary>'
                '<span class="file-heading">'
                f'<span class="file-kind">{html.escape(Path(path).suffix.lstrip(".").upper())}</span>'
                f'<span class="source-title">{html.escape(filename)}</span>'
                '<span class="chevron" aria-hidden="true">⌄</span>'
                '</span>'
                f'<span class="method-source-info">{strip_java_markup(source_description)}</span>'
                f'<span class="source-meta">{html.escape(path)}</span>'
                f'<span class="file-count">{len(declarations)} declarations</span>'
                '</summary>'
                '<div class="method-stack">'
            )
            if source_doc["description"] or source_doc["tags"]:
                pieces.append('<section class="class-details">'
                              '<h4>About this class</h4>' + description_html(source_doc)
                              + method_tags_html(source_doc["tags"]) + '</section>')

            for entry, language in declarations:
                doc = entry["doc"]
                signature = entry["signature"]
                description = doc["summary"]

                all_doc = (
                    " ".join(doc["description"])
                    + " "
                    + " ".join(
                        tag + " " + value
                        for tag, value in doc["tags"]
                    )
                )

                searchable = " ".join([
                    path,
                    category,
                    source_description,
                    " ".join(source_doc["description"]),
                    entry.get("owner", ""),
                    entry["name"],
                    signature,
                    all_doc
                ]).lower()

                label = (
                    "JavaScript function"
                    if language == "js"
                    else (
                        "Constructor"
                        if entry["name"] == "<init>"
                        else "Java method"
                    )
                )

                pieces.append(
                    f'<details class="method-entry" '
                    f'id="{anchor(entry, language)}" '
                    f'data-search="{html.escape(searchable, quote=True)}">'
                    '<summary class="method-header">'
                    f'<code>{html.escape(signature)}</code>'
                    f'<span class="method-type">{label}</span>'
                    '<span class="chevron" aria-hidden="true">⌄</span>'
                    '</summary><div class="method-body">'
                )
                pieces.append('<section class="definition-section"><h5>Description</h5>'
                              + description_html(doc) + '</section>')
                pieces.append(method_tags_html(doc["tags"], entry, language))

                line_number = entry["line"]

                pieces.append(
                    '<div class="method-foot">'
                    f'<span>{html.escape(entry.get("owner", Path(path).stem))}</span>'
                    f'<a href="{html.escape(link_prefix + path, quote=True)}" '
                    'title="Open source file">'
                    f'{html.escape(path)} · line {line_number}'
                    '</a>'
                    '</div>'
                    '</div></details>'
                )

            pieces.append('</div></details>')

        pieces.append('</div></details>')

    pieces.append('</div></details>')

    return "\n".join(pieces)


def settings_from_source():
    """Load current scanner settings for the generated HTML user guide."""
    settings = {}

    path = ROOT / "src/test/resources/config.properties"

    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()

        if (
            not line
            or line.startswith(("#", "!"))
            or "=" not in line
        ):
            continue

        key, value = line.split("=", 1)
        settings[key.strip()] = value.strip()

    return settings


def styled_readme_html(entries, js):
    """Generate the styled HTML user guide using documented source methods."""
    template_path = (
        ROOT / "scripts/templates/readme_template.html"
    )

    template = template_path.read_text(encoding="utf-8")
    settings = settings_from_source()

    juice = settings.get(
        "zap.active.maxMinutes",
        "Not configured"
    )

    dvwa = settings.get(
        "zap.dvwa.active.maxMinutes",
        "Not configured"
    )

    limit_label = (
        f"{juice} minutes per application"
        if juice == dvwa
        else f"{juice} minutes for Juice Shop and {dvwa} for DVWA"
    )

    replacements = {
        "JAVA_SOURCE_FILES": str(
            len({entry["path"] for entry in entries})
        ),
        "JAVA_DECLARATIONS": str(len(entries)),
        "JS_FUNCTIONS": str(len(js)),
        "JUICE_SCAN_MINUTES": html.escape(juice),
        "DVWA_SCAN_MINUTES": html.escape(dvwa),
        "SCAN_LIMIT_LABEL": html.escape(limit_label),

        # README.html now lives in docs/, so source links
        # must navigate up one directory to project root.
        "METHOD_REFERENCE": methods_section(
            entries,
            js,
            link_prefix="../"
        )
    }

    for name, value in replacements.items():
        token = "{{" + name + "}}"

        if token not in template:
            raise ValueError(
                f"Missing template marker: {token}"
            )

        template = template.replace(token, value)

    if re.search(r"\{\{[A-Z_]+\}\}", template):
        raise ValueError(
            "Unfilled documentation template markers"
        )

    return template


def styled_api_html(entries, js):
    """Generate the standalone API reference using the shared HTML styling."""
    template_path = (
        ROOT / "scripts/templates/readme_template.html"
    )

    template = template_path.read_text(encoding="utf-8")

    style = re.search(
        r"<style>([\s\S]*?)</style>",
        template
    )

    script = re.search(
        r"<script>([\s\S]*?)</script>",
        template
    )

    if not style or not script:
        raise ValueError(
            "Missing shared documentation styles or search behavior"
        )

    content = (
        '<header class="appbar">'
        '<div class="brand">'
        '<div class="brand-mark" aria-hidden="true">◈</div>'
        '<div>'
        '<strong>OWASP Security Automation</strong>'
        '<small>Source API reference</small>'
        '</div>'
        '</div>'
        '<div class="appbar-right">'
        '<a href="README.html">← User guide</a>'
        '</div>'
        '</header>'

        '<div class="layout">'
        '<nav class="sidebar" aria-label="Documentation">'
        '<div class="side-label">Navigation</div>'
        '<a href="README.html">User guide</a>'
        '<a href="#method-reference">Searchable source reference</a>'
        '</nav>'

        '<main class="main">'
        '<div class="hero">'
        '<div class="eyebrow">Developer reference</div>'
        '<h1>Explore the documented source.</h1>'
        '<p>Browse every documented Java declaration and named '
        'JavaScript function. All content comes directly '
        'from the source comments.</p>'
        '<div class="hero-links">'
        '<a class="action" href="README.html">'
        '← Return to user guide</a>'
        '</div>'
        '</div>'

        # Both HTML files now reside in docs/.
        # Java and JavaScript sources are one directory above.
        + methods_section(
            entries,
            js,
            link_prefix="../"
        )

        + '<footer class="footer">'
        'Generated directly from the project source. '
        'Edit Javadoc and JSDoc, then run the '
        'documentation generator.'
        '</footer>'
        '</main>'
        '</div>'
    )

    return (
        '<!doctype html>'
        '<html lang="en">'
        '<head>'
        '<meta charset="utf-8">'
        '<meta name="viewport" '
        'content="width=device-width, initial-scale=1">'
        '<title>OWASP Security Automation — Source API reference</title>'
        '<style>'
        + style.group(1)
        + '</style>'
        '</head>'
        '<body>'
        + content
        + '<script>'
        + script.group(1)
        + '</script>'
        '</body>'
        '</html>'
    )


def main():
    """Regenerate both HTML documents without changing docs/README.md."""
    entries = java_entries()
    js = js_entries()

    if not entries or not js:
        raise SystemExit(
            "Source documentation not found; nothing generated"
        )

    docs = ROOT / "docs"
    docs.mkdir(parents=True, exist_ok=True)

    (docs / "README.html").write_text(
        styled_readme_html(entries, js),
        encoding="utf-8"
    )

    (docs / "api.html").write_text(
        styled_api_html(entries, js),
        encoding="utf-8"
    )

    print(
        f"Updated docs/README.html and docs/api.html "
        f"from {len(entries)} Java declarations "
        f"and {len(js)} JavaScript functions."
    )


if __name__ == "__main__":
    main()
