.DEFAULT_GOAL := help

EXAMPLES := example_platforms
ANDROID_EXAMPLE := $(EXAMPLES)/android
IOS_EXAMPLE := $(EXAMPLES)/ios
IOS_PROJECT := $(IOS_EXAMPLE)/PrinterTesting.xcodeproj
IOS_BUILD := $(IOS_EXAMPLE)/build

help:
	@echo "@sincpro/printer-expo — targets"
	@echo ""
	@echo "  Contract (CI)"
	@echo "    init                      install pre-commit and npm dependencies"
	@echo "    verify-format             format, lint and typecheck; fails if files changed"
	@echo "    test                      unit tests (TypeScript layout/domain)"
	@echo "    update-version VERSION=x  bump package.json and android/build.gradle"
	@echo "    publish                   build and publish to npm"
	@echo ""
	@echo "  Module"
	@echo "    build                     compile src/ and plugin/ into build/"
	@echo "    format                    prettier + eslint --fix"
	@echo "    pack                      npm pack -> .tgz to install in an app (file:...)"
	@echo ""
	@echo "  Example apps (native, compile the module code directly)"
	@echo "    open-android              open $(ANDROID_EXAMPLE) in Android Studio"
	@echo "    open-ios                  open $(IOS_PROJECT) in Xcode"
	@echo "    example-android           install the debug app on the USB-connected Android device"
	@echo "    example-android-release   install the release app (R8 on, like production)"
	@echo "    example-android-build     build debug and release APKs without a device"
	@echo "    example-ios-build         compile the iOS app for devices (no signing; run it from Xcode)"
	@echo ""
	@echo "    clean                     remove build outputs"

prepare-environment:
	@pipx install pre-commit
	@pipx ensurepath
	@pre-commit install

init: prepare-environment
	@npm install

build:
	@npm run build

format:
	@npx prettier --write . --log-level warn
	@npx eslint src/ plugin/src/ --fix

verify-format: format
	@npx eslint src/ plugin/src/ --max-warnings 0
	@npm run typecheck --silent
	@if ! git diff --quiet; then \
	  echo >&2 "✘ El formateo ha modificado archivos. Por favor agrégalos al commit."; \
	  git --no-pager diff --name-only HEAD -- >&2; \
	  exit 1; \
	fi
	@echo "✓ Format verification passed"

test:
	@CI=1 npm test --silent

update-version:
ifndef VERSION
	$(error VERSION is required. Usage: make update-version VERSION=1.2.3)
endif
	@npm version $(VERSION) --no-git-tag-version --allow-same-version
	@if [ "$$(uname)" = "Darwin" ]; then \
		sed -i '' "s/^version = '[^']*'/version = '$(VERSION)'/; s/versionName \"[^\"]*\"/versionName \"$(VERSION)\"/" android/build.gradle; \
	else \
		sed -i "s/^version = '[^']*'/version = '$(VERSION)'/; s/versionName \"[^\"]*\"/versionName \"$(VERSION)\"/" android/build.gradle; \
	fi
	@echo "✓ Version $(VERSION) set in package.json and android/build.gradle (podspec reads package.json)"

publish: build
	@echo "📦 Publishing to npm..."
	@if [ -n "$$NPM_TOKEN" ]; then \
		echo "//registry.npmjs.org/:_authToken=$$NPM_TOKEN" > .npmrc.tmp; \
		chmod 600 .npmrc.tmp; \
		npm publish --access public --userconfig .npmrc.tmp; \
		rm -f .npmrc.tmp; \
	else \
		npm publish --access public; \
	fi
	@echo "✓ Published"

pack: build
	@npm pack

open-android:
	@open -a "Android Studio" $(ANDROID_EXAMPLE)

open-ios:
	@open $(IOS_PROJECT)

example-android:
	@cd $(ANDROID_EXAMPLE) && ./gradlew :app:installDebug

example-android-release:
	@cd $(ANDROID_EXAMPLE) && ./gradlew :app:installRelease

example-android-build:
	@cd $(ANDROID_EXAMPLE) && ./gradlew :app:assembleDebug :app:assembleRelease

example-ios-build:
	@xcodebuild -project $(IOS_PROJECT) -scheme PrinterTesting -configuration Debug \
		-destination 'generic/platform=iOS' CODE_SIGNING_ALLOWED=NO \
		-derivedDataPath $(IOS_BUILD)/dd build | tail -3

clean:
	@rm -rf build plugin/build android/build $(IOS_BUILD)
	@cd $(ANDROID_EXAMPLE) && ./gradlew clean --quiet || true
	@echo "✓ Cleaned"

deploy:
	@echo "Deploy not applicable for library modules"

.PHONY: help prepare-environment init build format verify-format test update-version publish pack \
	open-android open-ios example-android example-android-release example-android-build \
	example-ios-build clean deploy
