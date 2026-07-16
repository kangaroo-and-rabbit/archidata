#!/usr/bin/env python3
"""
Simplified jdtls query tool for Claude Code
Usage: jdtls_query.py <command> [args...]
"""

import sys
import json
import subprocess
from pathlib import Path

# Auto-detect paths relative to this script
SCRIPT_DIR = Path(__file__).parent.resolve()
ARCHIDATA_PATH = SCRIPT_DIR.parent.parent.parent
JDTLS_CLIENT = SCRIPT_DIR / "jdtls_client.py"

def run_query(command, *args):
    """Run jdtls query and return parsed JSON"""
    cmd = ["python3", str(JDTLS_CLIENT), str(ARCHIDATA_PATH), command] + list(args)
    result = subprocess.run(cmd, capture_output=True, text=True)

    if result.returncode != 0:
        print(f"Error: {result.stderr}", file=sys.stderr)
        sys.exit(1)

    try:
        return json.loads(result.stdout)
    except json.JSONDecodeError:
        print(f"Failed to parse JSON output", file=sys.stderr)
        print(result.stdout, file=sys.stderr)
        sys.exit(1)

def format_location(uri, range_obj):
    """Format location as file:line:col"""
    file_path = uri.replace("file://", "")
    line = range_obj["start"]["line"] + 1
    col = range_obj["start"]["character"] + 1
    return f"{file_path}:{line}:{col}"

def cmd_find(query):
    """Find symbols matching query"""
    results = run_query("symbols", query)

    if not results:
        print(f"No symbols found matching '{query}'")
        return

    print(f"Found {len(results)} symbol(s) matching '{query}':\n")
    for item in results:
        name = item["name"]
        container = item.get("containerName", "")
        location = format_location(item["location"]["uri"], item["location"]["range"])
        kind_map = {
            1: "File", 2: "Module", 3: "Namespace", 4: "Package", 5: "Class",
            6: "Method", 7: "Property", 8: "Field", 9: "Constructor", 10: "Enum",
            11: "Interface", 12: "Function", 13: "Variable", 14: "Constant"
        }
        kind = kind_map.get(item["kind"], str(item["kind"]))

        if container:
            print(f"  {name} ({kind}) in {container}")
        else:
            print(f"  {name} ({kind})")
        print(f"    {location}\n")

def cmd_symbols(file_path):
    """List symbols in a file"""
    results = run_query("doc-symbols", file_path)

    if not results:
        print(f"No symbols found in {file_path}")
        return

    print(f"Symbols in {file_path}:\n")
    for item in results:
        name = item["name"]
        line = item["location"]["range"]["start"]["line"] + 1
        kind_map = {
            5: "Class", 6: "Method", 8: "Field", 9: "Constructor",
            10: "Enum", 11: "Interface", 12: "Function", 14: "Constant"
        }
        kind = kind_map.get(item["kind"], str(item["kind"]))
        print(f"  {name} ({kind}) at line {line}")

def cmd_definition(file_path, line, col):
    """Find definition of symbol"""
    results = run_query("definition", file_path, str(int(line) - 1), col)

    if not results:
        print(f"No definition found")
        return

    for item in results:
        location = format_location(item["uri"], item["range"])
        print(f"Definition: {location}")

def cmd_references(file_path, line, col):
    """Find references to symbol"""
    results = run_query("references", file_path, str(int(line) - 1), col)

    if not results:
        print(f"No references found")
        return

    print(f"Found {len(results)} reference(s):\n")
    for item in results:
        location = format_location(item["uri"], item["range"])
        print(f"  {location}")

def main():
    if len(sys.argv) < 2:
        print("Usage: jdtls_query.py <command> [args...]")
        print("\nCommands:")
        print("  find <query>              - Find symbols by name")
        print("  symbols <file>            - List symbols in file")
        print("  def <file> <line> <col>   - Find definition")
        print("  refs <file> <line> <col>  - Find references")
        print("\nExamples:")
        print("  jdtls_query.py find ChangeNotification")
        print("  jdtls_query.py symbols src/main/.../Manager.java")
        print("  jdtls_query.py def src/main/.../Manager.java 42 15")
        print("  jdtls_query.py refs src/main/.../Manager.java 42 15")
        sys.exit(1)

    command = sys.argv[1]

    try:
        if command == "find":
            if len(sys.argv) < 3:
                print("Usage: jdtls_query.py find <query>")
                sys.exit(1)
            cmd_find(sys.argv[2])

        elif command == "symbols":
            if len(sys.argv) < 3:
                print("Usage: jdtls_query.py symbols <file>")
                sys.exit(1)
            cmd_symbols(sys.argv[2])

        elif command == "def":
            if len(sys.argv) < 5:
                print("Usage: jdtls_query.py def <file> <line> <col>")
                sys.exit(1)
            cmd_definition(sys.argv[2], sys.argv[3], sys.argv[4])

        elif command == "refs":
            if len(sys.argv) < 5:
                print("Usage: jdtls_query.py refs <file> <line> <col>")
                sys.exit(1)
            cmd_references(sys.argv[2], sys.argv[3], sys.argv[4])

        else:
            print(f"Unknown command: {command}")
            sys.exit(1)

    except Exception as e:
        print(f"Error: {e}", file=sys.stderr)
        sys.exit(1)

if __name__ == "__main__":
    main()
