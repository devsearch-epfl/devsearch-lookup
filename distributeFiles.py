# Created by hubi on 4/28/15.
#
# This script applies 'consistent hashing' to our features. More about consistent hashing on
# http://www.tom-e-white.com//2007/11/consistent-hashing.html
# 
# installing the hash_ring module: 'sudo easy_install hash_ring'
# see http://amix.dk/blog/viewEntry/19367

import sys
import json
import os.path
import hash_ring
from hash_ring import *

args = sys.argv
if(len(args) < 3):
	raise Exception('''
\tNot enough arguments! Correct usage: 
\t - arg1 = path/to/features.json
\t - arg2, ..., argN = path/to/output.json''')
	sys.exit(0)

source  = args[1]
buckets = []

if(not os.path.isfile(source)):
	raise Exception('''
\tInput directory does not exist! Correct usage:
\t - arg1 = path/to/features.json
\t - arg2, ..., argN = path/to/output.json''')
	sys.exit(0)


for output in args[2::]:
	buckets.append(open(output, 'w+'))



# create hash ring
ring = HashRing(buckets)

nbFiles = 0

# assign each file to its bucket
with open(source) as f:
	for line in f:
		nbFiles = nbFiles + 1
		j = json.loads(line)
		bucket = ring.get_node(j['file'])
		bucket.write(line)
		if(nbFiles%500000 == 0):
			print 'Processed ' + str(nbFiles/1000) + 'k features.' 
	

print 'Total: ' + str(nbFiles) + ' features distributed. \n'

for output in buckets:
	output.close()


