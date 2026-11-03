
document.addEventListener("DOMContentLoaded", () => {
    const findings = [...document.querySelectorAll(".finding")];
    const search = document.getElementById("finding-search");
    const filter = document.getElementById("risk-filter");
    const expandAll = document.getElementById("expand-all");
    const collapseAll = document.getElementById("collapse-all");
    const emptyState = document.getElementById("finding-empty");

    function updateFindings() {
        const query = search.value.trim().toLowerCase();
        const risk = filter.value;
        let visible = 0;

        findings.forEach(finding => {
            const matchesText = finding.textContent.toLowerCase().includes(query);
            const matchesRisk = risk === "all" || finding.dataset.risk === risk;
            const show = matchesText && matchesRisk;

            finding.hidden = !show;
            if (show) visible++;
        });

        emptyState.hidden = visible > 0;
    }

    search.addEventListener("input", updateFindings);
    filter.addEventListener("change", updateFindings);

    expandAll.addEventListener("click", () => {
        findings.filter(finding => !finding.hidden)
            .forEach(finding => finding.open = true);
    });

    collapseAll.addEventListener("click", () => {
        findings.forEach(finding => finding.open = false);
    });

    updateFindings();
});
