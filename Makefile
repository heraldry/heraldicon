.PHONY: release-frontend-prod release-backend-prod dev-local

release-frontend-prod:
	mkdir -p release
	rm -rf release/output 2> /dev/null || true
	cp -r frontend/assets release/output
	rm -rf release/output/js/generated 2> /dev/null || true
	STAGE=prod npx shadow-cljs release frontend --config-merge '{:output-dir "release/output/js/generated"}'

deploy-frontend-prod: release-frontend-prod
	aws --profile heraldry-serverless s3 sync --acl public-read release/output s3://heraldry.digital && aws --profile heraldry cloudfront create-invalidation --distribution-id $(shell aws --profile heraldry cloudfront list-distributions | jq -r '.DistributionList.Items[] | select([.Aliases.Items[] == "heraldry.digital"] | any) | .Id') --paths '/*'

release-backend-prod:
	rm -rf backend/generated/* 2> /dev/null || true
	rm -rf backend/node_modules/sharp 2> /dev/null || true
	cp -r backend/linux-sharp/ backend/node_modules/sharp
	STAGE=prod npx shadow-cljs release backend

deploy-backend-prod: release-backend-prod
	cd backend && npx sls deploy --stage prod

dev-local:
	npx shadow-cljs watch frontend backend test
