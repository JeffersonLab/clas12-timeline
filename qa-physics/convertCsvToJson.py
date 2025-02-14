import sys
import pandas as pd
import os
import json

# Check for input path argument
inpath = "/path/to/my/file.csv"
fillna_value = -1.0
if len(sys.argv)<2:
    print("Usage: python3",os.path.abspath(__file__),"</path/to/file.csv>")
    sys.exit(101)
if len(sys.argv)>1:
    inpath = os.path.abspath(sys.argv[1])
if len(sys.argv)>2:
    fillna_value = float(sys.argv[2])

# Read from CSV
df = pd.read_csv(inpath)
df = df.fillna(fillna_value)
data = {key: df[key].to_list() for key in df.keys()}

# Write to JSON
outpath = inpath.replace('.csv','.json')
print("Writing JSON file ",outpath)
with open(outpath, 'w', encoding='utf-8') as f:
    json.dump(data, f, ensure_ascii=False, indent=4)
