prepare-environment:
	@pipx install pre-commit
	@pipx ensurepath
	@pre-commit install

init: prepare-environment
	@echo "Installing Node.js dependencies..."
	npm install

format:
	@npx prettier --write --tab-width 2 --ignore-path .prettierignore "**/*.{yml,yaml,json}" --ignore-unknown
	@echo "Kotlin formatting disabled - ktlint incompatible with Expo Modules API lambda syntax"
	@echo "  → Use IDE formatter (Android Studio/IntelliJ) for Kotlin files"
	@echo "  → See: https://kotlinlang.org/docs/coding-conventions.html"

verify-format: format
	@if ! git diff --quiet; then \
	  echo >&2 "✘ El formateo ha modificado archivos. Por favor agrégalos al commit."; \
	  git --no-pager diff --name-only HEAD -- >&2; \
	  exit 1; \
	fi
	@echo "✓ Format verification passed"

test:
	@echo "Running tests..."

# Get version from package.json
VERSION := $(shell node -p "require('./package.json').version")

SDK_DIR := sincpro-printer-sdk
SDK_AAR := $(SDK_DIR)/build/outputs/aar/sincpro-printer-sdk-release.aar
SDK_LIBS := $(SDK_DIR)/libs
TEST_APP_LIBS := sincpro-printer-test-app/app/libs
EXPO_MODULE_LIBS := android/libs

prebuild:
	@cd $(SDK_DIR) && ./gradlew assembleRelease --quiet

distribute-app-test:
	@mkdir -p $(TEST_APP_LIBS)
	@rm -rf $(TEST_APP_LIBS)/bixolon 2>/dev/null || true
	@rm -f $(TEST_APP_LIBS)/sincpro-printer-sdk-release.aar 2>/dev/null || true
	@cp $(SDK_AAR) $(TEST_APP_LIBS)/sincpro-printer-sdk.aar
	@cp $(SDK_LIBS)/pdf/*.aar $(TEST_APP_LIBS)/ 2>/dev/null || true

distribute-expo-module:
	@mkdir -p $(EXPO_MODULE_LIBS)
	@cp $(SDK_AAR) $(EXPO_MODULE_LIBS)/sincpro-printer-sdk.aar
	@cp $(SDK_LIBS)/pdf/*.aar $(EXPO_MODULE_LIBS)/ 2>/dev/null || true
	@rm -f $(EXPO_MODULE_LIBS)/*.jar 2>/dev/null || true


build: prebuild distribute-app-test distribute-expo-module
	@npm run build


android: build
	@echo "🚀 Building Android app..."
	@npx expo run:android

sync-versions:
	@echo "📌 Syncing version $(VERSION) across gradle files..."
	@if [ "$$(uname)" = "Darwin" ]; then \
		sed -i '' "s/version = '[^']*'/version = '$(VERSION)'/g" android/build.gradle; \
		sed -i '' "s/versionName \"[^\"]*\"/versionName \"$(VERSION)\"/g" android/build.gradle; \
		find sincpro-printer-sdk -name "gradle.properties" -exec sed -i '' "s/version=[^ ]*/version=$(VERSION)/g" {} \; ; \
	else \
		sed -i "s/version = '[^']*'/version = '$(VERSION)'/g" android/build.gradle; \
		sed -i "s/versionName \"[^\"]*\"/versionName \"$(VERSION)\"/g" android/build.gradle; \
		find sincpro-printer-sdk -name "gradle.properties" -exec sed -i "s/version=[^ ]*/version=$(VERSION)/g" {} \; ; \
	fi
	@echo "✓ Versions synced to $(VERSION)"


update-version: sync-versions
ifndef VERSION
	$(error VERSION is required. Usage: make update-version VERSION=1.2.3)
endif
	@echo "Updating version to $(VERSION) using npm..."
	@CURRENT_VERSION=$$(node -p "require('./package.json').version"); \
	if [ "$$CURRENT_VERSION" = "$(VERSION)" ]; then \
		echo "✓ Version is already $(VERSION), skipping update"; \
	else \
		npm version $(VERSION) --no-git-tag-version && echo "✓ Version updated successfully"; \
	fi

publish: build
	@echo "📦 Publishing to NPM..."
	@if [ -n "$$NPM_TOKEN" ]; then \
		echo "//registry.npmjs.org/:_authToken=$$NPM_TOKEN" > .npmrc.tmp; \
		chmod 600 .npmrc.tmp; \
		npm publish --access public --userconfig .npmrc.tmp; \
		rm -f .npmrc.tmp; \
	elif [ -n "$$NODE_AUTH_TOKEN" ]; then \
		npm publish --access public; \
	else \
		npm publish --access public; \
	fi
	@echo "✓ Published successfully"

deploy:
	@echo "Deploy not applicable for library modules"

clean:
	@npm run clean || true
	@rm -rf node_modules build
	@cd android && rm -rf .gradle build .idea || true
	@rm -rf $(SDK_DIR)/.gradle $(SDK_DIR)/build
	@rm -rf sincpro-printer-test-app/.gradle sincpro-printer-test-app/app/build
	@rm -rf $(TEST_APP_LIBS)/*.aar


.PHONY: prepare-environment init format verify-format test prebuild build sync-versions android distribute-app-test distribute-expo-module update-version publish deploy clean
