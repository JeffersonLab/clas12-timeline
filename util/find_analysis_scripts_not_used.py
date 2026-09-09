import os
import re
from glob import glob

default_timeline_dir = "{}/../src/main/java/org/jlab/clas/timeline/analysis/".format(os.path.dirname(os.path.abspath(__file__)))
default_analysis_file = "{}/../src/main/java/org/jlab/clas/timeline/run_analysis.groovy".format(os.path.dirname(os.path.abspath(__file__)))


def find_referenced_classes(analysis_file=default_analysis_file):
  '''
  parse `def engines = [ out_KEY: [ new class_name(...), ... ], ... ]`
  out of run_analysis.groovy, returning the set of every class name
  instantiated anywhere in the engines map (across all out_KEY groups,
  QADB included - this check only cares whether a script is referenced
  at all, not which output group it belongs to).
  '''
  with open(analysis_file, "r") as file:
    text = file.read()

  # strip line comments so commented-out engines aren't picked up
  text = re.sub(r"//.*", "", text)

  # isolate the `engines = [ ... ]` block by bracket-matching
  start = text.index("[", text.index("engines ="))
  depth = 0
  end = start
  for i in range(start, len(text)):
    if text[i] == "[":
      depth += 1
    elif text[i] == "]":
      depth -= 1
      if depth == 0:
        end = i
        break
  engines_block = text[start:end + 1]

  return set(re.findall(r"new\s+(\w+)\s*\(", engines_block))


def find_analysis_scripts_not_used(timeline_dir=default_timeline_dir, analysis_file=default_analysis_file):
  '''
  find every .groovy file under timeline_dir/*/ whose class name (the
  filename, minus extension) is never instantiated anywhere in
  run_analysis.groovy - i.e. dead scripts that exist on disk but aren't
  wired up to run (including ones commented out there, e.g.
  `//new dc_residuals_sec_rescut()`). returns a sorted list of paths,
  relative to timeline_dir.
  '''
  referenced_classes = find_referenced_classes(analysis_file)
  unused = []
  for path in sorted(glob("{}/*/*.groovy".format(timeline_dir))):
    class_name = os.path.splitext(os.path.basename(path))[0]
    if class_name not in referenced_classes:
      unused.append(os.path.relpath(path, timeline_dir))
  return unused


if __name__ == "__main__":
  for path in find_analysis_scripts_not_used():
    print(path)