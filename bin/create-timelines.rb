#!/usr/bin/env ruby

require 'optparse'
require 'ostruct'

ERROR_CODE = 100
WWW_DIR    = '/group/clas/www/clas12mon/html/hipo'
BIN_DIR    = File.expand_path File.dirname(__FILE__)

@args = OpenStruct.new
@args.user_args = {
  :detectors => [],
  :physics   => [],
  :deploy    => [],
}
@args.focus = {
  :detectors => false,
  :physics => false,
}
@args.required = {
  :d => false,
  :i => false,
  :r => false,
}
@args.dataset           = ''
@args.output_dir        = File.join '.', 'outfiles'
@args.custom_output_dir = false

# parse options
# NOTE: we only provide the important options for chefs here
OptionParser.new do |o|
  o.banner = "USAGE #{$0} [OPTIONS]..."
  o.separator ''
  o.separator 'REQUIRED OPTIONS'
  o.separator ''
  o.on("-r RUN_GROUP", 'run group, for run-group specific configurations') do |a|
    @args.user_args[:detectors] += ['-r', a.downcase]
    @args.required[:r] = true
  end
  o.separator ''
  o.on('-d DATASET_NAME', 'unique dataset name', 'example: rga_sp19_v8') do |a|
    [:detectors, :physics, :deploy].each do |s| @args.user_args[s] += ['-d', a] end
    @args.dataset = a
    @args.required[:d] = true
  end
  o.separator ''
  o.on('-i INPUT_DIR', 'directory containing run subdirectories', 'of timeline histograms (the output of', 'the workflow)') do |a|
    [:detectors, :physics].each do |s| @args.user_args[s] += ['-i', a] end
    @args.required[:i] = true
  end
  o.separator ''
  o.on('-w WWW_DIR', 'deployment directory, which will be:', "  #{WWW_DIR}/rg[RUN_GROUP]/[WWW_DIR]/[DATASET_NAME]",
       "example: '-r b -d v8.3 -w pass0' will deploy to:", "  #{WWW_DIR}/rgb/pass0/v8.3",
       "IMPORTANT: make sure you have write permission,", "           and DO NOT OVERWRITE old timelines!!!") do |a|
         @args.user_args[:deploy] += ['-t', a]
       end
  o.separator ''
  o.separator 'OPTIONAL OPTIONS'
  o.separator ''
  o.on('-o OUTPUT_DIR', 'output directory, for temporary files;', 'preferably somewhere on /volatile', "default: #{@args.output_dir}/[DATASET_NAME]") do |a|
    @args.output_dir        = a
    @args.custom_output_dir = true
  end
  o.separator ''
  ['detectors', 'physics'].each do |m|
    o.on("--focus-#{m}", "only create #{m.upcase} timelines") do |a|
      @args.focus[m.to_sym] = true
    end
  end
  o.separator ''
  o.on('-n NUM_THREADS', 'number of parallel threads to run detectors timelines') do |a|
    @args.user_args[:detectors] += ['-n', a]
  end
  o.separator ''
  o.on_tail('-h', '--help', 'show this message') do
    puts o
    exit
  end
end.parse! ARGV.length>0 ? ARGV : ['--help']
puts "SETTINGS: #{@args}"

# check for required args
requirements_satisfied = true
@args.required.each do |o,a|
  unless a
    requirements_satisfied = false
    $stderr.puts "ERROR: required option '-#{o.to_s}' is missing"
  end
end
exit ERROR_CODE unless requirements_satisfied

# set custom output dir
if @args.custom_output_dir
  [:detectors, :physics].each do |m|
    @args.user_args[m] += ['-o', @args.output_dir]
  end
else
  @args.output_dir = File.join @args.output_dir, @args.dataset
end

# set input dir for deployment step
@args.user_args[:deploy] += ['-i', File.join(@args.output_dir, 'timeline_web')]

# generate commands
cmds = []
cmds += [[ "#{BIN_DIR}/run-detectors-timelines.sh", *@args.user_args[:detectors] ]] unless @args.focus[:physics]
cmds += [[ "#{BIN_DIR}/run-physics-timelines.sh",   *@args.user_args[:physics]   ]] unless @args.focus[:detectors]
cmds += [[ "#{BIN_DIR}/deploy-timelines.sh",        *@args.user_args[:deploy]    ]]
cmds.map! do |cmd| cmd.join ' ' end
puts "=========== COMMANDS ==========="
cmds.each do |cmd| puts cmd end
puts "================================"
exec cmds.join(' && ')
