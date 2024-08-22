package org.jlab.clas.timeline.timeline.cvt
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors

class cvt_efficiency {

def data = new ConcurrentHashMap()
def data_file = null

def processDirectory(dir, run) {

  // load the data file
  if(data_file == null) {
    def data_file_name = "${System.getenv('TIMELINESRC')}/data/cvt/sample.dat"
    data_file = new File(data_file_name)
    if(!(data_file.exists())) {
      throw new Exception("cannot find data file '${data_file_name}'")
    }
  }

  // parse the data file
  def found = false
  data_file.eachLine{ line ->
    if(found) { // if already found, don't keep parsing
      return
    }
    def tokens = line.replaceAll(/#.*/,'').tokenize(' ') // parse line: remove comments and tokenize
    if(tokens[0] == null || tokens[1] == null) { // skip empty or incomplete lines
      return
    }
    if(tokens[0].toInteger() == run) { // if the line correpsonds to this run number (`run`), insert to `data`
      found = true
      data[run] = [
        run: run,
        eff: tokens[1].toDouble(),
      ]
    }
  }
}


def close() {

  TDirectory out = new TDirectory()

  def grtl = new GraphErrors('cvt_efficiency')
  grtl.setTitle("CVT Efficiency")
  grtl.setTitleY("Efficiency")
  grtl.setTitleX("run number")

  data.sort{it.key}.each{run, it ->
    grtl.addPoint(it.run, it.eff, 0, 0)
  }

  // if there are no points, we need to add at least one bogus point, otherwise the front-end will hang
  if(grtl.getDataSize(0) == 0) {
    grtl.addPoint(0, 0, 0, 0)
    grtl.setTitle("${grtl.getTitle()}: ERROR - efficiency table has not yet been generated for these runs")
  }

  out.mkdir('/timelines')
  out.cd('/timelines')
  grtl.each{ out.addDataSet(it) }
  out.writeFile('cvt_efficiency.hipo')
}
}
