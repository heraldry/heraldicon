.PHONY: release-frontend-prod release-backend-prod dev-local

FRONTEND_RELEASE_DIR = frontend/build/prod
BACKEND_RELEASE_DIR = backend/build/prod

release-frontend-prod:
	rm -rf $(FRONTEND_RELEASE_DIR) 2> /dev/null || true
	mkdir -p $(FRONTEND_RELEASE_DIR)
	cp -r frontend/assets/* $(FRONTEND_RELEASE_DIR)
	rm -rf $(FRONTEND_RELEASE_DIR)/js/generated 2> /dev/null || true
	perl -p -i -e "s/__GIT-COMMIT-HASH__/$(shell git rev-parse --short HEAD)/" $(FRONTEND_RELEASE_DIR)/index.html
	STAGE=prod npx shadow-cljs release frontend

setup-sharp-linux:
	rm -rf backend/node_modules/sharp 2> /dev/null || true
	cp -r backend/linux-sharp/ backend/node_modules/sharp

setup-sharp-osx:
	rm -rf backend/node_modules/sharp > /dev/null || true
	ln -s ../osx-sharp backend/node_modules/sharp

deploy-frontend-prod: check-before-deploy-frontend release-frontend-prod
	aws --profile heraldry-serverless s3 sync --acl public-read $(FRONTEND_RELEASE_DIR) s3://heraldry.digital && aws --profile heraldry cloudfront create-invalidation --distribution-id $(shell aws --profile heraldry cloudfront list-distributions | jq -r '.DistributionList.Items[] | select([.Aliases.Items[] == "heraldry.digital"] | any) | .Id') --paths '/*' | cat
	git tag $(shell date +"deploy-frontend-%Y-%m-%d_%H-%M-%S")

release-backend-prod:
	rm -rf $(BACKEND_RELEASE_DIR) 2> /dev/null || true
	STAGE=prod npx shadow-cljs release backend

deploy-backend-prod: check-before-deploy-backend release-backend-prod
	make setup-sharp-linux
	cd backend && npx sls deploy --stage prod
	make setup-sharp-osx
	cd backend && git tag $(shell date +"deploy-backend-%Y-%m-%d_%H-%M-%S")
	git tag $(shell date +"deploy-backend-%Y-%m-%d_%H-%M-%S")

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
