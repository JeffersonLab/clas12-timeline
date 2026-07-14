#!/usr/bin/env ruby

# dump total charges for a given 'chargeTree.json' file

require 'json'

# args
unless ARGV.length == 1
  $stderr.puts "USAGE #{$0} [chargeTree.json file]"
  exit 2
end
chargeTree = JSON.parse(File.read(ARGV[0]))

# charge value names (and run number); these'll be the columns to be printed
q_keys = [
  'runnum',
  'dsc2_qg',
  'dsc2_qu',
  'struck_helP_qg',
  'struck_hel0_qg',
  'struck_helN_qg',
  'struck_totl_qg',
]

# print a row
def row(cols)
  puts cols.map{ |c| c.to_s.ljust 15}.join(' ')
end

# print the header row
row q_keys

# loop over run numbers
chargeTree.each do |runnum, bins|

  # compute the total charge by summing over the bins' charge
  q = q_keys.map{ |k| [k, 0.0] }.to_h
  bins.each_value do |b|
    q['dsc2_qg']        += b['fcChargeMax']  - b['fcChargeMin']
    q['dsc2_qu']        += b['ufcChargeMax'] - b['ufcChargeMin']
    q['struck_helP_qg'] += b['fcChargeHelicity']['1']
    q['struck_hel0_qg'] += b['fcChargeHelicity']['0']
    q['struck_helN_qg'] += b['fcChargeHelicity']['-1']
  end
  q['struck_totl_qg'] = q['struck_helP_qg'] + q['struck_hel0_qg'] + q['struck_helN_qg']
  q['runnum'] = runnum

  # round them to a few decimal places
  q_keys.each do |key|
    next if key=='runnum'
    q[key] = q[key].round 2
  end

  # print them
  row q.values

end
