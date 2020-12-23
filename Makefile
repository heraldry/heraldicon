.PHONY: setup-dev dev release

setup-dev:
	yarn install

dev:
	npx shadow-cljs watch main

release:
	rm -rf release/coad 2> /dev/null || true
	cp -r main/ release/coad
	rm -rf release/coad/js/generated 2> /dev/null || true
	npx shadow-cljs release main --config-merge '{:output-dir "release/coad/js/generated"}'

generate-svg-clj:
	./optimize-svgs.sh
	./svg-to-clj.sh

generate-charge-map:
	./generate-charge-map.py
