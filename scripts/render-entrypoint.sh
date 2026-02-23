#!/usr/bin/env sh
set -eu

# Render Postgres often provides URL as postgres://... .
# Spring Boot with explicit JDBC driver expects jdbc:postgresql://... .
if [ -n "${DB_URL:-}" ]; then
  case "$DB_URL" in
    postgres://*|postgresql://*)
      DB_URL="jdbc:${DB_URL}"
      export DB_URL
      ;;
  esac
fi

exec java -jar /app/app.jar
