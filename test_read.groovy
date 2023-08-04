// check if a HIPO file can be opened

/*
TODO:
- find a better place to put this file
- consider qa-physics/Tools.groovy, but move it to some top-level `utils` directory
- make a method, which returns true/false, instead of exit codes
- consider using Java, which may be slightly faster
*/


import org.jlab.groot.data.TDirectory

if(args.length==0) {
  System.err.println("ERROR: specify one or more HIPO files to test")
  System.exit(100)
}

args.each {
  try {
    def inTdir = new TDirectory()
    inTdir.readFile(it)
  } catch(Exception ex) {
    System.err.println("\nERROR: cannot read HIPO file $it\n\nSTACK TRACE:\n============")
    ex.printStackTrace()
    System.exit(100)
  }
}
