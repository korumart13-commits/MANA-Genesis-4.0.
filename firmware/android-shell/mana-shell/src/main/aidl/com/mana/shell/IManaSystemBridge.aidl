package com.mana.shell;
/** Signature-protected UI integration contract for a future MANA Brain app. */
interface IManaSystemBridge {
    int getApiVersion();
    String[] getSupportedScreens();
    boolean openScreen(String screenId);
}
