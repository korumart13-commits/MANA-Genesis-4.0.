package com.mana.shell;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import com.mana.controlcenter.ManaShadeActivity;
import com.mana.launcher.ManaLockScreenPreviewActivity;

/** Narrow signature-protected UI bridge; no hidden device-control privileges. */
public final class ManaSystemBridgeService extends Service {
    private final IManaSystemBridge.Stub binder=new IManaSystemBridge.Stub(){
        @Override public int getApiVersion(){return 1;}
        @Override public String[] getSupportedScreens(){return new String[]{"home","settings","control_center","notification_panel_preview","lock_screen_preview"};}
        @Override public boolean openScreen(String id){
            Intent i;
            if("notification_panel_preview".equals(id)) i=new Intent(ManaSystemBridgeService.this,ManaShadeActivity.class);
            else if("lock_screen_preview".equals(id)) i=new Intent(ManaSystemBridgeService.this,ManaLockScreenPreviewActivity.class);
            else if("control_center".equals(id)) i=new Intent().setClassName(ManaSystemBridgeService.this,"com.mana.controlcenter.ManaControlCenterActivity");
            else if("settings".equals(id)) i=new Intent().setClassName(ManaSystemBridgeService.this,"com.mana.settings.ManaSettingsActivity");
            else if("home".equals(id)) i=new Intent().setClassName(ManaSystemBridgeService.this,"com.mana.launcher.ManaHomeActivity");
            else return false;
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(i); return true;
        }
    };
    @Override public IBinder onBind(Intent intent){return binder;}
}
