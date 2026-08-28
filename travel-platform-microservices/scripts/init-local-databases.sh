#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPO_ROOT="$(cd "$ROOT/.." && pwd)"
MYSQL_URL="${MYSQL_URL:-jdbc:mysql://localhost:3306/?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false}"
MYSQL_USERNAME="${MYSQL_USERNAME:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-123456}"
JDBC_JAR="${JDBC_JAR:-$HOME/.m2/repository/com/mysql/mysql-connector-j/8.3.0/mysql-connector-j-8.3.0.jar}"
BUILD_DIR="$ROOT/build/sql-runner"
SCHEMA_SQL="$REPO_ROOT/travel-platform-server/src/main/resources/sql/schema.sql"
DATA_SQL="$REPO_ROOT/travel-platform-server/src/main/resources/sql/data-demo.sql"

if [ ! -f "$SCHEMA_SQL" ]; then
  echo "Schema SQL not found: $SCHEMA_SQL" >&2
  exit 1
fi

if [ ! -f "$DATA_SQL" ]; then
  echo "Demo data SQL not found: $DATA_SQL" >&2
  exit 1
fi

if [ ! -f "$JDBC_JAR" ]; then
  mvn -q -pl product-service -am dependency:go-offline
fi

mkdir -p "$BUILD_DIR"
javac -encoding UTF-8 -cp "$JDBC_JAR" -d "$BUILD_DIR" "$ROOT/tools/SqlRunner.java"
java -cp "$BUILD_DIR:$JDBC_JAR" SqlRunner "$MYSQL_URL" "$MYSQL_USERNAME" "$MYSQL_PASSWORD" "$SCHEMA_SQL" "$DATA_SQL"
