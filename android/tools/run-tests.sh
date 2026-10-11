#!/usr/bin/env bash
# JVM tests for the pure logic ported from the PHP (no Android device needed).
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
[ -d build/classes ] || bash build.sh >/dev/null
mkdir -p build/test
javac -cp build/classes -d build/test tools/LogicTest.java
java -cp build/classes:build/test LogicTest
