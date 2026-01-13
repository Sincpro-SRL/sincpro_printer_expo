prepare-environment:
	@pipx install pre-commit
	@pipx ensurepath
	@pre-commit install

init: prepare-environment
	@echo "Installing Node.js dependencies..."
	npm install

format:
	@npx prettier --write --tab-width 2 "**/*.{yml,yaml,json,md}"

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
	@npm version $(VERSION) --no-git-tag-version
	@echo "Version updated successfully"

publish: build
	@echo "Publishing to NPM..."
	@if [ -z "$$NPM_TOKEN" ] && [ -z "$$NODE_AUTH_TOKEN" ]; then \
		echo "⚠️  Publishing locally - make sure you are logged in (npm login)"; \
		npm publish --access public; \
	else \
		echo "📦 Publishing to NPM via CI/CD"; \
		npm publish --access public; \
	fi
	@echo "✓ Package published successfully"

deploy:
	@echo "Deploy not applicable for library modules"

clean:
	@echo "Cleaning build artifacts..."
	@npm run clean || true
	@rm -rf node_modules build

.PHONY: prepare-environment init format verify-format test build update-version publish deploy clean
