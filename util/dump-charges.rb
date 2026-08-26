#!/usr/bin/env ruby

# dump total charges for a given 'chargeTree.json' file

require 'json'

# number of decimal places to print
PRECISION = 2

# args
print_mode = 'sum'
if ARGV.empty?
  $stderr.puts """USAGE #{$0} [chargeTree.json file] <MODE>
  MODE may be one of (default='#{print_mode}'):
    sum     sum charge over all bins for each run
    fine    print charge for each bin
  """
  exit 2
end
charge_tree = JSON.parse File.read(ARGV[0])
print_mode  = ARGV[1] if ARGV.size > 1

# charge value names (and run number); these'll be the columns to be printed
COL_NAMES = [
  print_mode == 'sum' ? 'runnum' : 'runnum_bin',
  'num_bins',
  'dsc2_qg',
  'dsc2_qu',
  'struck_helP_qg',
  'struck_hel0_qg',
  'struck_helN_qg',
  'struck_totl_qg',
]

# print a general row
def row(cols)
  puts cols.map{ |c| c.to_s.ljust 12+PRECISION}.join(' ')
end

# print a row of values from a hash
def row_vals(col_hash)
  row col_hash.map{ |k,v|
    if ['runnum', 'runnum_bin', 'num_bins'].include? k
      v
    else
      v.round PRECISION
    end
  }
end

# print the header row
row COL_NAMES

# loop over run numbers
charge_tree.each do |runnum, bins|

  # initialize output values
  q_out = COL_NAMES.map do |col_name|
    init_val = case col_name
    when /runnum/
      runnum.to_s
    when 'num_bins'
      bins.size
    else
      0.0
    end
    [col_name, init_val]
  end.to_h

  # read charges and print them
  case print_mode
  when 'sum'
    # sum over bins
    bins.each do |binnum, b|
      q_out['dsc2_qg']        += b['fcChargeMax']  - b['fcChargeMin']
      q_out['dsc2_qu']        += b['ufcChargeMax'] - b['ufcChargeMin']
      q_out['struck_helP_qg'] += b['fcChargeHelicity']['1']
      q_out['struck_hel0_qg'] += b['fcChargeHelicity']['0']
      q_out['struck_helN_qg'] += b['fcChargeHelicity']['-1']
    end
    q_out['struck_totl_qg'] = q_out['struck_helP_qg'] + q_out['struck_hel0_qg'] + q_out['struck_helN_qg']
    row_vals q_out
  when 'fine'
    bins.each do |binnum, b|
      q_out['runnum_bin']     = "#{runnum}_#{binnum}"
      q_out['dsc2_qg']        = b['fcChargeMax']  - b['fcChargeMin']
      q_out['dsc2_qu']        = b['ufcChargeMax'] - b['ufcChargeMin']
      q_out['struck_helP_qg'] = b['fcChargeHelicity']['1']
      q_out['struck_hel0_qg'] = b['fcChargeHelicity']['0']
      q_out['struck_helN_qg'] = b['fcChargeHelicity']['-1']
      q_out['struck_totl_qg'] = q_out['struck_helP_qg'] + q_out['struck_hel0_qg'] + q_out['struck_helN_qg']
      row_vals q_out
    end
  else
    raise "unknown MODE '#{print_mode}'"
  end

end
