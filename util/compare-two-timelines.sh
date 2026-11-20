#!/usr/bin/env bash
# Compare two timeline output directories.
#
# Usage:
#   util/compare-two-timelines.sh path/to/timelineA path/to/timelineB   (relative or absolute)
#
# Steps:
#   1. Compare the number (and relative paths) of *.hipo files
#   2. Compare ListOfTimelines.json, ignoring variable order
#   3. Dump every *.hipo with dump-timelines.groovy into
#        util/output/output-<basename A>/<subdir>/out_*.dat
#        util/output/output-<basename B>/<subdir>/out_*.dat
#   4. Diff the dumped out_*.dat files between A and B
#
# Logs are written to
#   util/output/logs/<basename A>_vs_<basename B>/
#
# Optional environment overrides:
#   RUN_GROOVY   path to run-groovy            (default: run-groovy on PATH, else <repo>/bin/run-groovy)
#   DUMP_GROOVY  path to dump-timelines.groovy (default: util/dump-timelines.groovy)

set -uo pipefail

usage() {
  echo "Usage: $(basename "$0") path/to/timelineA path/to/timelineB  (relative or absolute)" >&2
  exit 2
}

[ $# -eq 2 ] || usage

# ---------------------------------------------------------------------------
# inputs
# ---------------------------------------------------------------------------
for d in "$1" "$2"; do
  [ -d "$d" ] || { echo "ERROR: '$d' is not a directory" >&2; exit 2; }
done
# accept relative or absolute paths; resolve to absolute (symlinks resolved)
dirA=$(cd "$1" && pwd -P)
dirB=$(cd "$2" && pwd -P)
[ "$dirA" != "$dirB" ] || { echo "ERROR: both arguments point to the same directory" >&2; exit 2; }

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)

DUMP_GROOVY=${DUMP_GROOVY:-$SCRIPT_DIR/dump-timelines.groovy}
if [ -z "${RUN_GROOVY:-}" ]; then
  RUN_GROOVY=$(command -v run-groovy || echo "$SCRIPT_DIR/../bin/run-groovy")
fi
[ -f "$DUMP_GROOVY" ] || { echo "ERROR: dump script not found: $DUMP_GROOVY" >&2; exit 2; }
[ -x "$RUN_GROOVY" ]  || { echo "ERROR: run-groovy not found or not executable: $RUN_GROOVY" >&2; exit 2; }
DUMP_GROOVY=$(cd "$(dirname "$DUMP_GROOVY")" && pwd)/$(basename "$DUMP_GROOVY")
RUN_GROOVY=$(cd "$(dirname "$RUN_GROOVY")" && pwd)/$(basename "$RUN_GROOVY")

# output directory names (disambiguate if both basenames are equal)
baseA=$(basename "$dirA")
baseB=$(basename "$dirB")
if [ "$baseA" = "$baseB" ]; then
  baseA=${baseA}_1
  baseB=${baseB}_2
fi

OUT_DIR=$SCRIPT_DIR/output
OUT_A=$OUT_DIR/output-$baseA
OUT_B=$OUT_DIR/output-$baseB
LOG_DIR=$OUT_DIR/logs/${baseA}_vs_${baseB}

# start from a clean state for this pair only
rm -rf "$OUT_A" "$OUT_B" "$LOG_DIR"
mkdir -p "$OUT_A" "$OUT_B" "$LOG_DIR/dump-$baseA" "$LOG_DIR/dump-$baseB"

# copy everything printed to the terminal into a summary log
exec > >(tee "$LOG_DIR/summary.log") 2>&1

echo "A           : $dirA"
echo "B           : $dirB"
echo "run-groovy  : $RUN_GROOVY"
echo "dump script : $DUMP_GROOVY"
echo "output A    : $OUT_A"
echo "output B    : $OUT_B"
echo "logs        : $LOG_DIR"
echo

status=0

# list *.hipo files as sorted relative paths
list_hipo() {
  (cd "$1" && find . -type f -name '*.hipo' | sed 's|^\./||' | LC_ALL=C sort)
}

# ---------------------------------------------------------------------------
# 1. number of HIPO files
# ---------------------------------------------------------------------------
echo "=== [1/4] Number of HIPO files ==="
list_hipo "$dirA" > "$LOG_DIR/hipo_list_$baseA.txt"
list_hipo "$dirB" > "$LOG_DIR/hipo_list_$baseB.txt"
nA=$(grep -c . "$LOG_DIR/hipo_list_$baseA.txt")
nB=$(grep -c . "$LOG_DIR/hipo_list_$baseB.txt")
echo "$baseA : $nA"
echo "$baseB : $nB"

