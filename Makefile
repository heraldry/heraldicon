.PHONY: setup-dev dev release

setup-dev:
	yarn install

dev:
	npx shadow-cljs watch main

release:
	rm -rf release/coad 2> /dev/null || true
	cp -r client-coat-of-arms/assets/ release/coad
	rm -rf release/coad/js/generated 2> /dev/null || true
	npx shadow-cljs release client-coat-of-arms --config-merge '{:output-dir "release/coad/js/generated"}'

