#!/usr/bin/env bash

# usage
if [ $# -lt 1 ]; then
  echo """
  USAGE: $0  [RUN_DIRECTORY]...

  Check each [RUN_DIRECTORY] (which should be on /cache), comparing
  the files within to those from the corresponding tape stub directory 
  on /mss
  """
  exit 101
fi

source $(dirname $0)/environ.sh

# check inputs
cacheRunDirList=()
for inDir in $@; do
  [ ! -d $inDir ] && printError "directory '$inDir' does not exist" && continue
  inDir=$(realpath $inDir)
  [[ "$inDir" =~ /cache/ ]] && cacheRunDirList+=($inDir) || printError "directory '$inDir' is not on /cache"
done
if [ ${#cacheRunDirList[@]} -eq 0 ]; then
  printError "none of the specified directories are on cache"
  exit 100
fi

# preparation
mkdir -p tmp
jcacheList=tmp/jcacheSuggested.sh
echo "#!/usr/bin/env bash" > $jcacheList
chmod u+x $jcacheList

# convert cache directory => tape stub directory
cache2tape() { realpath $1 | sed 's;^.*/cache/;/mss/;'; }

# get list of unique parent directories on /cache, then cross check their subdirectories with tape
cacheParentDirList=($(
  for cacheRunDir in ${cacheRunDirList[@]}; do
    dirname $cacheRunDir
  done | sort -u
  ))
for cacheParentDir in ${cacheParentDirList[@]}; do
  tapeParentDir=$(cache2tape $cacheParentDir)
  [ ! -d $tapeParentDir ] && printError "tape stub directory does not exist: $tapeParentDir" && continue
  ls $cacheParentDir | grep -vi readme > tmp/cacheList
  ls $tapeParentDir  | grep -vi readme > tmp/tapeList
  diff -y --suppress-common-lines tmp/{cache,tape}List > tmp/diffList
  if [ -s tmp/diffList ]; then
    printWarning """there are differences between the list of runs in the cache and tape directories.
      cache dir: $cacheParentDir
      tape dir:  $tapeParentDir
    This means that one may have run subdirectories that the other does not.
    If the tape has directories that cache does not, consider making the cache directory
    and running \`jcache\`.

    Here is a summary of their differences:

     cache_dir                                                    tape_dir
===============                                              ==============
$(cat tmp/diffList)

      """
  fi
done

# check each run subdirectory
for cacheRunDir in ${cacheRunDirList[@]}; do
  tapeRunDir=$(cache2tape $cacheRunDir)
  if [ ! -d $cacheRunDir ]; then
    printWarning "cache directory does not exist: $cacheRunDir"
  elif [ ! -d $tapeRunDir ]; then
    printWarning "tape stub directory does not exist: $tapeRunDir"
  else
    cacheDirLS=$(ls $cacheRunDir | grep -vi readme)
    tapeDirLS=$(ls $tapeRunDir  | grep -vi readme)
    if [ -n "$(diff <(echo $cacheDirLS) <(echo $tapeDirLS))" ]; then
      printError """some files are on tape but not on cache for subdirectory $(basename $cacheRunDir)
      To see their differences, run the following command (cache on left, tape on right):
        diff -y --suppress-common-lines \\
          <(ls $cacheRunDir) \\
          <(ls $tapeRunDir)
      """
      echo "$TIMELINESRC/bin/jcacheRun.sh $(echo $cacheRunDir | sed 's;^.*/cache/;/cache/;')" >> $jcacheList
    fi
  fi
done

echo "DONE cross checking /cache with tape stubs"
if [ $(cat $jcacheList | wc -l) -gt 1 ]; then
  echo "Since differences were found, consider running \`jcache\`"
  echo "For convenience, the script $jcacheList was generated to help you"
fi
