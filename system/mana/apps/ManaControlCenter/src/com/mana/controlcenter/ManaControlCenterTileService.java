package com.mana.controlcenter;

import android.content.Intent;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/** User-addable Quick Settings shortcut to the non-privileged MANA Control Center. */
public final class ManaControlCenterTileService extends TileService {
    @Override public void onStartListening() {
        super.onStartListening();
        Tile tile = getQsTile();
        if (tile != null) {
            tile.setLabel("MANA");
            tile.setState(Tile.STATE_ACTIVE);
            tile.updateTile();
        }
    }

    @Override public void onClick() {
        super.onClick();
        unlockAndRun(() -> {
            Intent intent = new Intent(this, ManaShadeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivityAndCollapse(intent);
        });
    }
}
