#!/usr/bin/env zsh -e

cd assets/charges

cd originals
for f in **/*.svg; do
  adjusted_file="../adjusted/$f"
  if [ -f "$adjusted_file" ]; then
    continue
  fi
  echo "copying $f..."
  outdir="$(dirname "$adjusted_file")"
  mkdir -p "$outdir"
  cp "$f" "$adjusted_file"
done
