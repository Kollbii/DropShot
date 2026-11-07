SHELL := /bin/bash

# Project paths (resolved relative to this Makefile in the repository root)
ROOT_DIR      := $(patsubst %/,%,$(dir $(abspath $(lastword $(MAKEFILE_LIST)))))
ANDROID_DIR   := $(ROOT_DIR)/Android
LOCAL_PROPS   := $(ANDROID_DIR)/local.properties

# App configuration
AVD_NAME      ?= DropShot_Phone
PKG_NAME      := pl.maciek.squashscoreboard
MAIN_ACTIVITY := $(PKG_NAME)/.MainActivity
APK_OUT       := $(ANDROID_DIR)/app/build/outputs/apk/debug/app-debug.apk
APK_ROOT      := $(ANDROID_DIR)/Punkty-Squash.apk

# Resolve Android SDK path from Android/local.properties, environment, or common macOS locations
SDK_FROM_PROPS := $(shell [ -f "$(LOCAL_PROPS)" ] && sed -n 's/^sdk\.dir=//p' "$(LOCAL_PROPS)" | tail -n 1 | tr -d '\r')
SDK_DIR        := $(firstword $(wildcard \
	$(SDK_FROM_PROPS) \
	$(ANDROID_HOME) \
	$(ANDROID_SDK_ROOT) \
	/opt/homebrew/share/android-commandlinetools \
	$(HOME)/Library/Android/sdk \
))

# Resolve JDK 17 (required by Android Gradle Plugin 8.x when Homebrew default OpenJDK is newer)
JDK17_DIR      := /Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home
ifneq ($(wildcard $(JDK17_DIR)/bin/java),)
export JAVA_HOME := $(JDK17_DIR)
endif

ifneq ($(SDK_DIR),)
export ANDROID_HOME     := $(SDK_DIR)
export ANDROID_SDK_ROOT := $(SDK_DIR)
export PATH             := $(SDK_DIR)/platform-tools:$(SDK_DIR)/emulator:$(SDK_DIR)/cmdline-tools/latest/bin:/opt/homebrew/bin:/usr/local/bin:$(PATH)
else
export PATH             := /opt/homebrew/bin:/usr/local/bin:$(PATH)
endif

ADB      := $(shell command -v adb 2>/dev/null || echo "$(SDK_DIR)/platform-tools/adb")
EMULATOR := $(shell command -v emulator 2>/dev/null || echo "$(SDK_DIR)/emulator/emulator")
GRADLE   := $(shell if [ -x "$(ANDROID_DIR)/gradlew" ]; then echo "$(ANDROID_DIR)/gradlew"; else command -v gradle 2>/dev/null || echo "/opt/homebrew/bin/gradle"; fi)

.PHONY: help check-tools start build run reinstall watch status stop

help:
	@echo "Available commands:"
	@echo "  make start      - Boot emulator, wait until ready, build & launch app"
	@echo "  make run        - Compile latest code, install to device/emulator, and launch app"
	@echo "  make reinstall  - Uninstall existing app (fixes signature mismatch), then install & launch"
	@echo "  make build      - Compile debug APK and update $(APK_ROOT)"
	@echo "  make watch      - Auto-compile & relaunch app whenever source files change"
	@echo "  make status     - Check SDK/JDK paths, running emulators, ADB, and Gradle daemons"
	@echo "  make stop       - Safely shut down emulator, ADB server, and Gradle daemons"

check-tools:
	@if [ ! -d "$(ANDROID_DIR)" ]; then \
		echo "Error: Android project directory not found at $(ANDROID_DIR)"; \
		exit 1; \
	fi
	@if [ ! -x "$(GRADLE)" ]; then \
		echo "Error: gradle not found (checked $(GRADLE)). Install with: brew install gradle"; \
		exit 1; \
	fi
	@if [ ! -x "$(ADB)" ]; then \
		echo "Error: adb not found (checked $(ADB)). Check sdk.dir in $(LOCAL_PROPS)"; \
		exit 1; \
	fi

