#!/bin/bash
for f in mon qa; do
  run-groovy validate_beam_energy.groovy beam-energy-$f.yaml |& tee diff-$f.txt
done
echo """
to see differences:
diff-*.txt
"""
