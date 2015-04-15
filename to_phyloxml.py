#!/usr/bin/env python
from Bio import Phylo
import sys

tree = Phylo.parse(sys.argv[1],'newick')
Phylo.write(tree, sys.argv[2], 'phyloxml')

