setup-sharp-linux:
	rm -rf backend/node_modules/sharp 2> /dev/null || true
	cp -r backend/linux-sharp/ backend/node_modules/sharp

setup-sharp-osx:
	rm -rf backend/node_modules/sharp > /dev/null || true
	ln -s ../osx-sharp backend/node_modules/sharp

# PROD

PROD_FRONTEND_RELEASE_DIR = frontend/build/prod
PROD_BACKEND_RELEASE_DIR = backend/build/prod
PROD_CONFIG = {:closure-defines {heraldry.config/stage "prod"}}}

prod-backend-release:
	rm -rf $(PROD_BACKEND_RELEASE_DIR) 2> /dev/null || true
	npx shadow-cljs release backend --config-merge '$(PROD_CONFIG)'

prod-backend-deploy: check-before-deploy-backend prod-backend-release
	make setup-sharp-linux
	cd backend && npx sls deploy --stage prod
	make setup-sharp-osx
	cd backend && git tag $(shell date +"deploy-backend-%Y-%m-%d_%H-%M-%S")
	git tag $(shell date +"deploy-backend-%Y-%m-%d_%H-%M-%S")

prod-frontend-release:
	rm -rf $(PROD_FRONTEND_RELEASE_DIR) 2> /dev/null || true
	mkdir -p $(PROD_FRONTEND_RELEASE_DIR)
	cp -r frontend/assets/* $(PROD_FRONTEND_RELEASE_DIR)
	rm -rf $(PROD_FRONTEND_RELEASE_DIR)/js/generated 2> /dev/null || true
	perl -p -i -e "s/__GIT-COMMIT-HASH__/$(shell git rev-parse --short HEAD)/" $(PROD_FRONTEND_RELEASE_DIR)/index.html
	npx shadow-cljs release frontend --config-merge '$(PROD_CONFIG)'

prod-frontend-deploy: check-before-deploy-frontend prod-frontend-release
	aws --profile heraldry-serverless s3 sync --acl public-read $(PROD_FRONTEND_RELEASE_DIR) s3://heraldry.digital
	aws --profile heraldry-serverless s3 cp --acl public-read $(PROD_FRONTEND_RELEASE_DIR)/index.html s3://heraldry.digital/index.html --metadata-directive REPLACE --cache-control max-age=0,no-cache,no-store,must-revalidate --content-type text/html
	git tag $(shell date +"deploy-frontend-%Y-%m-%d_%H-%M-%S")
	./invalidate-distribution.sh heraldry.digital

# STAGING

STAGING_FRONTEND_RELEASE_DIR = frontend/build/staging
STAGING_BACKEND_RELEASE_DIR = backend/build/staging
STAGING_CONFIG = {:closure-defines {heraldry.config/stage "staging"}}

staging-backend-release:
	rm -rf $(STAGING_BACKEND_RELEASE_DIR) 2> /dev/null || true
	npx shadow-cljs release backend --config-merge '$(STAGING_CONFIG)' --config-merge '{:output-to "./backend/build/staging/backend.js"}'

staging-backend-deploy: staging-backend-release
	make setup-sharp-linux
	cd backend && npx sls deploy --stage staging
	make setup-sharp-osx

staging-frontend-release:
	rm -rf $(STAGING_FRONTEND_RELEASE_DIR) 2> /dev/null || true
	mkdir -p $(STAGING_FRONTEND_RELEASE_DIR)
	cp -r frontend/assets/* $(STAGING_FRONTEND_RELEASE_DIR)
	rm -rf $(STAGING_FRONTEND_RELEASE_DIR)/js/generated 2> /dev/null || true
	perl -p -i -e "s/__GIT-COMMIT-HASH__/$(shell git rev-parse --short HEAD)/" $(STAGING_FRONTEND_RELEASE_DIR)/index.html
	npx shadow-cljs release frontend --config-merge '$(STAGING_CONFIG)' --config-merge '{:output-dir "./frontend/build/staging/js/generated"}'

staging-frontend-deploy: staging-frontend-release
	aws --profile heraldry-serverless s3 sync --acl public-read $(STAGING_FRONTEND_RELEASE_DIR) s3://staging.heraldry.digital
	aws --profile heraldry-serverless s3 cp --acl public-read $(STAGING_FRONTEND_RELEASE_DIR)/index.html s3://staging.heraldry.digital/index.html --metadata-directive REPLACE --cache-control max-age=0,no-cache,no-store,must-revalidate --content-type text/html
	./invalidate-distribution.sh staging.heraldry.digital

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
	! rg println src backend/src --glob=!src/**/*test*.cljs --glob=!backend/src/heraldry/manage.cljs
	! rg js/console backend/src

check-dirty-backend:
	cd backend && git diff --quiet || (echo "backend is dirty" && false)

check-before-deploy-frontend: check-debug-print-frontend check-dirty-frontend

check-before-deploy-backend: check-debug-print-frontend check-dirty-frontend check-debug-print-backend check-dirty-backend
