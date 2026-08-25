import os
import re
import pandas as pd
from glob import glob

class timeline_hists:
  '''
  a class to identify histogram variables used by timeline scripts.
  '''
  default_timeline_dir = "{}/../src/main/java/org/jlab/clas/timeline/analysis/".format(os.path.dirname(os.path.abspath(__file__)))
  default_histogram_dir = "{}/../src/main/java/org/jlab/clas/timeline/histograms/".format(os.path.dirname(os.path.abspath(__file__)))
  default_analysis_file = "{}/../src/main/java/org/jlab/clas/timeline/run_analysis.groovy".format(os.path.dirname(os.path.abspath(__file__)))

  def __init__(self, subset = 'all', raw = False):
    self.raw = raw
    self.construct_analysis_engines()
    self.construct_histogram_var_list()
    self.rows = []
    for key in self.engines:
      self.rows.extend(self.find_histogram_candidates(key))
    columns = ["timeline script", "timeline line number",
               "histogram script", "histogram line number"]
    if self.raw:
      columns += ["timeline line", "histogram line"]
    self.df = pd.DataFrame(self.rows, columns=columns)
    self.df["timeline line number"] = self.df["timeline line number"].astype("Int64")
    self.df["histogram line number"] = self.df["histogram line number"].astype("Int64")

  def construct_analysis_engines(self):
    '''
    parse `def engines = [ out_KEY: [ new class_name(...), ... ], ... ]`
    out of run_analysis.groovy, filling self.engines as
    { out_KEY: [class_name, class_name, ...], ... }
    '''
    self.engines = {}  # fresh per-instance dict (avoid mutable class attribute)
    with open(self.default_analysis_file, "r") as file:
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

    # within that block, find each "out_KEY: [ ... ]" entry and, within it,
    # every "new class_name(" instantiation
    for match in re.finditer(r"(out_\w+)\s*:\s*\[", engines_block):
      key = match.group(1)
      sub_start = match.end() - 1  # index of this entry's '['
      depth = 0
      sub_end = sub_start
      for i in range(sub_start, len(engines_block)):
        if engines_block[i] == "[":
          depth += 1
        elif engines_block[i] == "]":
          depth -= 1
          if depth == 0:
            sub_end = i
            break
      sub_block = engines_block[sub_start:sub_end + 1]
      class_names = re.findall(r"new\s+(\w+)\s*\(", sub_block)
      self.engines[key] = class_names
    del self.engines["out_QADB"]
    for key in list(self.engines.keys()):
      new_key = key[4:]
      self.engines[new_key] = self.engines.pop(key)
    self.engines['GeneralMon'] = self.engines.pop('monitor')
    self.engines['DCandFTOF'] = self.engines.pop('TOF')

  def _top_level_split_plus(self, text):
    '''split text on '+' at paren/bracket depth 0, outside quotes'''
    parts = []
    depth = 0
    in_str = None
    cur = []
    i = 0
    while i < len(text):
      c = text[i]
      if in_str:
        cur.append(c)
        if c == '\\':
          i += 1
          if i < len(text):
            cur.append(text[i])
        elif c == in_str:
          in_str = None
      else:
        if c in ('"', "'"):
          in_str = c
          cur.append(c)
        elif c in '([{':
          depth += 1
          cur.append(c)
        elif c in ')]}':
          depth -= 1
          cur.append(c)
        elif c == '+' and depth == 0:
          parts.append(''.join(cur))
          cur = []
        else:
          cur.append(c)
      i += 1
    parts.append(''.join(cur))
    return [p.strip() for p in parts]

  def name_expr_to_skeleton(self, expr):
    '''
    turn a name-generating expression - a groovy getObject(...) argument,
    or the first argument of a java new H1F(...)/String.format(...) call -
    into a "skeleton" string where every variable piece (${...}, $var,
    '...'+var+, %d/%02d/%s, ...) is collapsed to a single '#' wildcard.
    Two histogram names refer to the same histogram iff their skeletons
    match (see skeletons_match).
    '''
    expr = expr.strip()

    # String.format("fmt", ...) -> just take the format string itself
    fmt_match = re.match(r"String\.format\s*\(\s*['\"](.*?)['\"]", expr)
    if fmt_match:
      return self._literal_to_skeleton(fmt_match.group(1))

    # top-level '+' concatenation: recurse on each piece, then join -
    # this is what lets "a" + var + "b" + var2 correctly become "a#b#"
    # instead of corrupting quote-pairing with a single whole-string regex
    parts = self._top_level_split_plus(expr)
    if len(parts) > 1:
      skeleton = ''.join(self.name_expr_to_skeleton(p) for p in parts)
      return re.sub(r"#+", "#", skeleton)

    # single literal
    if len(expr) >= 2 and expr[0] == expr[-1] and expr[0] in ('"', "'"):
      return self._literal_to_skeleton(expr[1:-1])

    # bare variable / arbitrary expression (e.g. sector, (it+1), sl_string)
    return '#'

  def _literal_to_skeleton(self, s):
    '''
    the *content* of a quoted string (already unquoted): drop a leading
    '/DIR/' path component if present, then collapse GString interpolation
    and printf-style format specifiers to '#'.
    '''
    s = re.sub(r"^/[A-Za-z0-9_]+/", "", s)             # leading '/DIR/'
    s = re.sub(r"\$\{[^}]*\}", "#", s)                  # ${expr}
    s = re.sub(r"\$[A-Za-z_][A-Za-z0-9_]*", "#", s)     # $var
    s = re.sub(r"%[-+0# ]?\d*(?:\.\d+)?[dsfegxX]", "#", s)  # %d, %02d, %s, ...
    return s


  def skeletons_match(self, skel_a, skel_b):
    '''
    two skeletons "match" (probably refer to the same histogram) if they're
    identical, or if one's wildcard regex fullmatches a concrete sample of
    the other - this covers the case where one side has a real digit
    (groovy's 'SectorCombination1') and the other still has a '#'
    (java's "SectorCombination%d").
    '''
    if skel_a == skel_b:
      return True

    def to_regex(skel):
      parts = skel.split('#')
      return '^' + '.*?'.join(re.escape(p) for p in parts) + '$'

    def to_sample(skel):
      return skel.replace('#', '9')

    if re.match(to_regex(skel_a), to_sample(skel_b)):
      return True
    if re.match(to_regex(skel_b), to_sample(skel_a)):
      return True
    return False

  def resolve_variable_expr(self, java_lines, line_idx, var_name, window=40):
    '''
    when a H1F/H2F/H3F name argument is a bare variable (e.g. `histname`
    in `new H1F(histname, histitle, ...)`), the actual name-building
    expression usually lives a few lines above, as `histname = <expr>;`.
    scan backward from line_idx (0-based, exclusive) up to `window` lines
    for the nearest such assignment and return its RHS. returns None if
    none is found.
    '''
    pattern = re.compile(r"\b" + re.escape(var_name) + r"\s*=\s*(.*);")
    for i in range(line_idx - 1, max(-1, line_idx - 1 - window), -1):
      m = pattern.search(java_lines[i])
      if m:
        return m.group(1).strip()
    return None

  def find_histogram_candidates(self, key):
    '''
    for a given key, turn each collected getObject() entry into a skeleton
    and scan {key}.java under default_histogram_dir for H1F/H2F/H3F/H1Fb
    definitions whose name skeleton matches.
    returns a list of row-dicts, one per (getObject entry, matching
    histogram definition) pair - or one row with a blank histogram side
    if no definition matched at all.
    '''
    # case-insensitive match: engine keys come from run_analysis.groovy's
    # out_KEY names, but the actual filename casing varies (helicity.java,
    # trigger.java are lowercase; most others match the key exactly)
    histogram_file_matches = [
      f for f in glob("{}/*.java".format(self.default_histogram_dir))
      if os.path.basename(f).lower() == "{}.java".format(key).lower()
    ]
    histogram_script = histogram_file_matches[0] if histogram_file_matches else None
    if histogram_script is None:
      print("WARNING: no histogram file found for key '{}' ({}.java)".format(key, key))

    name_defs = []
    if histogram_script is not None:
      with open(histogram_script, "r") as file:
        java_lines = file.readlines()

      # each entry: (line_no, name_skeleton)
      for i, l in enumerate(java_lines):
        # require "new " before H1F/H1Fb/H2F/H3F so we only catch actual
        # instantiations, not the class's own constructor declaration
        # (e.g. `public H1Fb(String name, ...) {` inside helicity.java);
        # histClone(...) is always a real call, no "new" involved
        m = re.search(r"new\s+H1F\(|new\s+H1Fb\(|new\s+H2F\(|new\s+H3F\(|histClone\(", l)
        if not m:
          continue
        # first argument after the opening paren, up to the next top-level
        # comma, or the call's own closing paren if there's only one arg
        rest = l[m.end():]
        depth = 0
        end_j = len(rest)
        for j, c in enumerate(rest):
          if c in '([':
            depth += 1
          elif c in ')]':
            if depth == 0:
              end_j = j
              break
            depth -= 1
          elif c == ',' and depth == 0:
            end_j = j
            break
        name_arg = rest[:end_j].strip()

        # a bare identifier means the real name-building expression is
        # elsewhere - look for its nearest preceding assignment
        if re.fullmatch(r"[A-Za-z_][A-Za-z0-9_]*", name_arg):
          resolved = self.resolve_variable_expr(java_lines, i, name_arg)
          if resolved is not None:
            name_arg = resolved

        name_defs.append((i + 1, l, self.name_expr_to_skeleton(name_arg)))

    rows = []
    for entry in self.histogram_var_list.get(key, []):
      go_match = re.search(r"getObject\s*\((.*)\)", entry['line'])
      if not go_match:
        continue
      go_skeleton = self.name_expr_to_skeleton(go_match.group(1).strip())
      matches = [(no, l) for no, l, skel in name_defs if self.skeletons_match(go_skeleton, skel)]
      if not matches:
        matches = [(None, None)]
      for no, java_line in matches:
        row = {
          "timeline script": os.path.relpath(entry['timeline_script'], self.default_timeline_dir),
          "timeline line number": entry['line_no'],
          "histogram script": os.path.relpath(histogram_script, self.default_histogram_dir) if histogram_script else None,
          "histogram line number": no,
        }
        if self.raw:
          row["timeline line"] = entry['line'].rstrip("\n")
          row["histogram line"] = java_line.rstrip("\n") if java_line else None
        rows.append(row)
    return rows


  def construct_histogram_var_list(self):
    '''
    for each output group (key) in self.engines, open each of its timeline
    script files ({class_name}.groovy under default_timeline_dir/*/) and
    collect every getObject() line, remembering which file and line number
    it came from.
    fills self.histogram_var_list as
    { key: [{'timeline_script': path, 'line_no': N, 'line': text}, ...], ... }
    '''
    self.histogram_var_list = {}

    for key, class_names in self.engines.items():
      self.histogram_var_list[key] = []
      for class_name in class_names:
        matches = glob("{}/*/{}.groovy".format(self.default_timeline_dir, class_name))
        if not matches:
          print("WARNING: no .groovy file found for class", class_name)
          continue
        timeline_script = matches[0]
        with open(timeline_script, "r") as file:
          for line_no, line in enumerate(file, start=1):
            if "getObject" in line:
              self.histogram_var_list[key].append({
                "timeline_script": timeline_script,
                "line_no": line_no,
                "line": line,
              })


if __name__ == "__main__":
  import argparse
  parser = argparse.ArgumentParser(description="cross-reference timeline getObject() calls against histogram definitions")
  parser.add_argument("-raw", action="store_true", help="include the exact timeline/histogram source lines in the output table")
  args = parser.parse_args()

  timeline_histograms = timeline_hists(raw=args.raw)
  print(timeline_histograms.df.to_string(index=False))