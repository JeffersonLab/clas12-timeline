#!/usr/bin/env bash
set -e
set -u
source $(dirname $0)/../libexec/environ.sh
old='run-monitoring.sh'
new='thyme histogram'
printWarning "$old: deprecated program name; it will be removed in a future release"
printWarning "Use '$new' instead."
sleep 3
exec $new "$@"
