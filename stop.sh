#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
for name in frontend backend; do
  file=".run/$name.pid"
  [[ -f "$file" ]] || continue
  pid=$(<"$file")
  if [[ "$pid" =~ ^[0-9]+$ ]] && (( pid > 1 )); then
    kill -TERM -- "-$pid" 2>/dev/null || true
  fi
  rm -f "$file"
done
echo 'Project stop requested. MySQL and phpMyAdmin remain running.'
echo 'To stop database services too: sudo systemctl stop httpd php-fpm mysqld'
