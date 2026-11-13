
"use strict";

const state = { targets: [], data: null, view: "overview" };
const byId = id => document.getElementById(id);
const node = (tag, className = "", text = "") => {
  const element = document.createElement(tag);
  if (className) element.className = className;
  if (text !== undefined && text !== null) element.textContent = String(text);
  return element;
};
const add = (parent, ...children) => { children.forEach(child => parent.append(child)); return parent; };
const clear = element => element.replaceChildren();
const countBy = (items, key, value) => items.filter(item => item[key] === value).length;
const severityOrder = { High: 0, Medium: 1, Low: 2, Informational: 3 };
const lower = value => String(value ?? "").toLowerCase();

async function request(url) {
  const response = await fetch(url, { cache: "no-store" });
  if (!response.ok) throw new Error(`Unable to load ${url} (HTTP ${response.status})`);
  return response.json();
}

async function initialize() {
  try {
    state.targets = await request("/api/targets");
    const select = byId("target-select");
    clear(select);

    state.targets.forEach(target => {
      const option = node("option", "", target.title);
      option.value = target.id;
      select.append(option);
    });

    if (!state.targets.length) throw new Error("No reporting targets are configured.");

    select.addEventListener("change", () => loadAssessment(select.value));
    byId("refresh").addEventListener("click", () => loadAssessment(select.value));

    document.querySelectorAll("[data-view]").forEach(button =>
        button.addEventListener("click", () => showView(button.dataset.view)));

    ["finding-search", "finding-status", "finding-severity"].forEach(id =>
        byId(id).addEventListener(id === "finding-search" ? "input" : "change", renderFindings));

    ["test-search", "test-status"].forEach(id =>
        byId(id).addEventListener(id === "test-search" ? "input" : "change", renderTests));

    byId("expand-findings").addEventListener("click", () =>
        document.querySelectorAll("#findings-list details").forEach(card => { card.open = true; }));

    byId("collapse-findings").addEventListener("click", () =>
        document.querySelectorAll("#findings-list details").forEach(card => { card.open = false; }));

    await loadAssessment(select.value);
  } catch (error) {
    showError(error);
  }
}

async function loadAssessment(targetId) {
  byId("loading").hidden = false;
  byId("content").hidden = true;
  byId("error").hidden = true;

  try {
    state.data = await request(`/api/assessments/${encodeURIComponent(targetId)}`);

    byId("page-title").textContent = state.data.target.title + " assessment";
    byId("target-origin").textContent = state.data.target.site;
    byId("scan-time").textContent = state.data.scan.generated || "No ZAP scan available";
    byId("scan-status").textContent = state.data.scan.status === "loaded"
        ? "● Scan results loaded" : "○ " + state.data.scan.status;
    byId("page-subtitle").textContent = "Review findings, examine evidence, and track automated test execution.";
    byId("nav-findings-count").textContent = groupFindings(
        state.data.scan.findings.filter(x => x.status === "INCLUDE")).length;

    renderOverview();
    renderFindings();
    renderTests();
    renderEvidence();

    byId("content").hidden = false;
    showView(state.view);
  } catch (error) {
    showError(error);
  } finally {
    byId("loading").hidden = true;
  }
}

function showError(error) {
  byId("error").hidden = false;
  byId("error").textContent = error?.message || String(error);
  byId("loading").hidden = true;
}

function showView(view) {
  state.view = view;
  document.querySelectorAll(".view").forEach(element => {
    element.hidden = element.id !== `${view}-view`;
  });
  document.querySelectorAll(".nav-item").forEach(button =>
      button.classList.toggle("active", button.dataset.view === view));

  byId("breadcrumb").textContent = {
    overview: "Overview",
    findings: "Security findings",
    tests: "Automated tests",
    evidence: "Evidence library"
  }[view];
}

function badge(value, kind = value) {
  return node("span", `badge ${lower(kind).replace(/[^a-z]/g, "")}`, value);
}

function summaryMetric(label, value, caption, accent) {
  const element = node("div", "metric");
  element.style.setProperty("--accent", accent);
  add(element,
      node("div", "metric-label", label),
      node("div", "metric-number", value),
      node("div", "metric-caption", caption));
  return element;
}

