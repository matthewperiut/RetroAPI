#!/usr/bin/env python3
"""
CI helper: push a freshly-published RetroAPI version into the matthewperiut.github.io pages repo.

Given the pages repo checked out at --pages and the RetroAPI artifact already published to the local
Maven repo (~/.m2 via `./gradlew publish -Pstationapi`), this:

  1. copies repository/com/periut/retroapi/<version>/{jar,-sources.jar,.pom} into the pages maven repo
     (the jar carries the JiJ'd retroapi-stationapi compat, since it was built with -Pstationapi),
  2. rewrites repository/com/periut/retroapi/maven-metadata.xml (latest/release = newest version),
  3. updates the retroapi entry in manifest.json (version / path / commit / branch),
  4. regenerates repository-tree.json from the repository tree,
  5. bumps every retroapi version mention in the docs (retroapi/*.html) and inside the two template
     zips (gradle.properties + fabric.mod.json) to the new version.

It is non-interactive (unlike sync-m2.py) and idempotent: re-running with the same version is a no-op.
"""
import argparse
import io
import json
import os
import re
import shutil
import zipfile
from datetime import datetime, timezone
from pathlib import Path

GROUP_PATH = "com/periut/retroapi"
M2 = Path.home() / ".m2" / "repository" / GROUP_PATH


def copy_artifact(pages: Path, version: str) -> None:
    src = M2 / version
    if not src.is_dir():
        raise SystemExit(f"artifact not found in local maven repo: {src} (did `gradlew publish` run?)")
    dst = pages / "repository" / GROUP_PATH / version
    dst.mkdir(parents=True, exist_ok=True)
    # The Ornithe maven artifact only: main jar, sources jar, pom. The -babric jar is a GitHub/Modrinth
    # artifact, not part of the Gradle-consumable maven repo, so it is deliberately excluded.
    for f in src.iterdir():
        if f.name.startswith("maven-metadata"):
            continue
        if "-babric" in f.name:
            continue
        if f.suffix in (".jar", ".pom") or f.name.endswith("-sources.jar"):
            shutil.copy2(f, dst / f.name)
    print(f"copied retroapi {version} artifacts -> {dst}")


def write_maven_metadata(pages: Path, version: str) -> None:
    base = pages / "repository" / GROUP_PATH
    versions = sorted(
        (p.name for p in base.iterdir() if p.is_dir()),
        key=lambda v: [int(x) if x.isdigit() else x for x in re.split(r"[.+_-]", v)],
    )
    if version not in versions:
        versions.append(version)
    stamp = datetime.now(timezone.utc).strftime("%Y%m%d%H%M%S")
    ver_xml = "\n".join(f"      <version>{v}</version>" for v in versions)
    xml = (
        '<?xml version="1.0" encoding="UTF-8"?>\n'
        "<metadata>\n"
        "  <groupId>com.periut</groupId>\n"
        "  <artifactId>retroapi</artifactId>\n"
        "  <versioning>\n"
        f"    <latest>{version}</latest>\n"
        f"    <release>{version}</release>\n"
        "    <versions>\n"
        f"{ver_xml}\n"
        "    </versions>\n"
        f"    <lastUpdated>{stamp}</lastUpdated>\n"
        "  </versioning>\n"
        "</metadata>\n"
    )
    (base / "maven-metadata.xml").write_text(xml)
    print(f"wrote maven-metadata.xml (latest={version}, {len(versions)} versions)")


