// prints list of "golden runs": runs with no defects

import groovy.json.JsonSlurper
import groovy.json.JsonOutput

def dataset = "inbending1"
if(args.length>=1) dataset = args[0]
infile="qa.${dataset}/qaTree.json"
outfile="golden.${dataset}.dat"

def outfileF = new File(outfile)
def outfileW = outfileF.newWriter(false)

def slurper = new JsonSlurper()
def jsonFile = new File(infile)
def qaTree = slurper.parse(jsonFile)
def golden
qaTree.sort{a,b -> a.key.toInteger() <=> b.key.toInteger() }.each{
  run, runTree ->
  golden=true
  runTree.sort{a,b -> a.key.toInteger() <=> b.key.toInteger() }.each{
    itBin, binTree ->
    if(binTree['defect']>0) golden=false
  }
  if(golden) outfileW << "$run\n"
}

outfileW.close()
