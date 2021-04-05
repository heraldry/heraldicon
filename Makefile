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
	aws --profile heraldry-serverless s3 sync --acl public-read $(FRONTEND_RELEASE_DIR) s3://heraldry.digital
	aws --profile heraldry-serverless s3 cp --acl public-read $(FRONTEND_RELEASE_DIR)/index.html s3://heraldry.digital/index.html --metadata-directive REPLACE --cache-control max-age=0,no-cache,no-store,must-revalidate --content-type text/html
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

check-debug-print-frontend:
	! rg println src
	! rg js/console src

check-dirty-frontend:
	git diff --quiet || (echo ". is dirty" && false)

check-debug-print-backend:
	! rg println backend/src
	! rg js/console backend/src

check-dirty-backend:
	cd backend && git diff --quiet || (echo "backend is dirty" && false)

check-before-deploy-frontend: check-debug-print-frontend check-dirty-frontend

check-before-deploy-backend: check-debug-print-frontend check-dirty-frontend check-debug-print-backend check-dirty-backend
