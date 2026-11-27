# MANA Genesis 4.0 common Android 16 product fragment.
#
# Include this fragment from a real AOSP product definition only after the
# selected product's framework/vendor compatibility has been established.
# This is intentionally not an RMX5566RU lunch target and does not define a
# kernel, fstab, partition map, AVB chain or proprietary vendor configuration.

PRODUCT_BRAND := MANA
PRODUCT_MANUFACTURER := MANA
PRODUCT_MODEL := MANA Genesis 4.0
PRODUCT_SYSTEM_DEFAULT_PROPERTIES += \
    ro.mana.name=MANA \
    ro.mana.version=4.0 \
    ro.mana.codename=GENESIS

PRODUCT_SOONG_NAMESPACES += \
    vendor/mana/genesis/system/mana/interfaces \
    vendor/mana/genesis/system/mana/services/ManaBrainService \
    vendor/mana/genesis/system/mana/apps/ManaSettings \
    vendor/mana/genesis/system/mana/apps/ManaControlCenter

include vendor/mana/genesis/system/mana/mana_product.mk
