prepare-environment:
	@pipx install pre-commit
	@pipx ensurepath
	@pre-commit install

init: prepare-environment
	@echo "Installing Node.js dependencies..."
	npm install

format:
	@npx prettier --write --tab-width 2 "**/*.{yml,yaml,json,md}"
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
	@npm test || echo "No tests configured yet"

build:
	@echo "Building project..."
	@npm run clean || true
	@npm run build
	@echo "✓ Build completed successfully"

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

clean-android:
	@echo "Cleaning Android Studio cache and build files..."
	@cd android && rm -rf .gradle .idea build/ gradle/ gradlew gradlew.bat
	@echo "✓ Android cache cleaned. Restart Android Studio and sync again."

android-env:
	@echo "Setting up Android Studio development environment..."
	@echo "Copying config files from android-dev-env/ to android/..."
	@cp android-dev-env/settings.gradle android/settings.gradle
	@echo "✓ Copied settings.gradle"
	@cp android-dev-env/gradle.properties android/gradle.properties
	@echo "✓ Copied gradle.properties"
	@echo ""
	@echo "✓ Android environment ready."
	@echo "  Open 'android/' folder in Android Studio."


.PHONY: prepare-environment init format verify-format test build update-version publish deploy clean clean-android android-env
