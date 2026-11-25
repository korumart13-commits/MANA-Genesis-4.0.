# MANA Genesis 4.0 — Android application sources

This directory contains the first Android-framework-facing source for the integrated MANA UI.

## Packages

- ManaLauncher (com.mana.launcher): Android HOME activity, dark navy MANA identity, opens the MANA assistant and lists launchable user apps.
- ManaAssistant (com.mana.assistant): MANA-branded assistant shell and message-entry UI. It explicitly reports that no inference runtime is connected; it does not fake AI responses or transmit prompts.

## Build integration

Both modules are Soong android_app modules. Include system/mana/mana_product.mk from the eventual verified Android 16 product makefile to add them to PRODUCT_PACKAGES.

They are product apps, not privileged apps: no privileged=true, special permissions, root access, overlay permission, accessibility service, contacts, microphone, camera, or network permission is requested. Development modules use the default build certificate, not the platform certificate. A production release must use the owner's controlled release-signing process; private signing keys must never be committed.

## Not yet present

- AOSP checkout and verified RMX5566RU product/device tree.
- Binder connection from the assistant shell to MANA Brain.
- LiteRT-LM runtime, model weights and model manager.
- Avatar assets/runtime, voice service and persistent encrypted memory.
- A real device build or installation.

The launcher and assistant are source modules for the integrated OS. No standalone APK workflow is started.
