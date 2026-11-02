package com.today.ioskeyboard;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

public class MainActivity extends Activity {
    private SharedPreferences prefs;
    private LinearLayout page;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        render();
    }

    private void render() {
        boolean dark = prefs.getBoolean("dark", false);
        int bg = dark ? Color.rgb(22,24,29) : Color.rgb(242,243,247);
        int fg = dark ? Color.WHITE : Color.rgb(26,28,34);
        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(22), dp(26), dp(22), dp(24));
        page.setBackgroundColor(bg);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(page);
        setContentView(scroll);

        TextView title = text("iOS Keyboard", 30, fg, true);
        page.addView(title);
        TextView sub = text("Клавиатура в стиле iPhone", 16, dark ? 0xFFB8BBC4 : 0xFF70747E, false);
        LinearLayout.LayoutParams sp = params(-1, -2); sp.topMargin = dp(5);
        page.addView(sub, sp);

        LinearLayout preview = new LinearLayout(this);
        preview.setOrientation(LinearLayout.VERTICAL);
        preview.setPadding(dp(18),dp(18),dp(18),dp(18));
        preview.setBackground(round(dark ? 0xFF30343D : Color.WHITE, 22));
        LinearLayout.LayoutParams pp = params(-1,-2); pp.topMargin=dp(24);
        page.addView(preview,pp);
        preview.addView(text("Добро пожаловать",19,fg,true));
        TextView info=text("Включи клавиатуру в настройках Android, затем выбери её в переключателе ввода.",14,
                dark ? 0xFFD0D2D9 : 0xFF666B75,false);
        LinearLayout.LayoutParams ip=params(-1,-2); ip.topMargin=dp(8); preview.addView(info,ip);

        Button enable=button("1. Включить iOS Keyboard");
        LinearLayout.LayoutParams bp=params(-1,dp(54)); bp.topMargin=dp(20); page.addView(enable,bp);
        enable.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)));

        Button choose=button("2. Выбрать клавиатуру");
        LinearLayout.LayoutParams cp=params(-1,dp(54)); cp.topMargin=dp(10); page.addView(choose,cp);
        choose.setOnClickListener(v->{
            InputMethodManager imm=(InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            if(imm!=null) imm.showInputMethodPicker();
        });

        page.addView(section("Персонализация",fg),params(-1,-2));
        addSwitch("Тёмная тема","dark",dark,bg,fg);
        addSwitch("Виброотклик клавиш","haptic",prefs.getBoolean("haptic",true),bg,fg);
        addSwitch("Звук клавиш","sound",prefs.getBoolean("sound",false),bg,fg);

        TextView privacy=text("Работает локально. Приложение не запрашивает доступ к интернету и не отправляет вводимый текст.",12,
                dark ? 0xFF9EA3AE : 0xFF858A94,false);
        LinearLayout.LayoutParams lp=params(-1,-2); lp.topMargin=dp(26); page.addView(privacy,lp);
    }

    private void addSwitch(String label,String key,boolean checked,int bg,int fg) {
        LinearLayout row=new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(15),dp(12),dp(12),dp(12));
        row.setBackground(round(prefs.getBoolean("dark",false)?0xFF30343D:Color.WHITE,16));
        LinearLayout.LayoutParams rp=params(-1,-2); rp.topMargin=dp(9); page.addView(row,rp);
        TextView t=text(label,15,fg,false); row.addView(t,new LinearLayout.LayoutParams(0,-2,1));
        Switch s=new Switch(this); s.setChecked(checked); row.addView(s);
        s.setOnCheckedChangeListener((b,value)->{
            prefs.edit().putBoolean(key,value).apply();
            if(key.equals("dark")) render();
        });
    }

    private TextView section(String value,int color) {
        TextView t=text(value,18,color,true);
        LinearLayout.LayoutParams p=params(-1,-2); p.topMargin=dp(26); p.bottomMargin=dp(4);
        t.setLayoutParams(p); return t;
    }
    private TextView text(String value,int size,int color,boolean bold) {
        TextView t=new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return t;
    }
    private Button button(String value) {
        Button b=new Button(this); b.setText(value); b.setTextSize(15); b.setAllCaps(false);
        b.setTextColor(Color.WHITE); b.setBackground(round(0xFF087CF0,16)); return b;
    }
    private GradientDrawable round(int color,int radius) {
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    private LinearLayout.LayoutParams params(int w,int h){return new LinearLayout.LayoutParams(w,h);}
    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
}
