#!/usr/bin/env zsh -e

rm -rf assets/optimized-svgs/charges

(
  cd assets/charges/adjusted
  for relpath in **/*(/); do
    if [ $(ls -1 "$relpath" | grep "[.]svg$" | wc -l) -eq 0 ]; then
      continue
    fi

    outdir="../../optimized-svgs/charges/$relpath"
    mkdir -p "$outdir"
    svgo -f "$relpath" -o "$outdir"
  done
)
