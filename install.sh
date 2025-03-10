#!/usr/bin/env bash

if [ $# -ge 1 ]; then
  if [ "$1" = "clean" ]; then
    mvn clean
  else
    echo "ERROR: unknown argument '$1'" >&2
    exit 1
  fi
fi

mvn install dependency:copy-dependencies
