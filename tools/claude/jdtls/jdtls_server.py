#!/usr/bin/env python3
"""
Persistent jdtls server daemon with multi-project support
Manages multiple jdtls instances and provides a unified interface
"""

import json
import subprocess
import sys
import os
import threading
import time
import socket
import signal
from pathlib import Path
from typing import Dict, List, Optional

class JDTLSInstance:
    """Manages a single jdtls instance for one project"""

    def __init__(self, project_name: str, project_path: Path, config: dict):
        self.project_name = project_name
        self.project_path = Path(project_path).resolve()
        self.config = config
        self.process = None
        self.msg_id = 0
        self.responses = {}
        self.notifications = []
        self.lock = threading.Lock()
        self.initialized = False

    def start(self):
        """Start the jdtls process for this project"""
        workspace_data = Path(self.config["workspace_base"]) / f"jdtls-{self.project_name}"
        workspace_data.mkdir(parents=True, exist_ok=True)

        env = os.environ.copy()
        env["JAVA_HOME"] = self.config["java_home"]

        cmd = [
            f"{self.config['jdtls_path']}/bin/jdtls",
            "-data", str(workspace_data),
            f"--jvm-arg=-Dlog.level={self.config['log_level']}",
        ]

        print(f"Starting jdtls for project '{self.project_name}'...", file=sys.stderr)

        self.process = subprocess.Popen(
            cmd,
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            env=env,
            cwd=str(self.project_path)
        )

        # Start reader thread
        self.reader_thread = threading.Thread(target=self._read_messages, daemon=True)
        self.reader_thread.start()

        print(f"jdtls started for '{self.project_name}' with PID {self.process.pid}", file=sys.stderr)

    def _read_messages(self):
        """Read messages from jdtls stdout"""
        while self.process and self.process.poll() is None:
            try:
                # Read headers
                headers = {}
                while True:
                    line = self.process.stdout.readline().decode('utf-8')
                    if line == '\r\n' or line == '\n':
                        break
                    if ':' in line:
                        key, value = line.split(':', 1)
                        headers[key.strip()] = value.strip()

                if 'Content-Length' not in headers:
                    continue

                # Read content
                content_length = int(headers['Content-Length'])
                content = self.process.stdout.read(content_length).decode('utf-8')
                msg = json.loads(content)

                with self.lock:
                    if 'id' in msg:
                        self.responses[msg['id']] = msg
                    else:
                        self.notifications.append(msg)

            except Exception as e:
                print(f"Error reading message for {self.project_name}: {e}", file=sys.stderr)
                break

    def _send_message(self, message):
        """Send a JSON-RPC message to jdtls"""
        content = json.dumps(message)
        content_bytes = content.encode('utf-8')
        headers = f"Content-Length: {len(content_bytes)}\r\n\r\n"

        self.process.stdin.write(headers.encode('utf-8'))
        self.process.stdin.write(content_bytes)
        self.process.stdin.flush()

    def _send_request(self, method, params=None, timeout=30):
        """Send a request and wait for response"""
        self.msg_id += 1
        msg_id = self.msg_id

        message = {
            "jsonrpc": "2.0",
            "id": msg_id,
            "method": method,
            "params": params or {}
        }

        self._send_message(message)

        # Wait for response
        start = time.time()
        while time.time() - start < timeout:
            with self.lock:
                if msg_id in self.responses:
                    response = self.responses.pop(msg_id)
                    if 'error' in response:
                        raise Exception(f"LSP Error: {response['error']}")
                    return response.get('result')
            time.sleep(0.1)

        raise TimeoutError(f"No response for request {msg_id}")

    def initialize(self):
        """Initialize the LSP session"""
        params = {
            "processId": os.getpid(),
            "rootUri": self.project_path.as_uri(),
            "capabilities": {
                "textDocument": {
                    "definition": {"linkSupport": True},
                    "references": {},
                    "documentSymbol": {},
                },
                "workspace": {
                    "symbol": {}
                }
            },
            "initializationOptions": {
                "bundles": [],
                "workspaceFolders": [self.project_path.as_uri()],
            }
        }

        result = self._send_request("initialize", params)

        # Send initialized notification
        self._send_message({
            "jsonrpc": "2.0",
            "method": "initialized",
            "params": {}
        })

        self.initialized = True
        print(f"LSP initialized for '{self.project_name}'", file=sys.stderr)
        return result

    def workspace_symbols(self, query=""):
        """Search for symbols in the workspace"""
        params = {"query": query}
        return self._send_request("workspace/symbol", params)

    def document_symbols(self, file_path):
        """Get symbols in a document"""
        file_uri = Path(file_path).resolve().as_uri()
        params = {"textDocument": {"uri": file_uri}}
        return self._send_request("textDocument/documentSymbol", params)

    def goto_definition(self, file_path, line, character):
        """Go to definition of symbol at position"""
        file_uri = Path(file_path).resolve().as_uri()
        params = {
            "textDocument": {"uri": file_uri},
            "position": {"line": line, "character": character}
        }
        return self._send_request("textDocument/definition", params)

    def find_references(self, file_path, line, character):
        """Find all references to symbol at position"""
        file_uri = Path(file_path).resolve().as_uri()
        params = {
            "textDocument": {"uri": file_uri},
            "position": {"line": line, "character": character},
            "context": {"includeDeclaration": True}
        }
        return self._send_request("textDocument/references", params)

    def shutdown(self):
        """Shutdown the language server"""
        if self.process and self.process.poll() is None:
            try:
                self._send_request("shutdown", timeout=5)
                self._send_message({"jsonrpc": "2.0", "method": "exit"})
                self.process.wait(timeout=5)
            except:
                self.process.kill()

    def is_alive(self):
        """Check if the process is still running"""
        return self.process and self.process.poll() is None


