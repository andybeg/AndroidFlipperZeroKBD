# Android Keyboard Bridge — convenience targets
#
# Override paths if needed:
#   make flipper-launch FIRMWARE_DIR=/path/to/flipperzero-firmware
#   make apk JAVA_HOME=/path/to/jdk-17

PROJECT_ROOT := $(abspath $(dir $(lastword $(MAKEFILE_LIST))))
FIRMWARE_DIR ?= $(PROJECT_ROOT)/../flipperzero-firmware
ANDROID_DIR := $(PROJECT_ROOT)/android
FLIPPER_APP_SRC := applications_user/android_keyboard_bridge
APK_DEBUG := $(ANDROID_DIR)/app/build/outputs/apk/debug/FlipperZeroKbd-debug.apk
APK_RELEASE := $(ANDROID_DIR)/app/build/outputs/apk/release/FlipperZeroKbd.apk

# Resolve JDK 17 without baking in a single machine-specific default.
# - `make apk JAVA_HOME=/path/to/jdk-17` wins (command-line override)
# - otherwise probe known installs + macOS java_home, verifying major version 17
# - ambient JAVA_HOME is ignored (often points at a newer JDK)
ifeq ($(origin JAVA_HOME),command line)
# keep caller-provided JAVA_HOME
else
JAVA_HOME := $(shell \
	for d in \
		/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home \
		/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home \
		/usr/lib/jvm/java-17-openjdk \
		/usr/lib/jvm/java-17-openjdk-amd64 \
		$$(/usr/libexec/java_home -v 17 2>/dev/null); do \
		if [ -n "$$d" ] && [ -x "$$d/bin/java" ] && \
			"$$d/bin/java" -version 2>&1 | grep -Eq 'version "17[\."]|version "1\.17\.'; then \
			printf '%s\n' "$$d"; \
			break; \
		fi; \
	done)
endif

GRADLE := $(if $(wildcard $(ANDROID_DIR)/gradlew),$(ANDROID_DIR)/gradlew,gradle)
FBT := $(FIRMWARE_DIR)/fbt
ifneq ($(strip $(JAVA_HOME)),)
GRADLE_ENV := JAVA_HOME="$(JAVA_HOME)" PATH="$(JAVA_HOME)/bin:$$PATH"
else
GRADLE_ENV :=
endif

.PHONY: help apk apk-install apk-release apk-release-install flipper-link flipper-build flipper-flash flipper-launch flipper-cli android-clean

help:
	@echo "Android Keyboard Bridge"
	@echo ""
	@echo "Android:"
	@echo "  make apk                 Build debug APK (FlipperZeroKbd-debug.apk)"
	@echo "  make apk-install         Build and install debug APK via adb"
	@echo "  make apk-release         Build release APK (FlipperZeroKbd.apk)"
	@echo "  make apk-release-install Build and install release APK via adb"
	@echo "  make android-clean       Clean Android build outputs"
	@echo ""
	@echo "Flipper (FIRMWARE_DIR=$(FIRMWARE_DIR)):"
	@echo "  make flipper-link     Symlink FAP into firmware applications_user/"
	@echo "  make flipper-build    Build FAP only"
	@echo "  make flipper-flash    Flash firmware from local checkout"
	@echo "  make flipper-launch   Build, upload, and launch FAP on Flipper"
	@echo "  make flipper-cli      Open Flipper USB CLI (connectivity check)"

define require_jdk17
	@if [ -n "$(JAVA_HOME)" ]; then \
		test -x "$(JAVA_HOME)/bin/java" || (echo "JDK 17 not found at $(JAVA_HOME). Install OpenJDK 17 or set JAVA_HOME." && exit 1); \
	elif ! command -v java >/dev/null 2>&1; then \
		echo "JDK 17 required. Install OpenJDK 17 or set JAVA_HOME." && exit 1; \
	fi
endef

apk:
	@test -d "$(ANDROID_DIR)" || (echo "Missing $(ANDROID_DIR)" && exit 1)
	$(require_jdk17)
	@test -x "$(ANDROID_DIR)/gradlew" || (echo "Missing android/gradlew. Run: cd android && gradle wrapper --gradle-version 8.7" && exit 1)
	cd "$(ANDROID_DIR)" && $(GRADLE_ENV) "./gradlew" assembleDebug
	@echo "APK: $(APK_DEBUG)"

apk-install: apk
	adb install -r "$(APK_DEBUG)"

apk-release:
	@test -d "$(ANDROID_DIR)" || (echo "Missing $(ANDROID_DIR)" && exit 1)
	$(require_jdk17)
	@test -x "$(ANDROID_DIR)/gradlew" || (echo "Missing android/gradlew. Run: cd android && gradle wrapper --gradle-version 8.7" && exit 1)
	cd "$(ANDROID_DIR)" && $(GRADLE_ENV) "./gradlew" assembleRelease
	@echo "APK: $(APK_RELEASE)"

apk-release-install: apk-release
	adb install -r "$(APK_RELEASE)"

android-clean:
	@test -d "$(ANDROID_DIR)" || exit 0
	cd "$(ANDROID_DIR)" && $(GRADLE_ENV) "./gradlew" clean

flipper-link:
	@test -d "$(FIRMWARE_DIR)" || (echo "Missing firmware repo: $(FIRMWARE_DIR)" && exit 1)
	ln -sfn "$(PROJECT_ROOT)/flipper/android_keyboard_bridge" "$(FIRMWARE_DIR)/$(FLIPPER_APP_SRC)"
	@ls -l "$(FIRMWARE_DIR)/$(FLIPPER_APP_SRC)"

flipper-build:
	@test -x "$(FBT)" || (echo "Missing fbt: $(FBT)" && exit 1)
	cd "$(FIRMWARE_DIR)" && ./fbt build APPSRC=$(FLIPPER_APP_SRC)

flipper-flash:
	@test -x "$(FBT)" || (echo "Missing fbt: $(FBT)" && exit 1)
	cd "$(FIRMWARE_DIR)" && ./fbt flash_usb_full

flipper-launch:
	@test -x "$(FBT)" || (echo "Missing fbt: $(FBT)" && exit 1)
	cd "$(FIRMWARE_DIR)" && ./fbt launch APPSRC=$(FLIPPER_APP_SRC)

flipper-cli:
	@test -d "$(FIRMWARE_DIR)" || (echo "Missing firmware repo: $(FIRMWARE_DIR)" && exit 1)
	cd "$(FIRMWARE_DIR)" && python3 scripts/serial_cli.py -p auto
