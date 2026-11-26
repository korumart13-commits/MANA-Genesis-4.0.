package com.mana.controlcenter;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

/** MANA panel design screen; it does not replace privileged Android SystemUI. */
public final class ManaShadeActivity extends Activity {
    private static final int BG=Color.rgb(5,15,31), PANEL=Color.rgb(16,43,80), TILE=Color.rgb(25,68,112);
    private static final int CYAN=Color.rgb(84,200,245), WHITE=Color.rgb(234,244,255), MUTED=Color.rgb(145,169,197);
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(BG);
        LinearLayout root=new LinearLayout(this); root.setOrientation(1); root.setPadding(dp(20),dp(28),dp(20),dp(26)); scroll.addView(root);
        root.addView(text("MANA  /  GENESIS 4.0",12,CYAN,true));
        root.addView(text(DateFormat.getTimeInstance(DateFormat.SHORT,Locale.getDefault()).format(new Date()),42,WHITE,true),lp(0,0));
        root.addView(text(DateFormat.getDateInstance(DateFormat.FULL,Locale.getDefault()).format(new Date()),14,MUTED,false),lp(0,18));
        LinearLayout status=card(); status.addView(text("ПАНЕЛЬ УПРАВЛЕНИЯ",11,CYAN,true));
        status.addView(text("Устройство готово · защищённые переключатели остаются под управлением Android",13,MUTED,false),lp(0,0)); root.addView(status,lp(0,16));
        root.addView(text("БЫСТРЫЕ ДЕЙСТВИЯ",11,CYAN,true),lp(0,16));
        addTile(root,"Wi-Fi","Подключения",Settings.ACTION_WIFI_SETTINGS);
        addTile(root,"Bluetooth","Наушники, часы и устройства",Settings.ACTION_BLUETOOTH_SETTINGS);
        addTile(root,"Мобильная сеть","SIM и сеть оператора",Settings.ACTION_NETWORK_OPERATOR_SETTINGS);
        addTile(root,"Экран и яркость","Яркость, размер и режим экрана",Settings.ACTION_DISPLAY_SETTINGS);
        addTile(root,"Звук","Громкость и вибрация",Settings.ACTION_SOUND_SETTINGS);
        addTile(root,"Не беспокоить","Режимы уведомлений",Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
        addTile(root,"Батарея","Энергопотребление",Settings.ACTION_BATTERY_SAVER_SETTINGS);
        addTile(root,"Геолокация","Разрешения и местоположение",Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        root.addView(text("УВЕДОМЛЕНИЯ",11,CYAN,true),lp(0,18));
        LinearLayout notifications=card(); notifications.addView(text("Центр уведомлений MANA",17,WHITE,true));
        notifications.addView(text("Здесь подготовлено место для будущего дизайна уведомлений. Реальные уведомления Realme пока остаются в штатной шторке Android.",13,MUTED,false),lp(0,8));
        notifications.addView(text("Открыть настройки уведомлений  ›",14,CYAN,true),lp(0,14));
        notifications.setOnClickListener(v->open(Settings.ACTION_APP_NOTIFICATION_SETTINGS)); root.addView(notifications,lp(0,8));
        TextView close=text("ЗАКРЫТЬ ПАНЕЛЬ",13,WHITE,true); close.setGravity(Gravity.CENTER); close.setPadding(0,dp(18),0,dp(18)); close.setOnClickListener(v->finish()); root.addView(close,lp(0,14));
        setContentView(scroll);
    }
    private void addTile(LinearLayout root,String title,String detail,String action) {
        LinearLayout tile=card(); tile.setBackground(bg(TILE,17)); tile.addView(text(title,16,WHITE,true)); tile.addView(text(detail,12,MUTED,false),lp(0,4));
        tile.setOnClickListener(v->open(action)); root.addView(tile,lp(0,8));
    }
    private void open(String action) { try{startActivity(new Intent(action));}catch(Exception e){startActivity(new Intent(Settings.ACTION_SETTINGS));} }
    private LinearLayout card(){LinearLayout b=new LinearLayout(this);b.setOrientation(1);b.setPadding(dp(16),dp(15),dp(16),dp(15));b.setBackground(bg(PANEL,20));return b;}
    private GradientDrawable bg(int c,int r){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(r));d.setStroke(dp(1),Color.argb(45,84,200,245));return d;}
    private TextView text(String s,int z,int c,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(c);t.setTypeface(Typeface.DEFAULT,bold?Typeface.BOLD:Typeface.NORMAL);return t;}
    private LinearLayout.LayoutParams lp(int top,int bottom){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(top);p.bottomMargin=dp(bottom);return p;}
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
}
