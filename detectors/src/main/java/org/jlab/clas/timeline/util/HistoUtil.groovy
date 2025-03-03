package org.jlab.clas.timeline.util
import org.jlab.groot.data.H1F

class HistoUtil {

  /// zoom on the range of filled bins of a histogram
  /// @param histIn the input histogram
  /// @param threshold the fraction of the max bin to assume valid data (useful for ignoring long tails)
  /// @param nBufferBins how many extra bins on each side of the zoomed range
  static def zoomHisto(H1F histIn, double threshold=0.0, int nBufferBins=3) {

    // read input histogram
    def nBinsIn = histIn.getXaxis().getNBins()
    def xData   = (0..<nBinsIn).collect{ histIn.getBinContent(it) }
    def rangeIn = [ histIn.getXaxis().min(), histIn.getXaxis().max() ]
    def widthIn = (rangeIn[1]-rangeIn[0]) / nBinsIn

    // check arguments
    if(threshold<0 || threshold>1 || nBufferBins<0) {
      throw new Exception("bad arguments for zoomHisto")
    }

    // find the data range
    def minVal = xData.max() * threshold
    def dataBinRange = [
      [ xData.findIndexOf{ it > minVal } - nBufferBins, 0 ].max(),
      [ nBinsIn - xData.reverse().findIndexOf{ it > minVal } + nBufferBins, nBinsIn-1].min()
    ]
    def rangeOut = dataBinRange.collect{ histIn.getXaxis().getBinCenter(it) }
    rangeOut[0] -= widthIn / 2
    rangeOut[1] += widthIn / 2
    def nBinsOut = dataBinRange[1] - dataBinRange[0] + 1
    def widthOut = (rangeOut[1]-rangeOut[0]) / nBinsOut

    // print some information, for debugging
    def printDebug = {
      System.err.println """
      histoZoom of histogram '${histIn.getName()}':
        nBins: ${nBinsIn} -> ${nBinsOut}
        xMin:  ${rangeIn[0]} -> ${rangeOut[0]}
        xMax:  ${rangeIn[1]} -> ${rangeOut[1]}
        binWidth: ${widthIn} -> ${widthOut}
      """
    }
    // printDebug()

    // check that the bin width is correct
    if((widthIn-widthOut).abs()>0.0001) {
      printDebug()
      throw new Exception("bin widths don't match")
    }

    // define and fill the output, zoomed histogram
    def histOut = new H1F(histIn.getName(), histIn.getTitle(), nBinsOut, rangeOut[0], rangeOut[1])
    histOut.setTitleX(histIn.getTitleX())
    histOut.setTitleY(histIn.getTitleY())
    nBinsIn.times{ histOut.fill(histIn.getXaxis().getBinCenter(it), histIn.getBinContent(it)) }
    return histOut
  }

  /// @returns the Interquartile Range (IQR) of a histogram
  /// @param hist the histogram; FIXME: only unweighted histograms are supported at the moment
  /// @param defIfEmpty the default value, if `hist` is empty
  static def getHistoIQR(H1F hist, defIfEmpty) {
    if( !(hist.getEntries() > 0) || !(hist.integral() > 0) ) { // `.getEntries()` can be nonzero for empty histograms, so be sure to check the integral too
      return defIfEmpty
    }
    def hist_list = []
    int nbins = hist.getAxis().getNBins()
    for (int bin=0; bin<nbins; bin++) {
      int counts = hist.getBinContent(bin).toInteger() // FIXME: assumes the histogram is unweighted
      def value  = hist.getAxis().getBinCenter(bin)
      for (int j=0; j<counts; j++) { hist_list += value }
    }
    def listMedian = { d ->
      if(d.size()==0) {
        // this list may end up being empty if there are _few_ entries in the histogram, since this method
        // is called to get the quartiles; in this case, just return `defIfEmpty`
        return defIfEmpty
      }
      d.sort()
      def m = d.size().intdiv(2)
      d.size() % 2 ? d[m] : (d[m-1]+d[m]) / 2
    }
    def mq = listMedian(hist_list)
    def lq = listMedian(hist_list.findAll{it<mq})
    def uq = listMedian(hist_list.findAll{it>mq})
    return uq - lq
  }
}