LC_ALL=C comm -23 "$LOG_DIR/hipo_list_$baseA.txt" "$LOG_DIR/hipo_list_$baseB.txt" > "$LOG_DIR/hipo_only_in_$baseA.txt"
LC_ALL=C comm -13 "$LOG_DIR/hipo_list_$baseA.txt" "$LOG_DIR/hipo_list_$baseB.txt" > "$LOG_DIR/hipo_only_in_$baseB.txt"
if [ -s "$LOG_DIR/hipo_only_in_$baseA.txt" ] || [ -s "$LOG_DIR/hipo_only_in_$baseB.txt" ]; then
  status=1
  if [ -s "$LOG_DIR/hipo_only_in_$baseA.txt" ]; then
    echo "Only in $baseA:"
    sed 's/^/  /' "$LOG_DIR/hipo_only_in_$baseA.txt"
  fi
  if [ -s "$LOG_DIR/hipo_only_in_$baseB.txt" ]; then
    echo "Only in $baseB:"
    sed 's/^/  /' "$LOG_DIR/hipo_only_in_$baseB.txt"
  fi
else
  echo "Same set of HIPO files."
fi
echo

# ---------------------------------------------------------------------------
# 2. ListOfTimelines.json (order independent)
# ---------------------------------------------------------------------------
echo "=== [2/4] ListOfTimelines.json ==="
jsonA=$dirA/ListOfTimelines.json
jsonB=$dirB/ListOfTimelines.json
if ! command -v jq > /dev/null; then
  echo "WARNING: jq not found, skipping"
  status=1
elif [ ! -f "$jsonA" ] || [ ! -f "$jsonB" ]; then
  [ -f "$jsonA" ] || echo "WARNING: missing $jsonA"
  [ -f "$jsonB" ] || echo "WARNING: missing $jsonB"
  status=1
else
  norm='map({subsystem, variables: (.variables | sort)}) | sort_by(.subsystem)'
  diff <(jq "$norm" "$jsonA") <(jq "$norm" "$jsonB") > "$LOG_DIR/ListOfTimelines.diff"
  rc=$?
  case $rc in
    0) echo "Identical (ignoring order)." ;;
    1) echo "DIFFERENT, see $LOG_DIR/ListOfTimelines.diff"
       cat "$LOG_DIR/ListOfTimelines.diff"
       status=1 ;;
    *) echo "ERROR: jq/diff failed (exit code $rc)"
       status=1 ;;
  esac
fi
echo

# ---------------------------------------------------------------------------
# 3. dump every HIPO file
# ---------------------------------------------------------------------------
# dump-timelines.groovy writes out_[timeline_name].dat into the CURRENT
# directory, so it runs inside the matching subsystem directory. The number
# of new out_*.dat files is counted per HIPO file; zero new files means the
# dump failed or its output name collided with an earlier one.
dump_all() {
  local src=$1 base=$2 dest=$3 logdest=$4
  local list=$LOG_DIR/hipo_list_$base.txt
  local failed=$LOG_DIR/dump_failed_$base.txt
  local n i=0 nfail=0 rel sub name workdir logfile ndat nbefore
  n=$(grep -c . "$list")
  : > "$failed"

  echo "--- $base ($n files) ---"
  while IFS= read -r rel; do
    [ -n "$rel" ] || continue
    i=$((i + 1))
    sub=$(dirname "$rel")
    name=$(basename "$rel" .hipo)
    workdir=$dest/$sub
    logfile=$logdest/$sub/$name.log
    mkdir -p "$workdir" "$(dirname "$logfile")"

    nbefore=$(find "$workdir" -maxdepth 1 -name 'out_*.dat' | wc -l)
    (cd "$workdir" && "$RUN_GROOVY" "$DUMP_GROOVY" "$src/$rel") > "$logfile" 2>&1 < /dev/null
    rc=$?
    ndat=$(( $(find "$workdir" -maxdepth 1 -name 'out_*.dat' | wc -l) - nbefore ))

    if [ $rc -ne 0 ] || [ "$ndat" -eq 0 ]; then
      nfail=$((nfail + 1))
      echo "$rel (exit $rc, $ndat dat files)" >> "$failed"
      printf '[%d/%d] %s  FAILED (exit %d, %d dat files), log: %s\n' "$i" "$n" "$rel" "$rc" "$ndat" "$logfile"
    else
      printf '[%d/%d] %s  ok (%d dat files)\n' "$i" "$n" "$rel" "$ndat"
    fi
  done < "$list"

  echo "$base: $((n - nfail))/$n succeeded"
  [ $nfail -eq 0 ] || { echo "Failed files listed in $failed"; return 1; }
  return 0
}

