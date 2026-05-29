#!/usr/bin/env bash
set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:?请设置 MYSQL_PASSWORD 环境变量}"
MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
SCALE="${SCALE:-medium}"

mysql_cmd() {
  mysql -h "$MYSQL_HOST" -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" "$@"
}

echo "==> [1/4] Create database chatbi_bench"
mysql_cmd < "$DIR/00-init.sql"

echo "==> [2/4] Apply schema (32 tables)"
mysql_cmd chatbi_bench < "$DIR/01-schema.sql"

echo "==> [3/4] Seed dimensions"
mysql_cmd chatbi_bench < "$DIR/02-seed-dimensions.sql"

echo "==> [4/4] Generate & import facts (scale=$SCALE)"
python3 "$DIR/generate_facts.py" --scale "$SCALE"
mysql_cmd chatbi_bench < "$DIR/03-seed-facts.sql"

echo ""
echo "Done! Database: chatbi_bench"
echo "Connect in ChatBI: host=$MYSQL_HOST, db=chatbi_bench, user=$MYSQL_USER"
