#!/usr/bin/env ruby

# compare two tables dumped by `dump-charges.rb`
# example:
#   1. dump-charges.rb before/chargeTree.json > before.dat
#   2. dump-charges.rb after/chargeTree.json  > after.dat
#   3. dump-charges-diff.rb before.dat after.dat

# number of decimal places to print
PRECISION = 8

# args
@print_mode = 'pct'
unless ARGV.length >= 2
  abort """USAGE: #{$0} [file1] [file2] <MODE>
  [file1] and [file2] should be output from `dump-charges.rb`
  MODE may be one of (default='#{@print_mode}'):
    pct     print percent differences
    abs     print absolute differences
  """
end
file1, file2 = ARGV[0], ARGV[1]
@print_mode  = ARGV[2] if ARGV.size > 2

# read output tables from `dump-charges.rb`
def read_table(filename)
  lines  = File.readlines(filename).map(&:strip).reject(&:empty?)
  header = lines.shift.split(/\s+/)
  rows   = lines.map { |l| l.split(/\s+/) }
  [header, rows]
end
header1, rows1 = read_table(file1)
header2, rows2 = read_table(file2)
abort "ERROR: Files have different number of rows!" unless rows1.size == rows2.size
abort "ERROR: Files have different headers!" unless header1 == header2

# compute difference
def diff(a, b)
  case @print_mode
  when 'pct'
    return 0.0 if a == 0 && b == 0
    ((a - b) / ((a + b) / 2.0)) * 100
  when 'abs'
    return a - b
  else
    abort "ERROR: unknown MODE '#{@print_mode}'"
  end
end

# print a general row
def print_row(cols)
  puts cols.map{ |c| c.to_s.ljust 12+PRECISION}.join(' ')
end

# print header row
print_row header1

# loop over tables
rows1.each_with_index do |row1, i|
  row2 = rows2[i]

  unless row1[0] == row2[0]
    abort "ERROR: Row mismatch at line #{i + 2}: #{row1[0]} vs #{row2[0]}"
  end

  out_row = [row1[0]] # keep 1st column (runnum, etc.) as-is

  # compute differences
  (1...header1.size).each do |col_idx|
    a = row1[col_idx].to_f
    b = row2[col_idx].to_f
    out_row << diff(a, b).round(PRECISION)
  end

  print_row out_row
end