echo "=== [3/4] Dumping timelines ==="
dump_all "$dirA" "$baseA" "$OUT_A" "$LOG_DIR/dump-$baseA" || status=1
echo
dump_all "$dirB" "$baseB" "$OUT_B" "$LOG_DIR/dump-$baseB" || status=1
echo

# ---------------------------------------------------------------------------
# 4. diff dumped outputs
# ---------------------------------------------------------------------------
echo "=== [4/4] Diffing dumped outputs ==="
DIFF_DIR=$LOG_DIR/dat_diff
mkdir -p "$DIFF_DIR"
list_dat() {
  (cd "$1" && find . -type f -name 'out_*.dat' | sed 's|^\./||' | LC_ALL=C sort)
}
list_dat "$OUT_A" > "$LOG_DIR/dat_list_$baseA.txt"
list_dat "$OUT_B" > "$LOG_DIR/dat_list_$baseB.txt"
LC_ALL=C comm -23 "$LOG_DIR/dat_list_$baseA.txt" "$LOG_DIR/dat_list_$baseB.txt" > "$LOG_DIR/dat_only_in_$baseA.txt"
LC_ALL=C comm -13 "$LOG_DIR/dat_list_$baseA.txt" "$LOG_DIR/dat_list_$baseB.txt" > "$LOG_DIR/dat_only_in_$baseB.txt"
LC_ALL=C comm -12 "$LOG_DIR/dat_list_$baseA.txt" "$LOG_DIR/dat_list_$baseB.txt" > "$LOG_DIR/dat_common.txt"
: > "$LOG_DIR/dat_different.txt"

nsame=0
ndiff=0
while IFS= read -r rel; do
  [ -n "$rel" ] || continue
  if cmp -s "$OUT_A/$rel" "$OUT_B/$rel"; then
    nsame=$((nsame + 1))
  else
    ndiff=$((ndiff + 1))
    echo "$rel" >> "$LOG_DIR/dat_different.txt"
    mkdir -p "$DIFF_DIR/$(dirname "$rel")"
    diff "$OUT_A/$rel" "$OUT_B/$rel" > "$DIFF_DIR/${rel%.dat}.diff"
    echo "DIFFERENT : $rel  ($(grep -c '^[<>]' "$DIFF_DIR/${rel%.dat}.diff") changed lines)"
  fi
done < "$LOG_DIR/dat_common.txt"

nonlyA=$(grep -c . "$LOG_DIR/dat_only_in_$baseA.txt")
nonlyB=$(grep -c . "$LOG_DIR/dat_only_in_$baseB.txt")
[ "$nonlyA" -eq 0 ] || { echo "Only in $baseA:"; sed 's/^/  /' "$LOG_DIR/dat_only_in_$baseA.txt"; }
[ "$nonlyB" -eq 0 ] || { echo "Only in $baseB:"; sed 's/^/  /' "$LOG_DIR/dat_only_in_$baseB.txt"; }
echo "identical: $nsame, different: $ndiff, only in $baseA: $nonlyA, only in $baseB: $nonlyB"
[ "$ndiff" -gt 0 ] && echo "Per-file diffs in $DIFF_DIR"
[ $((ndiff + nonlyA + nonlyB)) -eq 0 ] || status=1
echo

# ---------------------------------------------------------------------------
# summary
# ---------------------------------------------------------------------------
echo "=== Summary ==="
echo "dat files in $OUT_A : $(find "$OUT_A" -name 'out_*.dat' | wc -l)"
echo "dat files in $OUT_B : $(find "$OUT_B" -name 'out_*.dat' | wc -l)"
if [ $status -eq 0 ]; then
  echo "No problems found in steps 1-4."
else
  echo "Problems found, check the messages above and $LOG_DIR"
fi

exit $status
