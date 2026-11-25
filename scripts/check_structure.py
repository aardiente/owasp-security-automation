#!/usr/bin/env python3
"""Validate challenge coverage, source comments, suite references and workbook integrity without Maven."""
from pathlib import Path
import base64
import hashlib
import json
import re
import subprocess
import tempfile
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'src/test/java'
INVENTORY = ROOT / 'docs/challenge-inventory.json'


def normalize(body):
    """Canonicalize the intentional evidence and domain-routing substitutions."""
    body = re.sub(r'\bactions\.(account|basket|feedback|resources|catalog|uploads|browser)\(\)\.', 'actions.', body)
    body = re.sub(r'ScreenshotManager\.captureResponse\((?:ZapManager\.getDriver\(\)|driver),\s*([^;]+?)\);', r'captureResponseEvidence(\1);', body)
    if len(re.findall(r'\bdriver\b', body)) == 1:
        body = body.replace('        var driver = ZapManager.getDriver();\n', '')
    body = body.replace('java.nio.file.Path.of(System.getProperty("user.dir"))\n                .resolve(getClass().getSimpleName() + "-challenges.json")', 'automation.AppConfig.juiceShopChallengesOutput()')
    return '\n'.join(line.rstrip() for line in body.splitlines() if line.strip())


def digest(text):
    """Return a stable hash for text or workbook bytes."""
    return hashlib.sha256(text.encode() if isinstance(text, str) else text).hexdigest()


def declarations():
    """Read source declarations and attached comments from the existing Java 17 parser."""
    with tempfile.TemporaryDirectory(prefix='structure-check-') as out:
        subprocess.run(['javac', '--release', '17', '-d', out, str(ROOT / 'scripts/MethodExtractor.java')], check=True)
        result = subprocess.run(['java', '-cp', out, 'MethodExtractor', str(ROOT)], check=True, capture_output=True, text=True)
    for line in result.stdout.splitlines():
        fields = line.split('\t')
        path, owner, name, _, parameters = [base64.b64decode(v).decode() for v in fields[:5]]
        source = Path(path).read_text(encoding='utf-8')
        start, end, body = map(int, fields[5:8])
        comment = base64.b64decode(fields[8]).decode()
        assert comment.strip(), f'Missing attached Javadoc: {owner}.{name}'
        package = re.search(r'^package ([\w.]+);', source, re.M)
        qualified = (package.group(1) + '.' if package else '') + owner
        yield qualified, name, parameters, source[start:end], source[body:end] if body >= 0 else ''


def main():
    """Check the preserved challenge manifest, action source hashes and suite class references."""
    inv = json.loads(INVENTORY.read_text())
    methods = list(declarations())
    tests = {(owner, name): (declaration, body) for owner, name, _, declaration, body in methods
             if owner.startswith('tests.JuiceShop.') and re.search(r'@Test\b', declaration)}
    expected = {(item['class'], item['method']) for item in inv['retained']}
    assert set(tests) == expected, f'Challenge inventory mismatch: {set(tests) ^ expected}'
    for item in inv['retained']:
        declaration, body = tests[item['class'], item['method']]
        assert int(re.search(r'priority\s*=\s*(\d+)', declaration).group(1)) == item['priority'], item['method']
        assert ('enabled = false' in declaration) == item['disabled'], item['method']
        assert digest(normalize(body)) == item['normalized_body_sha256'], f'Challenge body changed: {item["method"]}'
    for item in inv['action_bodies']:
        matches = [body for owner, name, params, _, body in methods
                   if owner == item['class'] and name == item['method'] and params == item['parameters']]
        assert len(matches) == 1 and digest(matches[0]) == item['body_sha256'], item['method']
    for name, checksum in inv['workbook_sha256'].items():
        assert digest((ROOT / 'src/test/resources/data' / name).read_bytes()) == checksum, name
    for suite in (ROOT / 'testng').glob('*.xml'):
        tree = ET.parse(suite)
        for cls in tree.iter('class'):
            name = cls.attrib['name']
            assert (SOURCE / (name.replace('.', '/') + '.java')).is_file(), f'Unknown suite class: {name}'
        for ref in tree.iter('suite-file'):
            assert (suite.parent / ref.attrib['path']).is_file(), f'Unknown nested suite: {ref.attrib["path"]}'
    actual = {node.attrib['name'] for node in ET.parse(ROOT / 'testng/testng-juiceshop.xml').iter('class')}
    assert actual == {owner for owner, _ in expected}, 'Juice Shop suite does not match challenge manifest'
    final_methods = {name: decl for (owner, name), (decl, _) in tests.items() if owner.endswith('JuiceShopScoreBoardTest')}
    assert 'dependsOnGroups = "juice-challenge"' in final_methods['testMassDispelChallenge']
    assert 'dependsOnMethods = "testMassDispelChallenge"' in final_methods['testScoreBoardChallenge']
    assert all('alwaysRun = true' in decl for decl in final_methods.values())
    print(f'Structure checks passed: {len(tests)} challenge methods, {len(inv["action_bodies"])} action bodies, '
          f'{len(inv["workbook_sha256"])} workbooks, {len(methods)} documented Java declarations.')


if __name__ == '__main__':
    main()
