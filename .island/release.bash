#!/bin/bash

mvn versions:set -DnewVersion=$(cat version.txt)

