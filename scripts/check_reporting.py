#!/usr/bin/env python3
"""Compile and run dependency-free Java 17 reporting smoke checks."""
from pathlib import Path
import os
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/test/java"
SOURCES = [JAVA / "utilities/EvidenceNames.java", *(
    JAVA / "utilities/reporting" / name for name in (
        "EvidencePolicy.java", "ReportingTarget.java", "ReportingConfiguration.java", "ReportCsv.java"
    )
), ROOT / "scripts/checks/ReportingSmokeCheck.java"]

if __name__ == "__main__":
    with tempfile.TemporaryDirectory(prefix="reporting-check-") as output:
        subprocess.run(["javac", "--release", "17", "-d", output, *map(str, SOURCES)], cwd=ROOT, check=True)
        classpath = os.pathsep.join([output, str(ROOT / "src/test/resources")])
        subprocess.run(["java", "-cp", classpath, "utilities.reporting.ReportingSmokeCheck"], cwd=ROOT, check=True)
