#!/bin/bash
set -ev

# Help: https://docs.travis-ci.com/user/job-lifecycle/

set +e # Don't exit because yarn audit returned non-zero exit code
./gradlew build --info
build_results=$?
set -e
echo "Build exit code $build_results"
test $build_results -g 0 && echo "Build failed!" && exit $build_results

set +e # Don't exit because yarn audit returned non-zero exit code
./gradlew test jacocoTestReport --info
unit_test_results=$?
set -e
echo "Unit test exit code $unit_test_results"
test $unit_test_results -g 0 && echo "Unit tests failed!" && exit $unit_test_results

set +e # Don't exit because yarn audit returned non-zero exit code
./gradlew sonarqube --info
code_quality_results=$?
set -e
echo "Code quality exit code $code_quality_results"
test $code_quality_results -g 0 && echo "Code quality check failed!" && exit $code_quality_results