class JDTLSServerManager:
    """Manages multiple jdtls instances across projects"""

    def __init__(self, config_path: Path):
        self.config_path = config_path
        self.config = self._load_config()
        self.instances: Dict[str, JDTLSInstance] = {}
        self.running = False

    def _load_config(self) -> dict:
        """Load configuration from JSON file"""
        with open(self.config_path, 'r') as f:
            return json.load(f)

    def save_config(self):
        """Save current configuration to file"""
        with open(self.config_path, 'w') as f:
            json.dump(self.config, f, indent=2)

    def add_project(self, name: str, path: str, auto_start: bool = True):
        """Add a new project to configuration"""
        # Check if project already exists
        for project in self.config["projects"]:
            if project["name"] == name:
                print(f"Project '{name}' already exists, updating path...", file=sys.stderr)
                project["path"] = path
                project["auto_start"] = auto_start
                self.save_config()
                return

        self.config["projects"].append({
            "name": name,
            "path": path,
            "auto_start": auto_start
        })
        self.save_config()
        print(f"Added project '{name}' at {path}", file=sys.stderr)

    def remove_project(self, name: str):
        """Remove a project from configuration"""
        self.config["projects"] = [p for p in self.config["projects"] if p["name"] != name]
        self.save_config()

        if name in self.instances:
            self.instances[name].shutdown()
            del self.instances[name]

        print(f"Removed project '{name}'", file=sys.stderr)

    def list_projects(self):
        """List all configured projects"""
        return self.config["projects"]

    def start_project(self, project_name: str):
        """Start jdtls for a specific project"""
        # Find project config
        project_config = None
        for project in self.config["projects"]:
            if project["name"] == project_name:
                project_config = project
                break

        if not project_config:
            raise ValueError(f"Project '{project_name}' not found in configuration")

        # Check if already running
        if project_name in self.instances and self.instances[project_name].is_alive():
            print(f"jdtls for '{project_name}' is already running", file=sys.stderr)
            return self.instances[project_name]

        # Start new instance
        instance = JDTLSInstance(
            project_name,
            project_config["path"],
            self.config["server"]
        )
        instance.start()
        time.sleep(2)  # Give it time to start
        instance.initialize()
        time.sleep(3)  # Give it time to index

        self.instances[project_name] = instance
        return instance

    def start_all(self):
        """Start all projects marked with auto_start"""
        for project in self.config["projects"]:
            if project.get("auto_start", True):
                try:
                    self.start_project(project["name"])
                except Exception as e:
                    print(f"Failed to start '{project['name']}': {e}", file=sys.stderr)

    def stop_project(self, project_name: str):
        """Stop jdtls for a specific project"""
        if project_name in self.instances:
            self.instances[project_name].shutdown()
            del self.instances[project_name]
            print(f"Stopped jdtls for '{project_name}'", file=sys.stderr)

    def stop_all(self):
        """Stop all running jdtls instances"""
        for name in list(self.instances.keys()):
            self.stop_project(name)

    def get_instance(self, project_name: str) -> Optional[JDTLSInstance]:
        """Get jdtls instance for a project (start if not running)"""
        if project_name not in self.instances or not self.instances[project_name].is_alive():
            self.start_project(project_name)
        return self.instances.get(project_name)

    def find_project_for_file(self, file_path: Path) -> Optional[str]:
        """Find which project a file belongs to"""
        file_path = file_path.resolve()

        for project in self.config["projects"]:
            project_path = Path(project["path"]).resolve()
            try:
                file_path.relative_to(project_path)
                return project["name"]
            except ValueError:
                continue

        return None

    def query_all_projects(self, method: str, query: str) -> List[dict]:
        """Query all projects and aggregate results"""
        results = []

        for project_name in [p["name"] for p in self.config["projects"]]:
            try:
                instance = self.get_instance(project_name)
                if method == "symbols":
                    project_results = instance.workspace_symbols(query)
                    # Add project info to results
                    for result in project_results:
                        result["_project"] = project_name
                    results.extend(project_results)
            except Exception as e:
                print(f"Error querying '{project_name}': {e}", file=sys.stderr)

        return results


