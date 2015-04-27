import sys
import json
from urllib.parse import unquote

for line in sys.stdin:
    feature, file, line = line.split(',')
    d = {
        'feature': unquote(feature),
        'file': unquote(file),
        'line': {"$numberLong": str(int(line))}
    }
    json_str = json.dumps(d)
    print(json_str)
