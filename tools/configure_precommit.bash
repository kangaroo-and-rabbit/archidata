#!/bin/bash
set -euo pipefail

# Run from the repository root whatever the directory the script is called from
cd "$(dirname "${BASH_SOURCE[0]}")/.."

if ln -sf ../../tools/pre-commit .git/hooks/pre-commit; then
    echo "pre-commit hooks: [OK]"
else
    echo "pre-commit hooks: [ERROR] Cannot configure"
    exit 1
fi
