# Created by hubi on 4/28/15.
#
# This script applies 'consistent hashing' to our features. More about consistent hashing on
# http://www.tom-e-white.com//2007/11/consistent-hashing.html
# 
#
# Usage: 
# - first argument is the input json file containing the features
# - the other arguments are at least one path or tuple: bucket_x.json weight_x 
#   (weight_x is optional)
#
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
\t - arg2, ..., argN = path/to/output.json optionalWeight
''')
	sys.exit(0)

source  = args[1]
buckets = []
weights = {}

if(not os.path.isfile(source)):
	raise Exception('''
\tInput directory does not exist! Correct usage:
\t - arg1 = path/to/features.json
\t - arg2, ..., argN = path/to/output.json optionalWeight
''')
	sys.exit(0)


i = 2
while i < len(args):
	f = open(args[i], 'w+')
	buckets.append(f)

	if(i < len(args)-1 and args[i + 1].isdigit()):
		weights[f] = int(args[i + 1])
		i = i + 2
	else:
		weights[f] = 1
		i = i + 1




print buckets


# create hash ring
ring = HashRing(buckets, weights)

nbFiles = 0

# assign each file to its bucket
with open(source) as f:
	for line in f:
		nbFiles = nbFiles + 1
		j = json.loads(line)
		bucket = ring.get_node(j['file'])
		bucket.write(line)
		if(nbFiles%500000 == 0):
			print 'Processed ' + str(nbFiles/1000) + 'k features' 
	

print 'Totally ' + str(nbFiles) + ' features distributed \n'

for output in buckets:
	output.close()


