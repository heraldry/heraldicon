.PHONY: release-frontend-prod release-backend-prod dev-local

release-frontend-prod:
	mkdir -p release
	rm -rf release/output 2> /dev/null || true
	cp -r frontend/assets release/output
	rm -rf release/output/js/generated 2> /dev/null || true
	perl -p -i -e "s/__GIT-COMMIT-HASH__/$(shell git rev-parse --short HEAD)/" release/output/index.html
	STAGE=prod npx shadow-cljs release frontend --config-merge '{:output-dir "release/output/js/generated"}'

deploy-frontend-prod: check-before-deploy-frontend release-frontend-prod
	aws --profile heraldry-serverless s3 sync --acl public-read release/output s3://heraldry.digital && aws --profile heraldry cloudfront create-invalidation --distribution-id $(shell aws --profile heraldry cloudfront list-distributions | jq -r '.DistributionList.Items[] | select([.Aliases.Items[] == "heraldry.digital"] | any) | .Id') --paths '/*' | cat
	git tag $(shell date +"deploy-frontend-%Y-%m-%d_%H-%M-%S")

release-backend-prod:
	rm -rf backend/generated/* 2> /dev/null || true
	rm -rf backend/node_modules/sharp 2> /dev/null || true
	cp -r backend/linux-sharp/ backend/node_modules/sharp
	STAGE=prod npx shadow-cljs release backend --config-merge '{:output-to "backend/release/backend.js"}'

deploy-backend-prod: check-before-deploy-backend release-backend-prod
	cd backend && npx sls deploy --stage prod
	cd backend && git tag $(shell date +"deploy-backend-%Y-%m-%d_%H-%M-%S")
	git tag $(shell date +"deploy-backend-%Y-%m-%d_%H-%M-%S")
	rm -rf backend/node_modules/sharp > /dev/null || true
	ln -s ../osx-sharp backend/node_modules/sharp

dev-local:
	npx shadow-cljs watch frontend backend

dev-test:
	npx shadow-cljs watch test

check-println-frontend:
	! rg println src

check-dirty-frontend:
	git diff --quiet || (echo ". is dirty" && false)

check-println-backend:
	! rg println backend/src

check-dirty-backend:
	cd backend && git diff --quiet || (echo "backend is dirty" && false)

check-before-deploy-frontend: check-println-frontend check-dirty-frontend

check-before-deploy-backend: check-println-frontend check-dirty-frontend check-println-backend check-dirty-backend
