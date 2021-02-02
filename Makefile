.PHONY: release

release-frontend-prod:
	mkdir -p release
	rm -rf release/output 2> /dev/null || true
	cp -r frontend/assets release/output
	rm -rf release/output/js/generated 2> /dev/null || true
	STAGE=prod npx shadow-cljs release frontend --config-merge '{:output-dir "release/output/js/generated"}'


release-backend-prod:
	rm -rf backend/generated/* 2> /dev/null || true
	rm -rf backend/node_modules/sharp 2> /dev/null || true
	cp -r backend/linux-sharp/ backend/node_modules/sharp
	STAGE=prod npx shadow-cljs release backend
