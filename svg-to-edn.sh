#!/usr/bin/env zsh -e

rm -rf main/data/charges

cd assets/optimized-svgs/charges
rm -rf hicv
lein hicv 2clj **/*.svg
perl -p -i \
  -e 's/clippathunits/clip-path-units/g;' \
  -e 's/spreadmethod/spread-method/g;' \
  -e 's/gradientunits/gradient-units/g;' \
  -e 's/gradienttransform/gradient-transform/g;' \
  -e 's/xlink:href/xlink-href/g;' \
  -e 's/maskunits/mask-units/g;' hicv/*

for f in **/*.svg; do
  relpath="$(dirname "$f")"
  filename="$(basename "$f")"
  filename="${filename%.*}"
  output="../../../main/data/charges/$relpath/$filename.edn"
  mkdir -p "$(dirname "$output")"
  echo "writing $output"
  mv "hicv/$filename.clj" "$output"
done

rm -rf hicv
