package org.jlab.clas.timeline.util
import org.jlab.clas.physics.LorentzVector
import org.jlab.clas.physics.Vector3
import org.jlab.io.hipo.HipoDataSource
import groovy.json.JsonOutput

class Tools {

  /////////////////
  // defect bits //
  /////////////////

  // define defect bits here, with syntax "[bitName]: description"
  // - bitName will be used as an enumerator; colon must follow
  // - description will be printed if desired; do not use colons in description
  def bitDefinitions = [
    "TotalOutlier: outlier N/F, but not terminal, marginal, or sector loss, for FD electron",
    "TerminalOutlier: outlier N/F of first or last file of run, not marginal, for FD electron",
    "MarginalOutlier: marginal outlier N/F, within one standard deviation of cut line, for FD electron",
    "SectorLoss: N/F diminished within a FD sector for several consecutive files",
    "LowLiveTime: live time < 0.9",
    "Misc: miscellaneous defect, documented as comment",
    "TotalOutlierFT: outlier N/F, but not terminal, marginal, or `LossFT`, FT electron",
    "TerminalOutlierFT: outlier N/F of first or last file of run, not marginal, FT electron",
    "MarginalOutlierFT: marginal outlier N/F, within one standard deviation of cut line, FT electron",
    "LossFT: N/F diminished within FT for several consecutive files",
  ]

  // list of bit names and descriptions
  def bitNames = bitDefinitions.collect{ it.tokenize(':')[0] }
  def bitDescripts = bitDefinitions.collect{ it.tokenize(':')[1].substring(1) }

  // map of bitName to bit number
  def bit = { bitName ->
    def bitNum = bitNames.findIndexOf{ it==bitName }
    if(bitNum>=0 && bitNum<bitNames.size()) return bitNum
    else {
      System.err.println "ERROR bad bit name $bitName"
      return 31
    }
  }


  //////////
  // MATH //
  //////////

  // calculate scalar product of 4-vectors
  def lorentzDot = { v1,v2 -> return v1.e()*v2.e() - v1.vect().dot(v2.vect()) }

  // calculate angle between two planes (used for PhiH)
  def crossAB,crossCD
  def sgn
  def numer
  def denom
  def planeAngle = { vA,vB,vC,vD ->
    crossAB = vA.cross(vB) // AxB
    crossCD = vC.cross(vD) // CxD

    // calculate sign of (AxB).D
    sgn = crossAB.dot(vD) // (AxB).D
    if(Math.abs(sgn)<0.00001) return -10000
    sgn /= Math.abs(sgn)

    // calculate numerator and denominator 
    numer = crossAB.dot(crossCD) // (AxB).(CxD)
    denom = crossAB.mag() * crossCD.mag() // |AxB|*|CxD|
    if(Math.abs(denom)<0.00001) return -10000

    // return angle
    return sgn * Math.acos(numer/denom)
  }

  // convert a positive integer into a string binary number
  def printBinary = { num ->
    if(num<=0) return "0b0"
    def str = ""
    def n = num
    while(n) {
      str += n&1 ? "1":"0"
      n>>=1
    }
    return "0b"+str.reverse()
  }
      


  ///////////
  // TREES //
  ///////////

  // build a tree recursively, with branch levels specified by 'levelList'
  // - the leaves will be set to the result of the closure 'clos'
  // - call the subroutine 'buildTree' to initiate
  def rec_buildTree(tree, levelList, lev, clos) {
    def levNew = lev
    levelList.each{ branches ->
      if(lev==levNew) {
        branches.each{ branch ->
          if(levelList.size()==1) { tree[branch] = clos() }
          else {
            if(!tree.containsKey(branch)) tree[branch] = [:]
            levNew = rec_buildTree(tree[branch],levelList[1..-1],lev+1,clos)
          }
        }
      }
    }
    return lev
  }

  // builds a tree called 'treeName', branching from 'trunkName' the branches will be
  // built according to 'levelList'
  // - each element of levelList is a branch list, wherein each branch will split into
  //   the branches listed in the subsequent element of levelList; the last element of
  //   levelList refers to the leaves
  // - the leaves will be initialized with the result of the closure 'clos'
  def buildTree = { trunkName, treeName, levelList, clos -> 
    rec_buildTree( trunkName, [[treeName],*levelList], 0, clos )
  }


  // get a leaf, following the path of branch names specified 
  // by the list 'path'
  def getLeaf = { tree,path ->
    def node = tree
    path.each { node = node[it] }
    return node
  }

  // add a leaf to a tree, specified by closure 'clos'
  // - if the branches listed in 'path' don't exist, create them
  // - if the leaf exists, do nothing
  def addLeaf ( tree,path,clos ) {
    def pathNode = path[0]
    if(tree[pathNode]==null) {
      if(path.size()==1 && !tree.containsKey(pathNode)) {
        tree.put(pathNode,clos()) // add the leaf, and exit
      }
      else if(path.size()>1) {
        tree.put(pathNode,[:]) // add the branch node from 'path', and recurse
        addLeaf(tree[pathNode],path[1..-1],clos)
      }
    }
    else {
      if(path.size()>1) addLeaf(tree[pathNode],path[1..-1],clos)
      else return // this leaf exists, do nothing and exit
    }
  }


  // execute a closure on all leaves which stem from a given node
  // - set 'node' to the top level node of the (sub)tree
  // - set 'clos' to the closure, wherein you may use the variables 'this.leaf' and
  //   'this.leafPath'
  def leaf
  def leafPath
  def key
  def exeLeaves( node, clos, path=[] ) {
    // if the current node has branches, it is not a leaf; loop through the branches and
    // analyze their nodes
    if(node.getClass()==java.util.LinkedHashMap) {
      node.each { 
        path += it.key
        exeLeaves(it.value,clos,path)
        if(it.value.getClass()!=java.util.LinkedHashMap) it.value = leaf
        path.removeAt(path.size()-1)
      }
    }
    else {
      // this is a leaf, since there are no branches;
      // -> execute the closure
      leaf = node
      leafPath = path
      key = path[-1]
      clos()
    }
  }

  // print a tree's branches, with optional printout of closure 'clos'
  def printTree(tree,clos={""}) { exeLeaves(tree,{println "$leafPath "+clos()}) }

  ////////////////
  // JSON FILES //
  ////////////////
  // access subtree at path
  def jAccess ( tree,path ) {
    if(path.size()<1) tree
    else if(path.size()==1) jAccess(tree[path[0]],[])
    else jAccess(tree[path[0]],path[1..-1])
  }

  // pretty printer
  def pPrint = { str -> JsonOutput.prettyPrint(JsonOutput.toJson(str)) }

  // print text output
  def printStatus = { str -> println "STATUS: $str" }
  def printError = { str -> System.err << "ERROR: $str\n" }


  ////////////////
  // HIPO FILES //
  ////////////////
  // get a run number from RUN::config
  def getRunNumber(infile) {
    def reader = new HipoDataSource()
    def runnum = 0
    def event
    reader.open(infile)
    while(reader.hasEvent()) {
      event = reader.getNextEvent()
      if(event.hasBank("RUN::config")) {
        runnum = BigInteger.valueOf(event.getBank("RUN::config").getInt('run',0))
        if(runnum>0) break
      }
    }
    reader.close()
    if(runnum<=0)
      System.err.println("[ERROR]: run number not found for file $infile")
    return runnum
  }
}