start: check-tools
	@if [ ! -x "$(EMULATOR)" ]; then \
		echo "Error: emulator binary not found at $(EMULATOR)"; \
		exit 1; \
	fi
	@echo "Checking if an Android emulator is already running..."
	@if ! "$(ADB)" devices | grep -q "^emulator-"; then \
		AVD_LIST="$$("$(EMULATOR)" -list-avds 2>/dev/null)"; \
		if [ -z "$$AVD_LIST" ]; then \
			echo "Error: No Android Virtual Devices (AVDs) found. Create one with avdmanager first."; \
			exit 1; \
		fi; \
		TARGET_AVD="$(AVD_NAME)"; \
		if ! echo "$$AVD_LIST" | grep -Fxq "$$TARGET_AVD"; then \
			TARGET_AVD="$$(echo "$$AVD_LIST" | head -n 1)"; \
			echo "AVD '$(AVD_NAME)' not found; falling back to '$$TARGET_AVD'."; \
		fi; \
		echo "Booting $$TARGET_AVD..."; \
		nohup "$(EMULATOR)" -avd "$$TARGET_AVD" >/dev/null 2>&1 & \
		echo "Waiting for Android system to finish booting..."; \
		"$(ADB)" wait-for-device; \
		BOOT_TIMEOUT=120; \
		until [ "$$("$(ADB)" -e shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do \
			sleep 1; \
			BOOT_TIMEOUT=$$((BOOT_TIMEOUT - 1)); \
			if [ "$$BOOT_TIMEOUT" -le 0 ]; then \
				echo "Error: Emulator boot timed out after 120 seconds."; \
				exit 1; \
			fi; \
		done; \
		echo "Emulator ready!"; \
	else \
		echo "Emulator is already running."; \
	fi
	@$(MAKE) run

run: check-tools
	@if ! "$(ADB)" get-state >/dev/null 2>&1; then \
		echo "Error: No connected Android device or running emulator found. Run 'make start' first."; \
		exit 1; \
	fi
	@echo "Compiling and installing $(PKG_NAME)..."
	@if ! "$(GRADLE)" -p "$(ANDROID_DIR)" installDebug; then \
		echo "Install failed (possibly signature mismatch). Uninstalling existing $(PKG_NAME) and retrying..."; \
		"$(ADB)" uninstall $(PKG_NAME) >/dev/null 2>&1 || true; \
		"$(GRADLE)" -p "$(ANDROID_DIR)" installDebug; \
	fi
	@"$(ADB)" shell am force-stop $(PKG_NAME)
	@"$(ADB)" shell am start -n $(MAIN_ACTIVITY) >/dev/null
	@echo "App launched!"

reinstall: check-tools
	@echo "Uninstalling $(PKG_NAME)..."
	@-"$(ADB)" uninstall $(PKG_NAME) 2>/dev/null || true
	@$(MAKE) run

build:
	@if [ ! -x "$(GRADLE)" ]; then \
		echo "Error: gradle not found at $(GRADLE)"; \
		exit 1; \
	fi
	"$(GRADLE)" -p "$(ANDROID_DIR)" assembleDebug
	@if [ ! -f "$(APK_OUT)" ]; then \
		echo "Error: Expected APK not found at $(APK_OUT)"; \
		exit 1; \
	fi
	@cp "$(APK_OUT)" "$(APK_ROOT)"
	@echo "Updated $(APK_ROOT)"

watch: check-tools
	@echo "Watching $(ANDROID_DIR)/app/src for changes (Press Ctrl+C to stop)..."
	@if command -v entr >/dev/null 2>&1; then \
		find "$(ANDROID_DIR)/app/src" "$(ANDROID_DIR)/app/build.gradle" -type f | entr -r $(MAKE) run; \
	else \
		echo "(Note: 'entr' is not installed; using polling fallback every 2s)"; \
		LAST_SUM=""; \
		while true; do \
			NEW_SUM="$$(find "$(ANDROID_DIR)/app/src" "$(ANDROID_DIR)/app/build.gradle" -type f -exec stat -f '%m %N' {} + 2>/dev/null | shasum)"; \
			if [ -n "$$LAST_SUM" ] && [ "$$NEW_SUM" != "$$LAST_SUM" ]; then \
				$(MAKE) run || true; \
			fi; \
			LAST_SUM="$$NEW_SUM"; \
			sleep 2; \
		done; \
	fi

status:
	@echo "=== Environment & Paths ==="
	@echo "ANDROID_DIR : $(ANDROID_DIR)"
	@echo "SDK_DIR     : $(SDK_DIR)"
	@echo "JAVA_HOME   : $(JAVA_HOME)"
	@echo "ADB         : $(ADB)"
	@echo "EMULATOR    : $(EMULATOR)"
	@echo "GRADLE      : $(GRADLE)"
	@echo ""
	@echo "=== Connected ADB Devices ==="
	@if [ -x "$(ADB)" ]; then "$(ADB)" devices; else echo "adb not found"; fi
	@echo "=== Emulator Processes ==="
	@ps aux | grep -i -E "emulator|qemu" | grep -v grep || echo "No emulator running."
	@echo "=== Gradle Daemons ==="
	@if [ -x "$(GRADLE)" ]; then "$(GRADLE)" -p "$(ANDROID_DIR)" --status || true; else echo "gradle not found"; fi

stop:
	@echo "1. Stopping emulator..."
	@if [ -x "$(ADB)" ]; then \
		for serial in $$("$(ADB)" devices 2>/dev/null | awk '/^emulator-/{print $$1}'); do \
			"$(ADB)" -s "$$serial" emu kill 2>/dev/null || true; \
		done; \
	fi
	@sleep 2
	@-killall -9 qemu-system-aarch64 qemu-system-x86_64 2>/dev/null || true
	@echo "2. Stopping ADB server..."
	@if [ -x "$(ADB)" ]; then "$(ADB)" kill-server 2>/dev/null || true; fi
	@echo "3. Stopping Gradle background daemons..."
	@if [ -x "$(GRADLE)" ]; then "$(GRADLE)" -p "$(ANDROID_DIR)" --stop 2>/dev/null || true; fi
	@echo "All development resources cleaned up!"

