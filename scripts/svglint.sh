#!/bin/bash

for f in "$@"; do
  echo $f
  xmllint --format "$f" --output "$f"
done
