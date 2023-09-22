#!/bin/bash
# jcache a directory of DST files

if [ $# -ne 1 ]; then
  echo "USAGE $0 [cache directory of DST stubs]" >&2
  exit 101
fi
cachedir=$1
ls $(echo $cachedir | sed 's/^\/cache/\/mss/g')/*.hipo > tmp/jlist.txt
jcache get $(cat tmp/jlist.txt)
