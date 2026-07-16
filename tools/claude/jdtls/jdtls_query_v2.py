#!/usr/bin/env python3
"""
Simplified jdtls query tool for Claude Code (v2 - Server-aware)
Usage: jdtls_query_v2.py <command> [args...]

Can work with:
1. Persistent server (via jdtls_server.py) - FAST
2. Standalone mode (fallback) - SLOW but always works
"""

import sys
import json
import subprocess
from pathlib import Path

# Auto-detect paths relative to this script
SCRIPT_DIR = Path(__file__).parent.resolve()
CONFIG_PATH = SCRIPT_DIR / "jdtls_config.json"
SERVER_SCRIPT = SCRIPT_DIR / "jdtls_server.py"
JDTLS_CLIENT = SCRIPT_DIR / "jdtls_client.py"


def load_config():
    """Load configuration"""
    with open(CONFIG_PATH, 'r') as f:
        return json.load(f)


def find_project_for_file(file_path: Path, config: dict) -> str:
    """Find which project a file belongs to"""
    file_path = file_path.resolve()

    for project in config["projects"]:
        project_path = Path(project["path"]).resolve()
        try:
            file_path.relative_to(project_path)
            return project["name"]
        except ValueError:
            continue

    # Default to first project if file doesn't belong to any
    return config["projects"][0]["name"] if config["projects"] else None


def run_query_server(project_name: str, command: str, *args):
    """Run query using persistent server"""
    # Import here to use the manager
    sys.path.insert(0, str(SCRIPT_DIR))
    from jdtls_server import JDTLSServerManager

    manager = JDTLSServerManager(CONFIG_PATH)
    instance = manager.get_instance(project_name)

    if command == "symbols":
        return instance.workspace_symbols(args[0] if args else "")
    elif command == "doc-symbols":
        return instance.document_symbols(args[0])
    elif command == "definition":
        return instance.goto_definition(args[0], int(args[1]), int(args[2]))
    elif command == "references":
        return instance.find_references(args[0], int(args[1]), int(args[2]))
    else:
        raise ValueError(f"Unknown command: {command}")


def run_query_standalone(project_path: str, command: str, *args):
    """Run query in standalone mode (old method)"""
    cmd = ["python3", str(JDTLS_CLIENT), project_path, command] + list(args)
    result = subprocess.run(cmd, capture_output=True, text=True)

    if result.returncode != 0:
        raise Exception(result.stderr)

    return json.loads(result.stdout)


def run_query(command: str, *args, use_server: bool = True):
    """Run jdtls query (try server first, fallback to standalone)"""
    config = load_config()

    # Determine project
    if command in ["doc-symbols", "definition", "references"] and args:
        file_path = Path(args[0])
        project_name = find_project_for_file(file_path, config)
    else:
        # For workspace symbols, use first project or all
        project_name = config["projects"][0]["name"] if config["projects"] else None

    if not project_name:
        raise Exception("No project configured")

    # Try server mode first
    if use_server:
        try:
            return run_query_server(project_name, command, *args)
        except Exception as e:
            print(f"Server mode failed ({e}), falling back to standalone...", file=sys.stderr)

    # Fallback to standalone
    project_path = None
    for project in config["projects"]:
        if project["name"] == project_name:
            project_path = project["path"]
            break

    if not project_path:
        raise Exception(f"Project '{project_name}' not found")

    return run_query_standalone(project_path, command, *args)


def format_location(uri, range_obj):
    """Format location as file:line:col"""
    file_path = uri.replace("file://", "")
    line = range_obj["start"]["line"] + 1
    col = range_obj["start"]["character"] + 1
    return f"{file_path}:{line}:{col}"


def cmd_find(query, all_projects=False):
    """Find symbols matching query"""
    if all_projects:
        # Query all projects
        config = load_config()
        sys.path.insert(0, str(SCRIPT_DIR))
        from jdtls_server import JDTLSServerManager

        manager = JDTLSServerManager(CONFIG_PATH)
        results = manager.query_all_projects("symbols", query)
    else:
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

        # Show project if available
        project_info = f"[{item['_project']}] " if "_project" in item else ""

        if container:
            print(f"  {project_info}{name} ({kind}) in {container}")
        else:
            print(f"  {project_info}{name} ({kind})")
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
        print("Usage: jdtls_query_v2.py <command> [args...]")
        print("\nCommands:")
        print("  find <query> [--all]      - Find symbols by name (--all searches all projects)")
        print("  symbols <file>            - List symbols in file")
        print("  def <file> <line> <col>   - Find definition")
        print("  refs <file> <line> <col>  - Find references")
        print("\nExamples:")
        print("  jdtls_query_v2.py find ChangeNotification")
        print("  jdtls_query_v2.py find Manager --all")
        print("  jdtls_query_v2.py symbols src/main/.../Manager.java")
        print("  jdtls_query_v2.py def src/main/.../Manager.java 42 15")
        print("  jdtls_query_v2.py refs src/main/.../Manager.java 42 15")
        print("\nNote: Uses persistent server if available, otherwise falls back to standalone mode")
        sys.exit(1)

    command = sys.argv[1]

    try:
        if command == "find":
            if len(sys.argv) < 3:
                print("Usage: jdtls_query_v2.py find <query> [--all]")
                sys.exit(1)
            query = sys.argv[2]
            all_projects = "--all" in sys.argv
            cmd_find(query, all_projects)

        elif command == "symbols":
            if len(sys.argv) < 3:
                print("Usage: jdtls_query_v2.py symbols <file>")
                sys.exit(1)
            cmd_symbols(sys.argv[2])

        elif command == "def":
            if len(sys.argv) < 5:
                print("Usage: jdtls_query_v2.py def <file> <line> <col>")
                sys.exit(1)
            cmd_definition(sys.argv[2], sys.argv[3], sys.argv[4])

        elif command == "refs":
            if len(sys.argv) < 5:
                print("Usage: jdtls_query_v2.py refs <file> <line> <col>")
                sys.exit(1)
            cmd_references(sys.argv[2], sys.argv[3], sys.argv[4])

        else:
            print(f"Unknown command: {command}")
            sys.exit(1)

    except Exception as e:
        print(f"Error: {e}", file=sys.stderr)
        import traceback
        traceback.print_exc()
        sys.exit(1)


if __name__ == "__main__":
    main()
