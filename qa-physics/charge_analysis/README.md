# Charge Analysis

From Bhawani Singh

`./analyze.py` Produces PNG files comparing the DAQ-gated FC charge determined from
- the `RUN::scaler` bank directly
- the livetime and ungated charge, by multiplication

### Example

Suppose we have reheated (`qtl reheat`) a skim file on `/cache` and
stored it at `/volatile/reheat/`. To compare results:

```bash
./analyze.py /cache/...../skim.hipo before_reheat
./analyze.py /volatile/reheat/skim.hipo after_reheat
ls -t *.png
```
