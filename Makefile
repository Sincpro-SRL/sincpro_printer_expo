prepare-environment:
	@pipx install pre-commit
	@pipx ensurepath
	@pre-commit install

init: prepare-environment
	@echo "Installing Node.js dependencies..."
	npm install

format:
	@npx prettier --write --tab-width 2 "**/*.{yml,yaml,json}"
	@echo "Formatting Kotlin code..."
	@ktlint --format "android/**/*.kt" || true

verify-format: format
	@if ! git diff --quiet; then \
	  echo >&2 "✘ El formateo ha modificado archivos. Por favor agrégalos al commit."; \
	  git --no-pager diff --name-only HEAD -- >&2; \
	  exit 1; \
	fi
	@echo "✓ Format verification passed"

test:
	@echo "Running tests..."

# ============================================================================
# Build - Single command that builds everything and syncs artifacts
# ============================================================================

SDK_DIR := sincpro-printer-sdk
SDK_AAR := $(SDK_DIR)/build/outputs/aar/sincpro-printer-sdk-release.aar
SDK_LIBS := $(SDK_DIR)/libs
TEST_APP_LIBS := sincpro-printer-test-app/app/libs
EXPO_MODULE_LIBS := android/libs

build:
	@echo "🔨 Building sincpro-printer-sdk (Android)..."
	@cd $(SDK_DIR) && ./gradlew assembleRelease --quiet
	@echo "📦 Syncing artifacts to sincpro-printer-test-app..."
	@mkdir -p $(TEST_APP_LIBS)
	@rm -rf $(TEST_APP_LIBS)/bixolon 2>/dev/null || true
	@rm -f $(TEST_APP_LIBS)/sincpro-printer-sdk-release.aar 2>/dev/null || true
	@cp $(SDK_AAR) $(TEST_APP_LIBS)/sincpro-printer-sdk.aar
	@cp $(SDK_LIBS)/pdf/*.aar $(TEST_APP_LIBS)/ 2>/dev/null || true
	@echo "📦 Syncing artifacts to expo module (android/libs)..."
	@mkdir -p $(EXPO_MODULE_LIBS)/pdf
	@cp $(SDK_AAR) $(EXPO_MODULE_LIBS)/sincpro-printer-sdk.aar
	@cp $(SDK_LIBS)/pdf/*.aar $(EXPO_MODULE_LIBS)/pdf/ 2>/dev/null || true
	@rm -f $(EXPO_MODULE_LIBS)/*.jar 2>/dev/null || true
	@echo "🔨 Building TypeScript..."
	@npm run build
	@echo "✓ Build complete:"
	@echo "  Android:"
	@echo "    → $(TEST_APP_LIBS)/sincpro-printer-sdk.aar (includes JARs)"
	@echo "    → $(TEST_APP_LIBS)/Bixolon_pdf.aar"
	@echo "    → $(EXPO_MODULE_LIBS)/sincpro-printer-sdk.aar (includes JARs)"
	@echo "    → $(EXPO_MODULE_LIBS)/pdf/Bixolon_pdf.aar"
	@echo "  TypeScript:"
	@echo "    → build/*.js + build/*.d.ts"

update-version:
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
	@echo "Publishing to NPM..."
	@if [ -n "$$NPM_TOKEN" ]; then \
		echo "📦 Publishing to NPM via CI/CD with NPM_TOKEN"; \
		echo "//registry.npmjs.org/:_authToken=$$NPM_TOKEN" > .npmrc.tmp; \
		chmod 600 .npmrc.tmp; \
		npm publish --access public --userconfig .npmrc.tmp; \
		rm -f .npmrc.tmp; \
	elif [ -n "$$NODE_AUTH_TOKEN" ]; then \
		echo "📦 Publishing to NPM via CI/CD with NODE_AUTH_TOKEN"; \
		npm publish --access public; \
	else \
		echo "⚠️  Publishing locally - make sure you are logged in (npm login)"; \
		npm publish --access public; \
	fi
	@echo "✓ Package published successfully"

deploy:
	@echo "Deploy not applicable for library modules"

clean:
	@echo "Cleaning build artifacts..."
	@npm run clean || true
	@rm -rf node_modules build
	@cd android && rm -rf .gradle build .idea || true
	@rm -rf $(SDK_DIR)/.gradle $(SDK_DIR)/build
	@rm -rf sincpro-printer-test-app/.gradle sincpro-printer-test-app/app/build
	@rm -rf $(TEST_APP_LIBS)/*.aar


.PHONY: prepare-environment init format verify-format test build update-version publish deploy clean
