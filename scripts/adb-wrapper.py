import subprocess
import sys
import re

# Note: no output will be printed until the entire test suite has finished
result = subprocess.run(sys.argv[1], shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, universal_newlines=True)

successRegex = re.compile('OK \(\d+ tests\)')

print(result.stderr)
print(result.stdout)

if successRegex.search(result.stderr + result.stdout):
    sys.exit(0)
else:
    sys.exit(1)
