#!/usr/bin/env zsh -e

rm -rf main/data/charges

(
  cd assets/optimized-svgs/charges
  for relpath in **/*(/); do
    if [ $(ls -1 "$relpath" | grep "[.]svg$" | wc -l) -eq 0 ]; then
      continue
    fi

    rm -rf hicv
    lein hicv 2clj "$relpath"/*.svg
    perl -p -i \
      -e 's/clippathunits/clip-path-units/g;' \
      -e 's/spreadmethod/spread-method/g;' \
      -e 's/gradientunits/gradient-units/g;' \
      -e 's/gradienttransform/gradient-transform/g;' \
      -e 's/xlink:href/xlink-href/g;' \
      -e 's/maskunits/mask-units/g;' hicv/*

    for f in hicv/*; do
      filename="$(basename "$f")"
      filename="${filename%.*}"
      output="../../../main/data/charges/$relpath/$filename.edn"
      mkdir -p "$(dirname "$output")"
      echo "writing $output"
      mv "$f" "$output"
    done

  done
  rm -rf hicv
)
