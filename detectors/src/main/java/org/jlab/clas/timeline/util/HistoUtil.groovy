package org.jlab.clas.timeline.util
import org.jlab.groot.data.H1F

class HistoUtil {

  // zoom on the range of filled bins of a histogram
  static def zoomHisto(H1F inH, int nBufferBins=3) {

    // read input histogram
    def nBins  = inH.getXaxis().getNBins()
    def xData  = (0..<nBins).collect{ inH.getBinContent(it) }

    // find the data range
    def dataBinRange = [
      xData.findIndexOf{ it>0 } - nBufferBins,
      nBins - xData.reverse().findIndexOf{ it>0 } + nBufferBins
    ]
    def dataValRange = dataBinRange.collect{ inH.getXaxis().getBinCenter(it) }
    def nDataBins    = dataBinRange[1] - dataBinRange[0]

    // define and fill the output, zoomed histogram
    def outH = new H1F(inH.getName(), inH.getTitle(), nDataBins, dataValRange[0], dataValRange[1])
    nbins.times{ outH.fill( inH.getXaxis().getBinCenter(it), inH.getBinContent(it) }
    return outH
  }
