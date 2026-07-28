#!/usr/bin/env ruby

# compare two tables dumped by `dump-charges.rb`
# example:
#   1. dump-charges.rb before/chargeTree.json > before.dat
#   2. dump-charges.rb after/chargeTree.json  > after.dat
#   3. dump-charges-diff.rb before.dat after.dat

file1, file2 = ARGV[0], ARGV[1]
abort "USAGE: #{$0} <file1> <file2>" unless file1 && file2

def read_table(filename)
  lines = File.readlines(filename).map(&:strip).reject(&:empty?)
  header = lines.shift.split(/\s+/)
  rows = lines.map { |l| l.split(/\s+/) }
  [header, rows]
end

header1, rows1 = read_table(file1)
header2, rows2 = read_table(file2)

abort "ERROR: Files have different number of rows!" unless rows1.size == rows2.size
abort "ERROR: Files have different headers!" unless header1 == header2

def pct_diff(a, b)
  return 0.0 if a == 0 && b == 0
  ((a - b) / ((a + b) / 2.0)) * 100
end

col_width = ->(s) { [s.to_s.length, 12].max }

puts header1.map { |h| h.ljust(col_width.call(h)) }.join(" ")

rows1.each_with_index do |row1, i|
  row2 = rows2[i]

  unless row1[0] == row2[0]
    abort "ERROR: Row mismatch at line #{i + 2}: #{row1[0]} vs #{row2[0]}"
  end

  out_row = [row1[0]] # keep runnum as-is

  (1...header1.size).each do |col_idx|
    a = row1[col_idx].to_f
    b = row2[col_idx].to_f
    out_row << format("%.2f", pct_diff(a, b))
  end

  puts out_row.each_with_index.map { |v, idx| v.to_s.ljust(col_width.call(header1[idx])) }.join(" ")
end
