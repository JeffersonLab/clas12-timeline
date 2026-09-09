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
    self.histogram_scripts = {}
    for key in self.engines:
      self.rows.extend(self.find_histogram_candidates(key))
    columns = ["timeline script", "timeline line number",
               "histogram script", "histogram line number",
               "histogram field", "histogram field declaration line number"]
    if self.raw:
      columns += ["timeline line", "histogram line"]
    self.df = pd.DataFrame(self.rows, columns=columns)
    self.df["timeline line number"] = self.df["timeline line number"].astype("Int64")
    self.df["histogram line number"] = self.df["histogram line number"].astype("Int64")
    self.df["histogram field declaration line number"] = self.df["histogram field declaration line number"].astype("Int64")

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

  def _top_level_split(self, text, sep):
    '''split text on `sep` at paren/bracket depth 0, outside quotes'''
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
        elif c == sep and depth == 0:
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
    parts = self._top_level_split(expr, '+')
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

  def _iter_declared_fields(self, java_lines):
    '''
    walk a histogram class's source top-to-bottom, yielding
    (line_no, type_str, field_name) for every H1F/H2F/H3F field
    declaration - splitting comma-grouped declarations
    (`public H1F[] foo, bar;`) into one tuple per field, in source order.
    shared by find_field_declarations (line-number lookup) and
    print_annotated_declarations (full rewrite with usage annotations).
    '''
    decl_re = re.compile(r"^\s*(public\s+|private\s+)?(H[123]F(?:\[\])*)\s+(.+);")
    list_re = re.compile(r"^\s*(public\s+|private\s+)?((?:Array)?List<H[123]Fb?>)\s+(\w+)\s*[=;]")
    for i, l in enumerate(java_lines):
      m = decl_re.match(l)
      if m:
        modifier, type_str, names_part = m.group(1) or '', m.group(2), re.sub(r"//.*", "", m.group(3))
        for name in self._top_level_split(names_part, ','):
          name = name.split("=")[0]
          name = re.sub(r"\[[^\]]*\]", "", name).strip()
          if name and re.fullmatch(r"[A-Za-z_][A-Za-z0-9_]*", name):
            yield i + 1, modifier.strip(), type_str, name
        continue
      m = list_re.match(l)
      if m:
        modifier = m.group(1) or ''
        yield i + 1, modifier.strip(), m.group(2), m.group(3)

  def find_field_declarations(self, java_lines):
    '''
    scan a histogram class's source for H1F/H2F/H3F field declarations -
    `public H1F[] foo, bar;`, `H2F[][] baz;`, `H1F hboard;`,
    `List<H1F> hiNphePMTOneHit = ...` - returning { field_name: line_no }
    for the line where each field was first declared (its type
    declaration), not where it's later constructed via `new H1F(...)`.
    '''
    declarations = {}
    for line_no, modifier, type_str, name in self._iter_declared_fields(java_lines):
      if name not in declarations:
        declarations[name] = line_no
    return declarations

  def extract_lhs_field(self, line):
    '''
    given a line that constructs a histogram (`new H1F(...)`), find the
    java field it's being stored into - the LHS of a plain assignment
    (`H_dt[m-1] = new H1F(...)` -> "H_dt"), or the receiver of an
    `.add(new H1F(...))` call (`hiNphePMTOneHit.add(new H1F(...))` ->
    "hiNphePMTOneHit"). returns None if neither pattern is found.
    '''
    m = re.search(r"([A-Za-z_][A-Za-z0-9_]*)\.add\s*\(\s*new\s+H[123]F", line)
    if m:
      return m.group(1)
    m = re.match(r"\s*([A-Za-z_][A-Za-z0-9_]*)\s*(?:\[[^\]]*\])*\s*=[^=]", line)
    if m:
      return m.group(1)
    return None

  def _resolve_field(self, line, field_declarations):
    '''returns (field_name, field_declaration_line_no), either may be None'''
    field_name = self.extract_lhs_field(line)
    field_line = field_declarations.get(field_name) if field_name else None
    return field_name, field_line

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
    self.histogram_scripts[key] = histogram_script
    if histogram_script is None:
      print("WARNING: no histogram file found for key '{}' ({}.java)".format(key, key))

    name_defs = []
    if histogram_script is not None:
      with open(histogram_script, "r") as file:
        java_lines = file.readlines()

      field_declarations = self.find_field_declarations(java_lines)

      # each entry: (line_no, name_skeleton, field_name, field_decl_line_no)
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

        name_defs.append((i + 1, l, self.name_expr_to_skeleton(name_arg),
                           *self._resolve_field(l, field_declarations)))

    rows = []
    for entry in self.histogram_var_list.get(key, []):
      go_match = re.search(r"getObject\s*\((.*)\)", entry['line'])
      if not go_match:
        continue
      go_skeleton = self.name_expr_to_skeleton(go_match.group(1).strip())
      matches = [(no, l, field, field_line) for no, l, skel, field, field_line in name_defs
                 if self.skeletons_match(go_skeleton, skel)]
      if not matches:
        matches = [(None, None, None, None)]
      for no, java_line, field_name, field_line in matches:
        row = {
          "timeline script": os.path.relpath(entry['timeline_script'], self.default_timeline_dir),
          "timeline line number": entry['line_no'],
          "histogram script": os.path.relpath(histogram_script, self.default_histogram_dir) if histogram_script else None,
          "histogram line number": no,
          "histogram field": field_name,
          "histogram field declaration line number": field_line,
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

  def print_annotated_declarations(self):
    '''
    one-time summary, meant to be printed at the end: for each histogram
    java file, print every H1F/H2F/H3F field declaration on its own line
    (splitting out from the original comma-grouped declarations),
    annotated with the timeline script(s) that reference it, or
    `// unused` if none do.
    '''
    # field usage: histogram_script (relpath) -> field_name -> sorted class names
    usage = {}
    for row in self.rows:
      script, field = row["histogram script"], row["histogram field"]
      if script is None or field is None:
        continue
      class_name = os.path.splitext(os.path.basename(row["timeline script"]))[0]
      usage.setdefault(script, {}).setdefault(field, set()).add(class_name)

    seen_scripts = set()
    for key, histogram_script in self.histogram_scripts.items():
      if histogram_script is None or histogram_script in seen_scripts:
        continue
      seen_scripts.add(histogram_script)
      relpath = os.path.relpath(histogram_script, self.default_histogram_dir)
      field_usage = usage.get(relpath, {})

      print("// {}".format(relpath))
      with open(histogram_script, "r") as file:
        java_lines = file.readlines()
      decl_lines = []
      for line_no, modifier, type_str, name in self._iter_declared_fields(java_lines):
        related = sorted(field_usage.get(name, []))
        comment = "// related timeline: {}".format(related) if related else "// unused"
        prefix = "{} ".format(modifier) if modifier else ""
        decl_lines.append(("  {}{} {};".format(prefix, type_str, name), comment))
      width = max((len(d) for d, _ in decl_lines), default=0) + 1
      for decl, comment in decl_lines:
        print("{} {}".format(decl.ljust(width), comment))
      print()


if __name__ == "__main__":
  import argparse
  parser = argparse.ArgumentParser(description="cross-reference timeline getObject() calls against histogram definitions")
  parser.add_argument("-raw", action="store_true", help="include the exact timeline/histogram source lines in the output table")
  args = parser.parse_args()

  timeline_histograms = timeline_hists(raw=args.raw)
  print(timeline_histograms.df.to_string(index=False))
  timeline_histograms.print_annotated_declarations()