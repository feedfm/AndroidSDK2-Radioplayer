#!/bin/bash

if [[ $# != 1 ]]; then
  echo usage:
  echo   $0 '#color'
  echo
  echo '(make sure to quote hex color)'
  exit 1
fi

COLOR=$1
OUT=../../playeractivity/src/main/res

for i in `find masks -name \*.png`; do
  mkdir -p ${OUT}/`dirname $i`
  convert $i -background $COLOR -alpha shape $OUT/$i
done
  
