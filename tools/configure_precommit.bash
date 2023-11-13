#!/bin/bash
set +e
ln -sf ../../tools/pre-commit-hook .git/hooks/pre-commit
if [[ 0 != $? ]]; then
    echo "pre-commit hooks: [ERROR] Cannot configure"
    exit 1
else
    echo "pre-commit hooks: [OK]"
fi