function groupFindings(findings) {
  const groups = new Map();

  for (const finding of findings) {
    const key = [finding.pluginId, finding.name, finding.status].join("\u0000");
    if (!groups.has(key)) groups.set(key, { ...finding, instances: [] });
    groups.get(key).instances.push(finding);
  }

  return [...groups.values()].sort((a, b) =>
      (severityOrder[a.severity] ?? 4) - (severityOrder[b.severity] ?? 4)
      || a.name.localeCompare(b.name));
}

function renderOverview() {
  const all = state.data.scan.findings;
  const active = all.filter(x => x.status === "INCLUDE");
  const tests = state.data.tests;
  const metrics = byId("metric-grid");
  clear(metrics);

  add(metrics,
      summaryMetric("Active findings", groupFindings(active).length,
          `${active.length} request observations`, "#3d72b8"),
      summaryMetric("High severity",
          groupFindings(active.filter(x => x.severity === "High")).length,
          "Review first", "#d3445c"),
      summaryMetric("Needs validation",
          groupFindings(all.filter(x => x.status === "REVIEW")).length,
          "Requires investigation", "#e6aa4e"),
      summaryMetric("Executed tests", tests.length,
          `${countBy(tests, "status", "PASSED")} passed · ${countBy(tests, "status", "FAILED")} failed`,
          "#40a892"));

  const severity = byId("severity-chart");
  clear(severity);
  const groups = groupFindings(active);
  const max = Math.max(1, ...["High", "Medium", "Low", "Informational"]
      .map(level => countBy(groups, "severity", level)));

  const colors = {
    High: "#d84c62",
    Medium: "#e9ac56",
    Low: "#d5bc63",
    Informational: "#6498da"
  };

  for (const level of ["High", "Medium", "Low", "Informational"]) {
    const number = countBy(groups, "severity", level);
    const row = node("div", "severity-row");
    const track = node("div", "severity-track");
    const fill = node("span", "severity-fill");

    fill.style.width = `${100 * number / max}%`;
    fill.style.setProperty("--accent", colors[level]);
    track.append(fill);

    add(row,
        node("label", "", level),
        track,
        node("span", "severity-number", number));
    severity.append(row);
  }

  const triage = byId("triage-list");
  clear(triage);

  const statuses = [
    ["Active / in scope", groupFindings(active).length, "#4d8abb"],
    ["Needs validation", groupFindings(all.filter(x => x.status === "REVIEW")).length, "#e1a247"],
    ["Suppressed by rule", groupFindings(all.filter(x => x.status === "SUPPRESS")).length, "#9099ad"],
    ["Out-of-scope instances", state.data.scan.excludedInstances, "#bbcadc"]
  ];

  statuses.forEach(([label, count, color]) => {
    const row = node("div", "triage-row");
    const dot = node("span", "triage-dot");
    dot.style.setProperty("--accent", color);
    add(row, dot, node("strong", "", label), node("b", "", count));
    triage.append(row);
  });

  const priorities = byId("priority-findings");
  clear(priorities);

  for (const finding of groups.slice(0, 5)) {
    const row = node("button", "priority-row");
    row.type = "button";
    row.style.width = "100%";
    row.style.textAlign = "left";
    row.style.background = "#fff";
    row.setAttribute("aria-label", `View ${finding.name} in Security findings`);
    row.addEventListener("click", () => openPriorityFinding(finding));

    const main = node("div", "result-main", finding.name);
    main.append(node("span", "muted", `${finding.instances.length} request observation(s)`));
    add(row, badge(finding.severity), main);
    priorities.append(row);
  }

  if (!groups.length)
    priorities.append(node("p", "note", "No active findings in the current report."));
}

