#!/bin/bash
version_file="version.txt"
# Never take a pre-release (alpha, beta, milestone, release candidate) of a dependency
IGNORE_PRERELEASE='(?i).*[-.](alpha|beta|m|rc|cr|b)[-.]?[0-9]*$'

# update the Maven version number
mvn versions:set -DnewVersion=$(sed 's/dev/SNAPSHOT/g' $version_file)
if grep -q "DEV" "$version_file"; then
    # update all versions release of dependency
    mvn versions:use-latest-releases "-Dmaven.version.ignore=$IGNORE_PRERELEASE"
    # update our manage dependency as snapshoot
    mvn versions:use-latest-versions -Dincludes=kangaroo-and-rabbit
else
    # update our manage dependency as release (must be done before)
    mvn versions:use-latest-releases -Dincludes=kangaroo-and-rabbit "-Dmaven.version.ignore=$IGNORE_PRERELEASE"
fi
