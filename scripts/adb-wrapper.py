import subprocess
import sys
import re

# Note: no output will be printed until the entire test suite has finished
p = subprocess.Popen(sys.argv[1], shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, universal_newlines=True)
stdoutResult, stderrResult = p.communicate()

successRegex = re.compile('OK \(\d+ tests\)')

print(stdoutResult)
print(stderrResult)

if successRegex.search(stderrResult + stdoutResult):
    sys.exit(0)
else:
    sys.exit(1)