function openPriorityFinding(finding) {
  byId("finding-search").value = "";
  byId("finding-status").value = "INCLUDE";
  byId("finding-severity").value = "ALL";

  renderFindings();
  showView("findings");

  const card = [...byId("findings-list").querySelectorAll(".result-card")].find(item =>
      item.dataset.pluginId === String(finding.pluginId) &&
      item.dataset.findingName === finding.name &&
      item.dataset.findingStatus === finding.status);

  if (!card) return;
  card.open = true;
  card.querySelector("summary")?.focus({ preventScroll: true });
  card.scrollIntoView({ behavior: "smooth", block: "start" });
}

function field(label, value) {
  const block = node("div", "detail-block");
  add(block, node("label", "", label), node("div", "", value || "Not provided"));
  return block;
}

function textSection(title, content, pre = false) {
  const section = node("section", "description-block");
  section.append(node("h3", "", title));

  if (pre)
    section.append(node("pre", "code-evidence",
        content || "No inline excerpt included in the ZAP JSON."));
  else
    section.append(node("p", "", content || "Not provided"));

  return section;
}

function detectionContext(group, observation) {
  const section = node("section", "description-block");
  section.append(node("h3", "", "ZAP detection context (scanner-reported)"));

  const name = lower(group.name);
  let basis;

  if (name.includes("content security policy") && name.includes("header not set")) {
    basis = "ZAP reported that the response did not include a Content-Security-Policy header. This check concerns an absent response header, so there is no matching text to quote.";
  } else if (name.includes("missing anti-clickjacking header")) {
    basis = "ZAP reported missing anti-clickjacking response protection. The relevant controls are X-Frame-Options and Content-Security-Policy with a frame-ancestors directive.";
  } else {
    basis = `ZAP classified this request under "${group.name}". No matched response text was included in the exported observation.`;
  }

  section.append(node("p", "", basis));

  const details = [
    `Request inspected: ${observation.method || "HTTP"} ${observation.url || "URL unavailable"}`,
    `Scanner confidence: ${observation.confidence || "Not provided"}`,
    observation.parameter ? `Inspected parameter/header: ${observation.parameter}` : ""
  ].filter(Boolean);

  section.append(node("pre", "code-evidence", details.join("\n")));
  return section;
}

function makeFinding(group) {
  const card = node("details", "result-card");
  card.dataset.pluginId = String(group.pluginId);
  card.dataset.findingName = group.name;
  card.dataset.findingStatus = group.status;

  const summary = node("summary");
  const main = node("div", "result-main");

  add(main,
      node("div", "result-title", group.name),
      node("div", "result-subtitle",
          `${group.instances.length} observation(s) · CWE ${group.cwe || "N/A"}`));

  const meta = node("div", "result-meta");

  add(meta,
      badge(group.severity),
      badge(group.status === "INCLUDE" ? "Active"
              : group.status === "REVIEW" ? "Needs validation" : "Suppressed",
          group.status),
      node("span", "muted", "▾"));

  add(summary, main, meta);
  card.append(summary);

  const body = node("div", "result-content");

  if (group.reason)
    body.append(textSection("Filtering decision", group.reason));

  body.append(textSection("Finding description", group.description));
  body.append(textSection("Recommendation", group.recommendation));

  for (let i = 0; i < group.instances.length; i++) {
    const observation = group.instances[i];
    const instance = node("div", "instance");

    instance.append(node("span", "instance-number",
        `OBSERVATION ${i + 1} OF ${group.instances.length}`));

    const grid = node("div", "detail-grid");

    add(grid,
        field("Affected URL", observation.url),
        field("HTTP method", observation.method),
        field("Parameter / header", observation.parameter),
        field("Confidence", observation.confidence));

    instance.append(grid);

    if (observation.evidence)
      instance.append(textSection("Matched evidence", observation.evidence, true));

    if (observation.testInput)
      instance.append(textSection("Test input", observation.testInput, true));

    if (observation.otherInfo)
      instance.append(textSection("ZAP details", observation.otherInfo));

    if (!observation.evidence && !observation.otherInfo)
      instance.append(detectionContext(group, observation));

    body.append(instance);
  }

  card.append(body);
  return card;
}

