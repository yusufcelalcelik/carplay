#!/usr/bin/env python3
"""Read-only ADB hardware inventory. No root, settings writes or firmware operations."""
import argparse
import json
import re
import subprocess
import sys
from pathlib import Path

PROPERTIES = (
    "ro.product.manufacturer", "ro.product.brand", "ro.product.model", "ro.product.device",
    "ro.product.board", "ro.board.platform", "ro.hardware", "ro.product.cpu.abilist",
    "ro.build.version.release", "ro.build.version.sdk", "ro.build.display.id",
    "ro.build.fingerprint", "sys.usb.config", "sys.mcu_version", "ro.mcu.version",
    "persist.sys.mcu.version",
)
VENDOR_PACKAGE = re.compile(r"^package:(?:com\.(?:microntek|syu|winca|nwd|hct|ts|zjinnova|zj)\.)", re.I)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--serial", required=True, help="Actual receiver's ADB serial; never selected automatically")
    parser.add_argument("--adb", default="adb", help="Path to adb executable")
    parser.add_argument("--output", type=Path, help="Write a new local JSON file; existing files are not overwritten")
    args = parser.parse_args()

    def run(*command):
        try:
            result = subprocess.run([args.adb, "-s", args.serial, *command],
                                    capture_output=True, text=True, timeout=10, check=False)
            return {"ok": result.returncode == 0,
                    "text": result.stdout.replace(args.serial, "<selected-device>").strip(),
                    "error": result.stderr.replace(args.serial, "<selected-device>").strip()}
        except FileNotFoundError:
            parser.error("adb not found; pass --adb PATH_TO_ADB")
        except subprocess.TimeoutExpired:
            return {"ok": False, "text": "", "error": "Read timed out after 10 seconds"}

    state = run("get-state")
    if not state["ok"] or state["text"] != "device":
        parser.error("The selected receiver is not available/authorized over ADB")

    properties = {}
    for name in PROPERTIES:
        value = run("shell", "getprop", name)
        properties[name] = value["text"] if value["ok"] and value["text"] else None
    cpu = run("shell", "cat", "/proc/cpuinfo")
    cpu["text"] = "\n".join(line for line in cpu["text"].splitlines()
                            if re.match(r"^(Processor|model name|Hardware|CPU architecture|CPU implementer|CPU part)\s*:", line))
    packages = run("shell", "pm", "list", "packages")
    packages["text"] = "\n".join(line for line in packages["text"].splitlines() if VENDOR_PACKAGE.match(line))
    inputs = run("shell", "getevent", "-lp")
    inputs["text"] = "\n".join(line for line in inputs["text"].splitlines()
                               if not re.search(r"\b(serial|uniq|unique)\b", line, re.I))
    inputs["truncated"] = len(inputs["text"]) > 16000
    inputs["text"] = inputs["text"][:16000]
    report = {"schema": 1, "mode": "read-only", "properties": properties,
              "cpu": cpu, "vendor_packages": packages, "input_inventory": inputs,
              "note": "No model inference. Missing input events or MCU properties do not prove missing hardware."}
    text = json.dumps(report, ensure_ascii=False, indent=2) + "\n"
    if args.output:
        try:
            with args.output.open("x", encoding="utf-8") as output:
                output.write(text)
        except FileExistsError:
            parser.error("Output already exists; choose a new report filename")
        except OSError:
            parser.error("Cannot write the output file; check its directory and permissions")
        print("Receiver report written locally; review before sharing.")
    else:
        sys.stdout.write(text)


if __name__ == "__main__":
    main()
