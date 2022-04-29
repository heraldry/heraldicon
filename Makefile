COMMIT = $(shell git rev-parse --short HEAD)

remove-backend-fonts:
	rm -rf backend/assets/font

copy-fonts-to-backend:
	mkdir -p backend/assets
	make remove-backend-fonts
	cp -r assets/font backend/assets

# PROD

PROD_FRONTEND_RELEASE_DIR = build/prod
PROD_BACKEND_RELEASE_DIR = backend/build/prod
PROD_CONFIG = {:closure-defines {heraldicon.config/stage "prod" heraldicon.config/commit "$(COMMIT)"}}}

prod-backend-release:
	rm -rf $(PROD_BACKEND_RELEASE_DIR) 2> /dev/null || true
	npx shadow-cljs release backend --config-merge '$(PROD_CONFIG)'

dirty-prod-backend-deploy: actual-prod-backend-deploy
prod-backend-deploy: check-before-deploy-backend actual-prod-backend-deploy

actual-prod-backend-deploy: prod-backend-release
	make copy-fonts-to-backend
	cd backend && npx sls deploy --stage prod
	cd backend && git tag $(shell date +"deploy-backend-%Y-%m-%d_%H-%M-%S")
	git tag $(shell date +"deploy-backend-%Y-%m-%d_%H-%M-%S")
	make remove-backend-fonts

prod-frontend-release:
	rm -rf $(PROD_FRONTEND_RELEASE_DIR) 2> /dev/null || true
	mkdir -p $(PROD_FRONTEND_RELEASE_DIR)
	cp -r assets/* $(PROD_FRONTEND_RELEASE_DIR)
	rm -rf $(PROD_FRONTEND_RELEASE_DIR)/js/generated 2> /dev/null || true
	perl -p -i -e "s/__GIT-COMMIT-HASH__/$(COMMIT)/" $(PROD_FRONTEND_RELEASE_DIR)/index.html
	npx shadow-cljs release frontend --config-merge '$(PROD_CONFIG)'

PROD_FRONTEND_DEV_DIR = build/dev-prod
PROD_DEV_CONFIG = {:closure-defines {heraldicon.config/stage "prod" heraldicon.config/commit "$(COMMIT)"}} :output-dir "$(PROD_FRONTEND_DEV_DIR)"}
dev-prod:
	rm -rf $(PROD_FRONTEND_DEV_DIR) 2> /dev/null || true
	mkdir -p $(PROD_FRONTEND_DEV_DIR)
	cp -r assets/* $(PROD_FRONTEND_DEV_DIR)
	rm -rf $(PROD_FRONTEND_DEV_DIR)/js/generated 2> /dev/null || true
	npx shadow-cljs watch frontend --config-merge '$(PROD_DEV_CONFIG)'

dirty-prod-frontend-deploy: actual-prod-frontend-deploy
prod-frontend-deploy: check-before-deploy-frontend actual-prod-frontend-deploy

actual-prod-frontend-deploy: prod-frontend-release
	./sync-with-s3.py $(PROD_FRONTEND_RELEASE_DIR) cdn.heraldicon.org
	aws --profile heraldry-serverless s3 cp --acl public-read $(PROD_FRONTEND_RELEASE_DIR)/index.html s3://cdn.heraldicon.org/index.html --metadata-directive REPLACE --cache-control max-age=0,no-cache,no-store,must-revalidate --content-type text/html
	git tag $(shell date +"deploy-frontend-%Y-%m-%d_%H-%M-%S")
	./invalidate-distribution.sh cdn.heraldicon.org

# STAGING

STAGING_FRONTEND_RELEASE_DIR = build/staging
STAGING_BACKEND_RELEASE_DIR = backend/build/staging
STAGING_CONFIG = {:closure-defines {heraldicon.config/stage "staging" heraldicon.config/commit "$(COMMIT)"}}

staging-backend-release:
	rm -rf $(STAGING_BACKEND_RELEASE_DIR) 2> /dev/null || true
	npx shadow-cljs release backend --config-merge '$(STAGING_CONFIG)' --config-merge '{:output-to "./backend/build/staging/backend.js"}'

staging-backend-deploy: staging-backend-release
	make copy-fonts-to-backend
	cd backend && npx sls deploy --stage staging
	make remove-backend-fonts

staging-frontend-release:
	rm -rf $(STAGING_FRONTEND_RELEASE_DIR) 2> /dev/null || true
	mkdir -p $(STAGING_FRONTEND_RELEASE_DIR)
	cp -r assets/* $(STAGING_FRONTEND_RELEASE_DIR)
	rm -rf $(STAGING_FRONTEND_RELEASE_DIR)/js/generated 2> /dev/null || true
	perl -p -i -e "s/__GIT-COMMIT-HASH__/$(COMMIT)/" $(STAGING_FRONTEND_RELEASE_DIR)/index.html
	npx shadow-cljs release frontend --config-merge '$(STAGING_CONFIG)' --config-merge '{:output-dir "./build/staging/js/generated"}'

staging-frontend-deploy: staging-frontend-release
	./sync-with-s3.py $(STAGING_FRONTEND_RELEASE_DIR) cdn.staging.heraldicon.org
	aws --profile heraldry-serverless s3 cp --acl public-read $(STAGING_FRONTEND_RELEASE_DIR)/index.html s3://cdn.staging.heraldicon.org/index.html --metadata-directive REPLACE --cache-control max-age=0,no-cache,no-store,must-revalidate --content-type text/html
	./invalidate-distribution.sh cdn.staging.heraldicon.org

# DEV

dev-local:
	npx shadow-cljs watch frontend backend test manage

dev-test:
	npx shadow-cljs watch test

check-debug-print-frontend:
	! rg println src --glob=!src/**/*test*.cljs
	! rg js/console src

check-dirty-frontend:
	git diff --quiet || (echo ". is dirty" && false)

check-debug-print-backend:
	! rg println src backend/src --glob=!src/**/*test*.cljs --glob=!backend/src/heraldicon/manage.cljs
	! rg js/console backend/src

check-dirty-backend:
	cd backend && git diff --quiet || (echo "backend is dirty" && false)

check-before-deploy-frontend: check-debug-print-frontend check-dirty-frontend

check-before-deploy-backend: check-debug-print-frontend check-dirty-frontend check-debug-print-backend check-dirty-backend

check-outdated:
	clojure -Sdeps '{:deps {olical/depot {:mvn/version "RELEASE"}}}' -M -m depot.outdated.main
