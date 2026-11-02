package com.today.ioskeyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class KeyboardView extends View {
    private static class Key {
        String label, value, kind;
        float weight;
        RectF rect = new RectF();
        Key(String l,String v,String k,float w){label=l;value=v;kind=k;weight=w;}
    }
    private final Paint paint = new Paint(3);
    private final List<List<Key>> rows = new ArrayList<>();
    private IosKeyboardService service;
    private SharedPreferences prefs;
    private boolean english=false, upper=false, dark=false;
    private int mode=0;
    private Key pressed;
    private float downX,downY;

    public KeyboardView(Context c){super(c);prefs=PreferenceManager.getDefaultSharedPreferences(c);setLayerType(View.LAYER_TYPE_SOFTWARE,null);refresh();}
    void attach(IosKeyboardService s){service=s;}
    void refresh(){
        dark=prefs.getBoolean("dark",false);
        rebuild(); invalidate();
    }
    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
    private Key k(String label,String value,String kind,float weight){return new Key(label,value,kind,weight);}
    private List<Key> row(Key... keys){return new ArrayList<>(Arrays.asList(keys));}

    private void rebuild(){
        rows.clear();
        if(mode==3){
            rows.add(row(k("😀","😀","char",1),k("😁","😁","char",1),k("😂","😂","char",1),k("🤣","🤣","char",1),k("😊","😊","char",1),k("😍","😍","char",1),k("🥰","🥰","char",1),k("😎","😎","char",1),k("🤔","🤔","char",1),k("😭","😭","char",1)));
            rows.add(row(k("👍","👍","char",1),k("👎","👎","char",1),k("🙏","🙏","char",1),k("❤️","❤️","char",1),k("🔥","🔥","char",1),k("🎉","🎉","char",1),k("✨","✨","char",1),k("💯","💯","char",1),k("😘","😘","char",1),k("🥳","🥳","char",1)));
            rows.add(row(k("😅","😅","char",1),k("💀","💀","char",1),k("😴","😴","char",1),k("🤗","🤗","char",1),k("🫶","🫶","char",1),k("🤝","🤝","char",1),k("👀","👀","char",1),k("✌️","✌️","char",1),k("💖","💖","char",1)));
            rows.add(row(k("ABC","","letters",1.3f),k(", ",",","char",1),k("space"," ","space",4.2f),k(".",".","char",1),k("↵","","enter",1.3f)));
            return;
        }
        if(mode==0){
            if(english){
                rows.add(chars("qwertyuiop"));
                rows.add(chars("asdfghjkl"));
                rows.add(row(k("⇧","","shift",1.35f),letter("z"),letter("x"),letter("c"),letter("v"),letter("b"),letter("n"),letter("m"),k("⌫","","delete",1.35f)));
            }else{
                rows.add(chars("йцукенгшщзх"));
                rows.add(chars("фывапролджэ"));
                rows.add(row(k("⇧","","shift",1.35f),letter("я"),letter("ч"),letter("с"),letter("м"),letter("и"),letter("т"),letter("ь"),letter("б"),letter("ю"),k("⌫","","delete",1.35f)));
            }
            rows.add(row(k("123","","numbers",1.35f),k("😊","","emoji",1),k(", ",",","char",0.9f),k("space"," ","space",4.3f),k(".",".","char",0.9f),k("🌐","","language",1),k("↵","","enter",1.35f)));
        }else if(mode==1){
            rows.add(chars("1234567890"));
            rows.add(chars("@#$%&-+()/"));
            rows.add(row(k("=","","symbols",1.3f),letter("*"),letter("\""),letter("'"),letter(":"),letter(";"),letter("!"),letter("?"),k("⌫","","delete",1.3f)));
            rows.add(row(k("ABC","","letters",1.35f),k("😊","","emoji",1),k(", ",",","char",0.9f),k("space"," ","space",4.3f),k(".",".","char",0.9f),k("🌐","","language",1),k("↵","","enter",1.35f)));
        }else{
            rows.add(chars("[]{}#%^*+="));
            rows.add(chars("_\\|~<>€£¥"));
            rows.add(row(k("123","","numbers",1.3f),letter("•"),letter("…"),letter("—"),letter("¿"),letter("¡"),letter("©"),letter("®"),k("⌫","","delete",1.3f)));
            rows.add(row(k("ABC","","letters",1.35f),k("😊","","emoji",1),k(", ",",","char",0.9f),k("space"," ","space",4.3f),k(".",".","char",0.9f),k("🌐","","language",1),k("↵","","enter",1.35f)));
        }
    }
    private List<Key> chars(String s){
        List<Key> r=new ArrayList<>();
        for(int i=0;i<s.length();i++){String v=s.substring(i,i+1);r.add(letter(v));}
        return r;
    }
    private Key letter(String s){String v=upper?s.toUpperCase(english?Locale.US:new Locale("ru")):s;return k(v,v,"char",1);}

    @Override protected void onMeasure(int w,int h){int width=MeasureSpec.getSize(w);setMeasuredDimension(width,dp(278));}

    @Override protected void onDraw(Canvas c){
        float w=getWidth(),d=getResources().getDisplayMetrics().density;
        int board=dark?0xFF242831:0xFFD0D4DC, key=dark?0xFF454B57:0xFFFFFFFF, special=dark?0xFF353B46:0xFFAEB5C0;
        int ink=dark?0xFFF7F8FA:0xFF20232A, secondary=dark?0xFFCBD0D8:0xFF5B606A;
        c.drawColor(board);
        float pad=dp(4), gap=dp(5), header=dp(38), rowH=dp(43), y=header;
        paint.setColor(dark?0xFF242831:0xFFD0D4DC); c.drawRect(0,0,w,header,paint);
        String[] suggestions=english?new String[]{"Hello","Thanks","Yes"}:new String[]{"Привет","Спасибо","Да"};
        paint.setTextAlign(Paint.Align.CENTER);paint.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));paint.setTextSize(dp(14));paint.setColor(secondary);
        for(int i=0;i<3;i++){float x=w*(i+0.5f)/3f;c.drawText(mode==0?suggestions[i]:"",x,dp(24),paint);if(i>0){paint.setColor(dark?0xFF4D535E:0xFFB5BAC3);c.drawRect(w*i/3f,dp(8),w*i/3f+1,dp(29),paint);paint.setColor(secondary);}}
        float totalH=rows.size()*rowH+(rows.size()-1)*gap;
        float available=Math.max(dp(180),getHeight()-header-dp(7));
        if(totalH>available) rowH=(available-gap*(rows.size()-1))/rows.size();
        for(List<Key> row:rows){
            float weights=0;for(Key a:row)weights+=a.weight;
            float full=w-2*pad-gap*(row.size()-1),x=pad;
            for(Key a:row){
                float kw=full*a.weight/weights;
                a.rect.set(x,y,x+kw,y+rowH);
                boolean isSpecial=!a.kind.equals("char")&&!a.kind.equals("space");
                int fill=isSpecial?special:key;
                paint.setColor(dark?0x33000000:0x22000000);c.drawRoundRect(new RectF(x,y+dp(1),x+kw,y+rowH+dp(1)),dp(5),dp(5),paint);
                paint.setColor(fill);c.drawRoundRect(a.rect,dp(5),dp(5),paint);
                paint.setColor(a.kind.equals("space")?secondary:ink);
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));
                float size=a.kind.equals("char")?dp(22):dp(15);
                if(a.label.length()>3)size=dp(13);
                paint.setTextSize(size);
                String label=a.kind.equals("space")?(english?"English":"Русский"):a.label;
                c.drawText(label,x+kw/2f,y+rowH/2f-(paint.ascent()+paint.descent())/2f,paint);
                x+=kw+gap;
            }
            y+=rowH+gap;
        }
    }

    @Override public boolean onTouchEvent(MotionEvent e){
        float x=e.getX(),y=e.getY();
        if(e.getAction()==MotionEvent.ACTION_DOWN){downX=x;downY=y;pressed=find(x,y);return true;}
        if(e.getAction()==MotionEvent.ACTION_UP){
            Key hit=find(x,y);
            if(pressed!=null&&pressed==hit) perform(pressed);
            pressed=null;return true;
        }
        if(e.getAction()==MotionEvent.ACTION_CANCEL){pressed=null;return true;}
        return true;
    }
    private Key find(float x,float y){
        for(List<Key> row:rows)for(Key a:row)if(a.rect.contains(x,y))return a;
        return null;
    }
    private void perform(Key a){
        if(prefs.getBoolean("haptic",true))performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
        if(prefs.getBoolean("sound",false)){
            android.media.AudioManager am=(android.media.AudioManager)getContext().getSystemService(Context.AUDIO_SERVICE);
            if(am!=null)am.playSoundEffect(android.media.AudioManager.FX_KEYPRESS_STANDARD);
        }
        switch(a.kind){
            case "char": if(service!=null)service.type(a.value); if(upper){upper=false;rebuild();} break;
            case "space": if(service!=null)service.type(" "); break;
            case "delete": if(service!=null)service.deleteOne(); break;
            case "enter": if(service!=null)service.enter(); break;
            case "shift": upper=!upper; rebuild(); break;
            case "numbers": mode=1;rebuild();break;
            case "symbols": mode=2;rebuild();break;
            case "letters": mode=0;rebuild();break;
            case "emoji": mode=3;rebuild();break;
            case "language": english=!english;upper=false;mode=0;rebuild();break;
        }
        invalidate();
    }
}
