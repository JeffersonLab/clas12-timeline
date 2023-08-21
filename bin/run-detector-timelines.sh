#!/bin/bash

# constants ############################################################
SCRIPTPATH=`realpath $0`
BINPATH=`dirname $SCRIPTPATH`
DIRPATH=`dirname $BINPATH`


if [ $1 = "build" ]; then
	echo "building detector timeline"
	
	cd $DIRPATH/detectors
	mvn clean package
	cd -
else
	GROOVYPATH=`which groovy`
	GROOVYBIN=`dirname $GROOVYPATH`
	export GROOVYLIB="`dirname $GROOVYBIN`/lib"

	export JARPATH=`realpath $(dirname $0)/../detectors/target`/timelineMon-1.0-SNAPSHOT.jar

	#Output directory names
	rungroup=$1 #"rgb"
	cookver=$2 #"pass0v25.18"

	out_dir=$rungroup"_"$cookver

	MAIN="org.jlab.clas.timeline.run"
	if [ $rungroup = "rgb" ]; then
		MAIN="org.jlab.clas.timeline.run_rgb"
	fi
	export MAIN

	echo "processing $rungroup $cookver timeline..."
	shift 2

	inputdir=`realpath $1`
	mkdir -p log $out_dir
	cd $out_dir

	#subdirectory names
	#bmtbst central cnd ctof cvt dc ec forward ft ftof htcc ltcc rf trigger
	for dir in log band bmtbst central cnd ctof cvt dc ec forward ft ftof htcc ltcc rf trigger particle_mass_ctof_and_ftof rich epics
	do
	  mkdir -p "$dir"
	done

	run() {
		echo "processing $1"
		java -DCLAS12DIR="${COATJAVA}/" -cp "${COATJAVA}/lib/clas/*:${COATJAVA}/lib/utils/*:$JARPATH:$GROOVYLIB/*" $MAIN "$@" >& log/$1.log
	}

	export -f run
	#JAVA_OPTS="-Dsun.java2d.pmoffscreen=false -Xms1024m -Xmx12288m"; export JAVA_OPTS

	java -cp "${COATJAVA}/lib/clas/*:${COATJAVA}/lib/utils/*:$JARPATH:$GROOVYLIB/*" $MAIN --timelines |
	xargs -I{} -n1 --max-procs 4 bash -c 'run "$@"' -- {} $inputdir


	mv bmt_*.hipo bmtbst/
	mv bst_*.hipo bmtbst/
	mv cen_*.hipo central/
	mv cnd_*.hipo cnd/
	mv ctof_*.hipo ctof/
	mv cvt_*.hipo cvt/
	mv dc_*.hipo dc/
	mv ec_*.hipo ec/
	mv forward_*.hipo forward/
	mv ftc_*.hipo ft/
	mv fth_*.hipo ft/
	mv ftof_*.hipo ftof/
	mv htcc_*.hipo htcc/
	mv ltcc_*.hipo ltcc/
	mv rftime_*.hipo rf/
	mv rat_* trigger/
	mv ctof/*m2* particle_mass_ctof_and_ftof/
	mv ftof/*m2* particle_mass_ctof_and_ftof
	mv band_* band/
	mv rich_* rich/
	mv epics_* epics/

	echo "---------------------------------------------------------------------------------------------------------------------"
	grep "error:" log/*
	echo "---------------------------------------------------------------------------------------------------------------------"

	echo "Done. Please place the output directory $out_dir in the desired location inside /group/clas/www/clas12mon/html/hipo/."
	echo "Place the json file in that directory so that clas12mon recognizes the hipo files to present them."
	echo "The possible technical errors can be inspected through the log files in $out_dir/log."
fi
