#!/bin/bash
grep --color -vE '█|═|Physics Division|^     $' /farm_out/$LOGNAME/clas12-timeline--*.err
