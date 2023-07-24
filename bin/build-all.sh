#!/bin/bash

set -e

d="$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"

cd $d/../monitoring
mvn package
cd -
cd $d/../detectors
mvn package
cd -
