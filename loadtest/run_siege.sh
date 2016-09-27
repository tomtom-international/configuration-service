#!/bin/sh
siege --concurrent=20 --time=20s --benchmark --file=urls.txt --log=siege.log

