#!/usr/bin/env python3
"""
Simple LSP client for jdtls (Eclipse JDT Language Server)
Allows command-line interaction with the language server
"""

import json
import subprocess
import sys
import os
import threading
import time
from pathlib import Path

class JDTLSClient:
    def __init__(self, workspace_path, java_home="/usr/lib/jvm/java-25-openjdk"):
        self.workspace_path = Path(workspace_path).resolve()
        self.java_home = java_home
        self.process = None
        self.msg_id = 0
        self.responses = {}
        self.notifications = []
        self.lock = threading.Lock()

    def start(self):
        """Start the jdtls process"""
        workspace_data = f"/tmp/jdtls-workspace-{self.workspace_path.name}"
        os.makedirs(workspace_data, exist_ok=True)

        env = os.environ.copy()
        env["JAVA_HOME"] = self.java_home

        # Use the official jdtls Python launcher
        cmd = [
            "/usr/share/java/jdtls/bin/jdtls",
            "-data", workspace_data,
            "--jvm-arg=-Dlog.level=ERROR",
        ]

        self.process = subprocess.Popen(
            cmd,
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            env=env,
            cwd=str(self.workspace_path)
        )

        # Start reader thread
        self.reader_thread = threading.Thread(target=self._read_messages, daemon=True)
        self.reader_thread.start()

        print(f"jdtls started with PID {self.process.pid}", file=sys.stderr)

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
                        # Response to our request
                        self.responses[msg['id']] = msg
                    else:
                        # Notification from server
                        self.notifications.append(msg)

            except Exception as e:
                print(f"Error reading message: {e}", file=sys.stderr)
                break

    def _send_message(self, message):
        """Send a JSON-RPC message to jdtls"""
        content = json.dumps(message)
        content_bytes = content.encode('utf-8')
        headers = f"Content-Length: {len(content_bytes)}\r\n\r\n"

        self.process.stdin.write(headers.encode('utf-8'))
        self.process.stdin.write(content_bytes)
        self.process.stdin.flush()

    def _send_request(self, method, params=None):
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

        # Wait for response (with timeout)
        timeout = 30
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
            "rootUri": self.workspace_path.as_uri(),
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
                "workspaceFolders": [self.workspace_path.as_uri()],
            }
        }

        result = self._send_request("initialize", params)

        # Send initialized notification
        self._send_message({
            "jsonrpc": "2.0",
            "method": "initialized",
            "params": {}
        })

        print("LSP initialized", file=sys.stderr)
        return result

    def workspace_symbols(self, query=""):
        """Search for symbols in the workspace"""
        params = {"query": query}
        return self._send_request("workspace/symbol", params)

    def document_symbols(self, file_path):
        """Get symbols in a document"""
        file_uri = Path(file_path).resolve().as_uri()
        params = {
            "textDocument": {
                "uri": file_uri
            }
        }
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
        self._send_request("shutdown")
        self._send_message({"jsonrpc": "2.0", "method": "exit"})
        self.process.wait(timeout=5)

def main():
    if len(sys.argv) < 3:
        print("Usage: jdtls_client.py <workspace_path> <command> [args...]")
        print("\nCommands:")
        print("  symbols [query]              - Search workspace symbols")
        print("  doc-symbols <file>           - Get document symbols")
        print("  definition <file> <line> <col> - Go to definition")
        print("  references <file> <line> <col> - Find references")
        sys.exit(1)

    workspace = sys.argv[1]
    command = sys.argv[2]

    client = JDTLSClient(workspace)

    try:
        client.start()
        time.sleep(2)  # Give server time to start

        client.initialize()
        time.sleep(3)  # Give server time to index the project

        if command == "symbols":
            query = sys.argv[3] if len(sys.argv) > 3 else ""
            result = client.workspace_symbols(query)
            print(json.dumps(result, indent=2))

        elif command == "doc-symbols":
            if len(sys.argv) < 4:
                print("Error: file path required")
                sys.exit(1)
            file_path = sys.argv[3]
            result = client.document_symbols(file_path)
            print(json.dumps(result, indent=2))

        elif command == "definition":
            if len(sys.argv) < 6:
                print("Error: file, line, and column required")
                sys.exit(1)
            file_path = sys.argv[3]
            line = int(sys.argv[4])
            col = int(sys.argv[5])
            result = client.goto_definition(file_path, line, col)
            print(json.dumps(result, indent=2))

        elif command == "references":
            if len(sys.argv) < 6:
                print("Error: file, line, and column required")
                sys.exit(1)
            file_path = sys.argv[3]
            line = int(sys.argv[4])
            col = int(sys.argv[5])
            result = client.find_references(file_path, line, col)
            print(json.dumps(result, indent=2))

        else:
            print(f"Unknown command: {command}")
            sys.exit(1)

        client.shutdown()

    except KeyboardInterrupt:
        print("\nInterrupted", file=sys.stderr)
        if client.process:
            client.process.kill()
    except Exception as e:
        print(f"Error: {e}", file=sys.stderr)
        if client.process:
            client.process.kill()
        sys.exit(1)

if __name__ == "__main__":
    main()
