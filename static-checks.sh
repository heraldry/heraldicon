#!/usr/bin/env bash

rg reg-event -g '*.clj*' | grep -E -o '::?[a-z][a-z0-9-]*' | sort > /tmp/heraldry-reg-events

double_regs=$(</tmp/heraldry-reg-events uniq -c | grep -v ' 1 ')
if [ -n "$double_regs" ]; then
  echo "event duplicate registrations:"
  echo "$double_regs"
  echo
fi

rg -I reg-sub -g '*.clj*' | grep -E -o '::?[a-z][a-z0-9-]*' | sort > /tmp/heraldry-reg-subs

double_subs=$(</tmp/heraldry-reg-subs uniq -c | grep -v ' 1 ')
if [ -n "$double_subs" ]; then
  echo "subscription duplicate registrations:"
  echo "$double_subs"
  echo
fi

rg -I dispatch -g '*.clj*' | grep -E -v '(defn|dispatch effect)' | perl -pe 's/.*dispatch.*?[[](::?[a-z][a-z0-9-]*).*/\1/' | sort | uniq > /tmp/heraldry-dispatches

unknown_events=$(comm -23 /tmp/heraldry-dispatches /tmp/heraldry-reg-events)
if [ -n "$unknown_events" ]; then
  echo "unknown events:"
  echo "$unknown_events"
  echo
fi

unused_events=$(comm -13 /tmp/heraldry-dispatches /tmp/heraldry-reg-events)
if [ -n "$unused_events" ]; then
  echo "unused events:"
  echo "$unused_events"
  echo
fi

rg -I subscribe -g '*.clj*' | perl -pe 's/.*subscribe.*?[[](::?[a-z][a-z0-9-]*).*/\1/' | sort | uniq > /tmp/heraldry-subscribes

unknown_subs=$(comm -23 /tmp/heraldry-subscribes /tmp/heraldry-reg-subs)
if [ -n "$unknown_subs" ]; then
  echo "unknown subs:"
  echo "$unknown_subs"
  echo
fi

unused_subs=$(comm -13 /tmp/heraldry-subscribes /tmp/heraldry-reg-subs)
if [ -n "$unused_subs" ]; then
  echo "unused subs:"
  echo "$unused_subs"
  echo
fi

ignore="^(squiggly-paths|tangent-point)$"
rg -I '[(]def(|n|multi) ' -g '*.clj*' -g '*.edn' | perl -ne 'print if s/.*[(]def[^ ]* +([a-z0-9_-]{3,}).*/\1/' | grep -E -v "$ignore" | sort | uniq > /tmp/heraldry-symbols

unused_symbols=$(
for s in $(cat /tmp/heraldry-symbols); do
   rg -I -o '[a-z0-9_-]*'"$s"'[a-z0-9_-]*' -g '*.clj*' -g '*.edn' | grep -o '^'"$s"'$' | sort | uniq -c | grep ' 1 '
done
              )

if [ -n "$unused_symbols" ]; then
  echo "unused symbols:"
  echo "$unused_symbols"
  echo
fi

ignore="^(ttf|otf|selected)$"
rg -I -o '[.][a-z][a-z0-9_-]*' -g '*.css' assets/css | sed 's/^[.]//' | grep -E -v "$ignore" | sort | uniq > /tmp/heraldry-css-classes

unused_css_classes=$(
for s in $(cat /tmp/heraldry-css-classes); do
  found=$(rg -I -o '[."]'"$s"'[a-z0-9_-]*' -g '*.clj*')
  if [ -z "$found" ]; then
    echo "$s"
  fi
done
              )

if [ -n "$unused_css_classes" ]; then
  echo "unused css classes:"
  echo "$unused_css_classes"
  echo
fi
