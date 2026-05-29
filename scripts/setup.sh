#!/usr/bin/env bash
# One-time per-clone setup:
#   1. Point git at the in-repo hooks directory.
#   2. Create a local patterns file the pre-commit hook reads. The file lives
#      inside .git/ so it is never tracked by git and never shared between
#      contributors — each developer's "don't leak this" list stays private
#      to their own clone.
set -euo pipefail

git config core.hooksPath .githooks
chmod +x .githooks/pre-commit

git_dir="$(git rev-parse --git-dir)"
patterns_file="${git_dir}/identity-block-patterns"
if [[ ! -f "${patterns_file}" ]]; then
  cat > "${patterns_file}" <<'EOF'
# One pattern per line. Lines starting with # are comments.
# The pre-commit hook will refuse commits whose staged additions contain any
# of these patterns as whole words (case-insensitive).
#
# Add words you do NOT want to appear in public commits — your name, your
# employer, project codenames, etc. Examples:
#
#   alice
#   acme-corp
EOF
  echo "Created ${patterns_file}."
  echo "Edit it to add words you don't want to appear in commits."
else
  echo "Patterns file already exists at ${patterns_file}."
fi

echo "Configured core.hooksPath -> .githooks"
