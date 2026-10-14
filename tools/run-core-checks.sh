#!/usr/bin/env bash
# Requires JDK 17+ and Kotlin 2.2.10+ on PATH. Does not download dependencies.
set -euo pipefail
cd "$(dirname "$0")/.."
temp_dir=$(mktemp -d)
trap 'rm -rf "$temp_dir"' EXIT
kotlinc app/src/main/java/com/example/util/HashUtil.kt \
  app/src/test/java/com/example/util/HashRegressionChecks.kt \
  app/src/main/java/com/example/util/LockedPackageCache.kt \
  app/src/test/java/com/example/util/CacheRegressionChecks.kt \
  app/src/main/java/com/example/util/ReleaseVersion.kt \
  app/src/test/java/com/example/util/VersionRegressionChecks.kt \
  app/src/main/java/com/example/util/CooldownPolicy.kt \
  app/src/test/java/com/example/util/CooldownRegressionChecks.kt \
  app/src/main/java/com/example/util/ForegroundTarget.kt \
  app/src/main/java/com/example/util/AuthRequestGeneration.kt \
  app/src/main/java/com/example/util/AuthAttemptLimiter.kt \
  app/src/test/java/com/example/util/LifecycleRegressionChecks.kt \
  tools/CoreCheckRunner.kt -include-runtime -d "$temp_dir/checks.jar"
java -jar "$temp_dir/checks.jar"