def update_manifest(pages: Path, version: str, commit: str, branch: str) -> None:
    short = commit[:7]  # match the existing manifest's short-hash convention
    path = pages / "manifest.json"
    data = json.loads(path.read_text())
    found = False
    for jar in data.get("jars", []):
        if jar.get("group") == "com.periut" and jar.get("artifact") == "retroapi":
            jar["version"] = version
            jar["path"] = f"repository/{GROUP_PATH}/{version}/retroapi-{version}.jar"
            jar["commit"] = short
            jar["branch"] = branch
            found = True
            break
    if not found:
        data.setdefault("jars", []).append({
            "group": "com.periut", "artifact": "retroapi", "version": version,
            "path": f"repository/{GROUP_PATH}/{version}/retroapi-{version}.jar",
            "repo": "matthewperiut/RetroAPI", "branch": branch, "commit": short,
        })
    path.write_text(json.dumps(data, indent=2))
    print(f"updated manifest.json retroapi -> {version} @ {short} ({branch})")


def build_tree(path: Path) -> dict:
    tree = {"children": {}}
    for entry in sorted(os.listdir(path)):
        full = path / entry
        tree["children"][entry] = build_tree(full) if full.is_dir() else {}
    return tree


def regenerate_tree(pages: Path) -> None:
    repo = pages / "repository"
    if repo.is_dir():
        (pages / "repository-tree.json").write_text(json.dumps(build_tree(repo)))
        print("regenerated repository-tree.json")


# Each pattern captures everything up to the version value in group 1; the value itself is replaced.
VERSION_PATTERNS = [
    re.compile(r'(retroapi_version\s*=\s*)[0-9A-Za-z.+_-]+'),           # gradle.properties / docs snippet
    re.compile(r'("retroapi"\s*:\s*"&gt;=)[^"]*(")'),                    # HTML-escaped fabric.mod.json (docs)
    re.compile(r'("retroapi"\s*:\s*">=?)[^"]*(")'),                      # raw fabric.mod.json (templates)
]


def bump_text(text: str, version: str) -> str:
    out = VERSION_PATTERNS[0].sub(lambda m: m.group(1) + version, text)
    out = VERSION_PATTERNS[1].sub(lambda m: m.group(1) + version + m.group(2), out)
    out = VERSION_PATTERNS[2].sub(lambda m: m.group(1) + version + m.group(2), out)
    return out


def bump_docs(pages: Path, version: str) -> None:
    for html in (pages / "retroapi").glob("*.html"):
        text = html.read_text()
        new = bump_text(text, version)
        if new != text:
            html.write_text(new)
            print(f"bumped {html.relative_to(pages)}")


def bump_template_zip(zip_path: Path, version: str) -> None:
    if not zip_path.is_file():
        print(f"template zip missing, skipping: {zip_path}")
        return
    with zipfile.ZipFile(zip_path) as zin:
        items = [(i, zin.read(i.filename)) for i in zin.infolist()]
    changed = False
    buf = io.BytesIO()
    with zipfile.ZipFile(buf, "w", zipfile.ZIP_DEFLATED) as zout:
        for info, data in items:
            if info.filename.endswith(("gradle.properties", "fabric.mod.json")):
                new = bump_text(data.decode("utf-8"), version).encode("utf-8")
                if new != data:
                    changed = True
                    data = new
            zout.writestr(info, data)
    if changed:
        zip_path.write_bytes(buf.getvalue())
        print(f"bumped {zip_path.name}")
    else:
        print(f"no version refs changed in {zip_path.name}")


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--pages", required=True, type=Path, help="path to the checked-out pages repo")
    ap.add_argument("--version", required=True, help="new retroapi version (from gradle.properties)")
    ap.add_argument("--commit", required=True, help="RetroAPI commit sha")
    ap.add_argument("--branch", required=True, help="RetroAPI source branch")
    args = ap.parse_args()

    pages = args.pages
    copy_artifact(pages, args.version)
    write_maven_metadata(pages, args.version)
    update_manifest(pages, args.version, args.commit, args.branch)
    regenerate_tree(pages)
    bump_docs(pages, args.version)
    for name in ("bare-retroapi-template.zip", "feature-showcase-retroapi-template.zip"):
        bump_template_zip(pages / "retroapi" / name, args.version)


if __name__ == "__main__":
    main()
