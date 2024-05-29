#!/usr/bin/env ruby

require 'fileutils'
require 'optparse'
require 'ostruct'

# input data information
def make_rgc_path(energy, target)
  "/cache/clas12/rg-c/production/summer22/pass1/#{energy}gev/#{target}/dst/recon"
end
DATA_HASH = {
  'rga_sp19'               => '/cache/clas12/rg-a/production/recon/spring2019/torus-1/pass2/dst/recon',
  'rgc_su22_10.5GeV_Align' => make_rgc_path(10.5, 'Align'),
  'rgc_su22_10.5GeV_C'     => make_rgc_path(10.5, 'C'),
  'rgc_su22_10.5GeV_CH2'   => make_rgc_path(10.5, 'CH2'),
  'rgc_su22_10.5GeV_ET'    => make_rgc_path(10.5, 'ET'),
  'rgc_su22_10.5GeV_ND3'   => make_rgc_path(10.5, 'ND3'),
  'rgc_su22_10.5GeV_NH3'   => make_rgc_path(10.5, 'NH3'),
  'rgc_su22_2.2GeV_Align'  => make_rgc_path(2.2, 'Align'),
  'rgc_su22_2.2GeV_C'      => make_rgc_path(2.2, 'C'),
  'rgc_su22_2.2GeV_ET'     => make_rgc_path(2.2, 'ET'),
  'rgc_su22_2.2GeV_NH3'    => make_rgc_path(2.2, 'NH3'),
}
YAML_FILE = 'train.qa.yaml'

# helper functions
def get_run_group(dataset_name)
  dataset_name.split('_').first
end

def shorten_dataset_name(dataset_name)
  dataset_name.split('_')[1..-1].map do |tok| # remove run group
    tok.sub(/GeV/,'') # remove units
  end.join # remove underscores
end

def print_info
  yield if block_given?
  puts "="*82
end

# parse options
options         = OpenStruct.new
options.dataset = ''
options.outDir  = "/volatile/clas12/users/#{ENV['LOGNAME']}"
options.coatjava = ''
OptionParser.new do |o|
  o.banner = "USAGE: #{$0} [OPTIONS]..."
  o.separator ''
  o.separator 'REQUIRED OPTIONS:'
  o.separator ''
  o.on(
    "--dataset [DATASET_NAME]",
    String,
    "the name of the dataset to process",
    "Choose one of the following:"
  ) do |a|
    if DATA_HASH.has_key? a
      options.dataset = a
    else
      $stderr.puts "ERROR: dataset name '#{a}' is not defined"
      exit 1
    end
  end
  rgTmp = ''
  DATA_HASH.keys.each do |key|
    rg = get_run_group key
    if rg != rgTmp
      rgTmp = rg
      o.separator ''
    end
    o.separator key.rjust(50)
  end
  o.separator ''
  o.on(
    "--coatjava [COATJAVA_VERSION]",
    String,
    "coatjava version"
  ) { |a| options.coatjava = a }
  o.separator ''
  o.separator 'OPTIONAL OPTIONS:'
  o.separator ''
  o.on(
    "--outDir [OUT_DIR]",
    String,
    "output files will appear in [OUT_DIR]/qa_[DATASET_NAME]",
    "Default: #{options.outDir}"
  ) { |a| options.outDir = a }
  o.separator ''
  o.on_tail('-h', '--help', 'Show this message') do
    puts o
    exit 2
  end
end.parse!(ARGV.length>0 ? ARGV : ['--help'])
print_info { puts "OPTIONS: #{options}" }
[ ['--dataset',options.dataset], ['--coatajava',options.coatjava] ].each do |n,o|
  if o.empty?
    $stderr.puts "ERROR: missing required argument for '#{n}'"
    exit 1
  end
end

# generate list of runs, using /mss
runListFile = 'tmp/runlist.txt'
FileUtils.mkdir_p 'tmp'
mssDir = DATA_HASH[options.dataset].gsub /^\/cache\//, '/mss/'
File.open(runListFile, 'w') do |out|
  runList = Dir.glob("#{mssDir}/*/")
    .map{ |dirName| dirName.split('/').last }
    .map(&:to_i)
    .sort
  runList.each{ |run| out.puts run }
  print_info { puts "runs = #{runList}" }
end

# generate clas12-workflow arguments
workflowArgs = {
  :runGroup  => get_run_group(options.dataset),
  :model     => 'ana',
  :tag       => shorten_dataset_name(options.dataset),
  :coatjava  => options.coatjava,
  :runs      => runListFile,
  :inputs    => mssDir,
  :trainYaml => YAML_FILE,
  :outDir    => "#{options.outDir}/qa_#{options.dataset}",
}

cmd = ['clas12-workflow'] + workflowArgs.map{ |opt,val| "--#{opt.to_s} #{val}" }

# print clas12-workflow command and exec it
print_info { puts cmd.join " \\\n" }
exec *cmd
