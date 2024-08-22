#!/usr/bin/env ruby

# parse args
unless ARGV.size == 2
  puts "USAGE: #{$0} [DATASET_PATTERN] [TRAIN_TOP_DIR]"
  exit
end
dataset_pattern, train_top_dir = ARGV

# get the list of datasets matching `dataset_pattern`
dataset_list = `cook-train.rb --listDatasets`
  .split("\n")
  .grep(/#{dataset_pattern}/)
puts "CHECKING DATASETS:"
puts dataset_list.map{|s| " - #{s}"}
sep = '='*82
puts sep

# loop over the datasets
success = true
dataset_list.each do |dataset|

  ### get the train HIPO files
  train_dir = "#{train_top_dir}/qa_#{dataset}/train/QA"
  hipo_files = Dir.glob "#{train_dir}/*.hipo"
  if hipo_files.empty?
    $stderr.puts "ERROR: directory #{train_dir} has no HIPO files or doesn't exist"
    exit 1
  end

  ### use the train HIPO files to get a list of their run numbers
  output_run_nums = hipo_files.map do |filename|
    File.basename(filename, '.hipo').split('_').last.to_i
  end.sort
  # puts output_run_nums

  ### get a correpsonding list of input run numbers from `/mss` tape stubs
  input_top_dir = `cook-train.rb --dataset #{dataset} --printDataDir`.chomp
  input_dirs = Dir.glob("#{input_top_dir}/*")
    .grep_v(/README/)
    .map{|d| d.sub /^\/cache\//, '/mss/'}
    .select{|d| Dir.exists? d}
    .reject{|d| Dir.glob("#{d}/*.hipo").empty?}
  # puts input_dirs
  input_run_nums = input_dirs.map{|d| File.basename(d).to_i}.sort
  # puts input_run_nums

  # compare the lists
  unless input_run_nums == output_run_nums
    $stderr.puts <<~EOS
      ERROR: input and output run number lists do not match for dataset '#{dataset}'
      - runs which are on /mss but not in output: #{input_run_nums - output_run_nums}
      - runs which are in output but not on /mss: #{output_run_nums - input_run_nums}
        - /mss dir:   #{input_top_dir}
        - output dir: #{train_dir}
    EOS
    success = false
  end
end
exit 1 unless success
