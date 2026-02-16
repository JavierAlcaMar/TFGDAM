#!/usr/bin/env python3
import json
import re
import zlib
import zipfile
import xml.etree.ElementTree as ET
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
XLSX = ROOT / "docs/reference/source_template.xlsx"
PDF = ROOT / "docs/reference/source_help.pdf"
OUT_EXCEL = ROOT / "docs/reference/excel_extract"
OUT_EXCEL.mkdir(parents=True, exist_ok=True)

NS = {
    "m": "http://schemas.openxmlformats.org/spreadsheetml/2006/main",
    "r": "http://schemas.openxmlformats.org/officeDocument/2006/relationships",
}


def extract_excel() -> None:
    with zipfile.ZipFile(XLSX) as z:
        wb = ET.fromstring(z.read("xl/workbook.xml"))
        rels = ET.fromstring(z.read("xl/_rels/workbook.xml.rels"))
        rel_map = {r.attrib["Id"]: r.attrib["Target"] for r in rels}

        sheets = []
        for sh in wb.findall("m:sheets/m:sheet", NS):
            rid = sh.attrib.get(
                "{http://schemas.openxmlformats.org/officeDocument/2006/relationships}id"
            )
            sheets.append(
                {
                    "name": sh.attrib["name"],
                    "sheetId": sh.attrib.get("sheetId"),
                    "target": rel_map.get(rid, ""),
                }
            )

        summary = {
            "sheet_count": len(sheets),
            "sheets": sheets,
            "formulas": {},
            "dataValidations": {},
            "mergedCells": {},
            "definedNames": [],
        }

        for sh in sheets:
            target = sh["target"]
            if not target.startswith("worksheets/"):
                continue
            xml = ET.fromstring(z.read("xl/" + target))

            formulas = []
            for c in xml.findall(".//m:c", NS):
                f = c.find("m:f", NS)
                if f is not None:
                    formulas.append(
                        {
                            "cell": c.attrib.get("r"),
                            "formula": (f.text or "").strip(),
                            "t": f.attrib.get("t"),
                        }
                    )

            dvs = []
            for dv in xml.findall(".//m:dataValidations/m:dataValidation", NS):
                obj = dict(dv.attrib)
                f1 = dv.find("m:formula1", NS)
                f2 = dv.find("m:formula2", NS)
                if f1 is not None:
                    obj["formula1"] = (f1.text or "").strip()
                if f2 is not None:
                    obj["formula2"] = (f2.text or "").strip()
                dvs.append(obj)

            merges = [mc.attrib.get("ref") for mc in xml.findall(".//m:mergeCells/m:mergeCell", NS)]

            summary["formulas"][sh["name"]] = formulas
            summary["dataValidations"][sh["name"]] = dvs
            summary["mergedCells"][sh["name"]] = merges

        for dn in wb.findall(".//m:definedNames/m:definedName", NS):
            summary["definedNames"].append(
                {
                    "name": dn.attrib.get("name"),
                    "localSheetId": dn.attrib.get("localSheetId"),
                    "text": (dn.text or "").strip(),
                }
            )

    (OUT_EXCEL / "excel_structure.json").write_text(
        json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8"
    )

    lines = [f"Sheets: {summary['sheet_count']}"]
    for sh in summary["sheets"]:
        name = sh["name"]
        formulas = summary["formulas"].get(name, [])
        dvs = summary["dataValidations"].get(name, [])
        lines.append(f"\n[{name}] formulas={len(formulas)} dataValidations={len(dvs)}")
        for item in formulas[:120]:
            lines.append(f"- {item['cell']}: ={item['formula']}")
    (OUT_EXCEL / "formula_index.txt").write_text("\n".join(lines), encoding="utf-8")


def extract_pdf_text_lines() -> None:
    raw = PDF.read_bytes()
    streams = re.findall(rb"stream\r?\n(.*?)\r?\nendstream", raw, flags=re.S)
    lines = []

    for chunk in streams:
        candidates = [chunk]
        try:
            candidates.append(zlib.decompress(chunk))
        except Exception:
            pass

        for data in candidates:
            if b"Tj" not in data and b"TJ" not in data:
                continue
            txt = data.decode("latin1", "ignore")
            for lit in re.findall(r"\((.*?)\)\s*Tj", txt, flags=re.S):
                lines.append(lit)
            for arr in re.findall(r"\[(.*?)\]\s*TJ", txt, flags=re.S):
                lines.extend(re.findall(r"\((.*?)\)", arr, flags=re.S))
                for hx in re.findall(r"<([0-9A-Fa-f]{4,})>", arr):
                    try:
                        decoded = bytes.fromhex(hx).decode("utf-16-be", "ignore")
                        if decoded:
                            lines.append(decoded)
                    except Exception:
                        pass

    clean = []
    seen = set()
    for t in lines:
        t = " ".join(t.split())
        if len(t) < 2:
            continue
        if sum(ch.isalpha() for ch in t) == 0:
            continue
        if t in seen:
            continue
        seen.add(t)
        clean.append(t)

    (ROOT / "docs/reference/pdf_extract_lines.txt").write_text(
        "\n".join(clean), encoding="utf-8"
    )


def main() -> None:
    if not XLSX.exists():
        raise SystemExit(f"Missing file: {XLSX}")
    if not PDF.exists():
        raise SystemExit(f"Missing file: {PDF}")
    extract_excel()
    extract_pdf_text_lines()
    print("Extraction completed:")
    print("- docs/reference/excel_extract/excel_structure.json")
    print("- docs/reference/excel_extract/formula_index.txt")
    print("- docs/reference/pdf_extract_lines.txt")


if __name__ == "__main__":
    main()
