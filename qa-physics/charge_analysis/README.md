These scripts are called by `qtl xcharge`

# Charge Analysis

From Bhawani Singh

`./analyze_charge.py` Produces PNG files comparing the DAQ-gated FC charge determined from
- the `RUN::scaler` bank directly
- the livetime and ungated charge, by multiplication

# Clock analysis
- `analyze_clock.groovy`: gets the clock for each timestamp
- `plot_clock.C`: plots the clock vs. timestamp