function renderFindings() {
  if (!state.data) return;

  const selectedStatus = byId("finding-status").value;
  const selectedSeverity = byId("finding-severity").value;
  const term = lower(byId("finding-search").value.trim());

  const matches = state.data.scan.findings.filter(item =>
      (selectedStatus === "ALL" || item.status === selectedStatus) &&
      (selectedSeverity === "ALL" || item.severity === selectedSeverity) &&
      (!term || [item.name, item.url, item.cwe, item.evidence, item.otherInfo, item.parameter]
          .some(value => lower(value).includes(term))));

  const grouped = groupFindings(matches);
  byId("finding-count").textContent = `${grouped.length} findings / ${matches.length} instances`;

  const list = byId("findings-list");
  clear(list);
  grouped.forEach(group => list.append(makeFinding(group)));
  byId("findings-empty").hidden = grouped.length !== 0;
}

function renderTests() {
  if (!state.data) return;

  const term = lower(byId("test-search").value.trim());
  const status = byId("test-status").value;

  const tests = state.data.tests.filter(test =>
      (status === "ALL" || test.status === status) &&
      (!term || lower(test.method).includes(term) || lower(test.className).includes(term)));

  byId("test-count").textContent = `${tests.length} test executions`;

  const list = byId("tests-list");
  clear(list);

  for (const test of tests) {
    const card = node("details", "result-card");
    const summary = node("summary");
    const main = node("div", "result-main");

    add(main,
        node("div", "result-title", test.method),
        node("div", "result-subtitle", test.className));

    const meta = node("div", "result-meta");

    add(meta,
        badge(test.status),
        node("span", "muted", `${test.durationMs} ms`),
        node("span", "muted", "▾"));

    add(summary, main, meta);

    const body = node("div", "result-content");

    if (test.error)
      body.append(textSection("Test failure", test.error.replaceAll("\\n", "\n"), true));

    // Only an explicit one-to-one association is acceptable.
    // Do not substitute method-level image galleries or guess using security levels.
    const screenshots = Array.isArray(test.screenshots) ? test.screenshots : [];
    const hasExactScreenshot = !test.ambiguousEvidence &&
        screenshots.length === 1 &&
        typeof screenshots[0] === "string" &&
        screenshots[0].startsWith("/api/screenshots/");

    if (hasExactScreenshot) {
      const src = screenshots[0];
      const filename = decodeURIComponent(src.split("/").at(-1));

      body.append(node("h3", "", "Execution screenshot"));

      const gallery = node("div", "screenshot-set");
      gallery.append(imageLink(src, filename));
      body.append(gallery);
    } else {
      body.append(node("p", "note",
          "Screenshot not linked to this execution. The report must contain the exact screenshot filename for this test run."));
    }

    card.append(summary, body);
    list.append(card);
  }

  if (!tests.length)
    list.append(node("div", "empty",
        "No tests match the selected filters, or no TestNG CSV is available."));
}

function imageLink(src, label) {
  const anchor = node("a", "screenshot");
  anchor.href = src;
  anchor.target = "_blank";
  anchor.rel = "noopener noreferrer";

  const img = node("img");
  img.src = src;
  img.loading = "lazy";
  img.alt = label;

  add(anchor, img, node("span", "", label));
  return anchor;
}

function renderEvidence() {
  if (!state.data) return;

  const gallery = byId("evidence-list");
  clear(gallery);

  const seen = new Set();
  let amount = 0;

  for (const test of state.data.evidence) {
    for (const src of test.screenshots) {
      if (seen.has(src)) continue;
      seen.add(src);

      const card = node("a", "gallery-item");
      card.href = src;
      card.target = "_blank";
      card.rel = "noopener noreferrer";

      const img = node("img");
      img.src = src;
      img.loading = "lazy";
      img.alt = `Evidence for ${test.method}`;

      const meta = node("div", "gallery-meta");
      add(meta,
          node("strong", "", test.method),
          node("span", "", decodeURIComponent(src.split("/").at(-1))));

      add(card, img, meta);
      gallery.append(card);
      amount++;
    }
  }

  byId("evidence-count").textContent = `${amount} images`;
  byId("evidence-empty").hidden = amount !== 0;
}

document.addEventListener("DOMContentLoaded", initialize);
