#!/bin/bash
# Helper functions for jdtls client

# Auto-detect paths relative to this script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ARCHIDATA_PATH="$(cd "$SCRIPT_DIR/../../.." && pwd)"
JDTLS_CLIENT="$SCRIPT_DIR/jdtls_client.py"

# Search for a symbol in the workspace
jdtls_find_symbol() {
    local query="$1"
    python3 "$JDTLS_CLIENT" "$ARCHIDATA_PATH" symbols "$query" 2>/dev/null | jq -r '.[] | "\(.name) (\(.containerName)) - \(.location.uri | sub("file://"; "")):\(.location.range.start.line + 1)"'
}

# List all symbols in a file
jdtls_list_file_symbols() {
    local file="$1"
    python3 "$JDTLS_CLIENT" "$ARCHIDATA_PATH" doc-symbols "$file" 2>/dev/null | jq -r '.[] | "\(.name) (\(.kind)) - line \(.location.range.start.line + 1)"'
}

# Find definition of symbol at position
jdtls_goto_definition() {
    local file="$1"
    local line="$2"
    local col="$3"
    python3 "$JDTLS_CLIENT" "$ARCHIDATA_PATH" definition "$file" "$line" "$col" 2>/dev/null | jq -r '.[0] | "\(.uri | sub("file://"; "")):\(.range.start.line + 1):\(.range.start.character + 1)"'
}

# Find all references to symbol at position
jdtls_find_references() {
    local file="$1"
    local line="$2"
    local col="$3"
    python3 "$JDTLS_CLIENT" "$ARCHIDATA_PATH" references "$file" "$line" "$col" 2>/dev/null | jq -r '.[] | "\(.uri | sub("file://"; "")):\(.range.start.line + 1):\(.range.start.character + 1)"'
}

# Export functions
export -f jdtls_find_symbol
export -f jdtls_list_file_symbols
export -f jdtls_goto_definition
export -f jdtls_find_references
