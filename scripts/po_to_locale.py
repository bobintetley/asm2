#!/usr/bin/python

"""
Given a po filename, returns a java locale, eg:
lt.po becomes lt_LT, en_GB.po becomes en_GB
"""

import sys

filename = sys.argv[1]
bits = filename.split(".")

if filename.find("_") == -1:
    print bits[0] + "_" + bits[0].upper()
else:
    print bits[0]

