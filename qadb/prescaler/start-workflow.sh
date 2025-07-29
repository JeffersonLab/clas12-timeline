#!/usr/bin/env bash
set -e
set -u
if [ $# -lt 1 ]; then
  echo "USAGE: $0 [JSON_FILES]..."
  exit 2
fi

echo "===== CANCEL WORKFLOWS ====="
for j in "$@"; do
  w=$(basename $j .json)
  swif2 list | grep $w && swif2 cancel -delete -workflow $w
done

echo "===== IMPORT WORKFLOWS ====="
for j in "$@"; do
  swif2 import -file $j
done

echo "===== START WORKFLOWS ====="
for j in "$@"; do
  w=$(basename $j .json)
  swif2 run -workflow $w
done