def main():
    """Main entry point for server management"""
    script_dir = Path(__file__).parent.resolve()
    config_path = script_dir / "jdtls_config.json"

    if len(sys.argv) < 2:
        print("Usage: jdtls_server.py <command> [args...]")
        print("\nCommands:")
        print("  start [project]      - Start server(s) (all or specific project)")
        print("  stop [project]       - Stop server(s) (all or specific project)")
        print("  status               - Show server status")
        print("  add <name> <path>    - Add a new project")
        print("  remove <name>        - Remove a project")
        print("  list                 - List all projects")
        sys.exit(1)

    command = sys.argv[1]
    manager = JDTLSServerManager(config_path)

    if command == "start":
        if len(sys.argv) > 2:
            project_name = sys.argv[2]
            manager.start_project(project_name)
        else:
            manager.start_all()
        print("Server(s) started. Keep this process running.", file=sys.stderr)

        # Keep running
        try:
            while True:
                time.sleep(1)
        except KeyboardInterrupt:
            print("\nShutting down...", file=sys.stderr)
            manager.stop_all()

    elif command == "stop":
        if len(sys.argv) > 2:
            project_name = sys.argv[2]
            manager.stop_project(project_name)
        else:
            manager.stop_all()

    elif command == "status":
        print("Configured projects:")
        for project in manager.list_projects():
            running = project["name"] in manager.instances and manager.instances[project["name"]].is_alive()
            status = "RUNNING" if running else "STOPPED"
            auto = "auto-start" if project.get("auto_start", True) else ""
            print(f"  {project['name']}: {status} {auto}")
            print(f"    Path: {project['path']}")

    elif command == "add":
        if len(sys.argv) < 4:
            print("Usage: jdtls_server.py add <name> <path>")
            sys.exit(1)
        name = sys.argv[2]
        path = sys.argv[3]
        manager.add_project(name, path)

    elif command == "remove":
        if len(sys.argv) < 3:
            print("Usage: jdtls_server.py remove <name>")
            sys.exit(1)
        name = sys.argv[2]
        manager.remove_project(name)

    elif command == "list":
        for project in manager.list_projects():
            auto = "(auto-start)" if project.get("auto_start", True) else ""
            print(f"{project['name']}: {project['path']} {auto}")

    else:
        print(f"Unknown command: {command}")
        sys.exit(1)


if __name__ == "__main__":
    main()
