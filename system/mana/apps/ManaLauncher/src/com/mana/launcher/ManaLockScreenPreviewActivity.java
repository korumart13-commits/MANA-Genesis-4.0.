package com.mana.launcher;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

/** Visual lock-screen preview only; does not replace or bypass Android Keyguard. */
public final class ManaLockScreenPreviewActivity extends Activity {
    private static final int BG=Color.rgb(5,15,31), PANEL=Color.rgb(16,43,80), CYAN=Color.rgb(84,200,245), WHITE=Color.rgb(234,244,255), MUTED=Color.rgb(145,169,197);
    @Override public void onCreate(Bundle state) {
        super.onCreate(state); getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        LinearLayout root=new LinearLayout(this); root.setOrientation(1); root.setGravity(Gravity.CENTER_HORIZONTAL); root.setPadding(dp(26),dp(34),dp(26),dp(28));
        GradientDrawable wallpaper=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{Color.rgb(9,36,68),BG,Color.rgb(4,10,23)}); root.setBackground(wallpaper);
        root.addView(text("MANA  •  GENESIS",12,CYAN,true));
        TextView clock=text(DateFormat.getTimeInstance(DateFormat.SHORT,Locale.getDefault()).format(new Date()),64,WHITE,false); clock.setPadding(0,dp(74),0,dp(4)); root.addView(clock);
        root.addView(text(DateFormat.getDateInstance(DateFormat.FULL,Locale.getDefault()).format(new Date()),14,MUTED,false));
        TextView spacer=new TextView(this); root.addView(spacer,new LinearLayout.LayoutParams(1,0,1f));
        LinearLayout info=card(); info.addView(text("ТВОЁ ПРОСТРАНСТВО. ТВОЯ MANA.",13,CYAN,true));
        info.addView(text("Погода, уведомления и быстрые действия появятся здесь после подключения соответствующих системных API.",13,WHITE,false),margin(0,8)); root.addView(info,margin(0,18));
        TextView hint=text("ПРЕДПРОСМОТР ДИЗАЙНА · НЕ АКТИВНЫЙ KEYGUARD",10,MUTED,true); hint.setGravity(Gravity.CENTER); hint.setPadding(0,dp(18),0,dp(16)); root.addView(hint);
        TextView close=text("Вернуться в MANA",15,WHITE,true); close.setGravity(Gravity.CENTER); close.setPadding(dp(18),dp(14),dp(18),dp(14)); close.setBackground(panel()); close.setOnClickListener(v->finish()); root.addView(close,margin(0,0));
        setContentView(root);
    }
    private LinearLayout card(){LinearLayout b=new LinearLayout(this);b.setOrientation(1);b.setPadding(dp(18),dp(17),dp(18),dp(17));b.setBackground(panel());return b;}
    private GradientDrawable panel(){GradientDrawable d=new GradientDrawable();d.setColor(PANEL);d.setCornerRadius(dp(22));d.setStroke(dp(1),Color.argb(60,84,200,245));return d;}
    private TextView text(String s,int z,int c,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(c);t.setTypeface(Typeface.DEFAULT,bold?Typeface.BOLD:Typeface.NORMAL);return t;}
    private LinearLayout.LayoutParams margin(int top,int bottom){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(top);p.bottomMargin=dp(bottom);return p;}
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
}
