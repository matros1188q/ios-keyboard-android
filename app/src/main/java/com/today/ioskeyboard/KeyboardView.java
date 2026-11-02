package com.today.ioskeyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
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
        Key(String label, String value, String kind, float weight) {
            this.label = label;
            this.value = value;
            this.kind = kind;
            this.weight = weight;
        }
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final List<List<Key>> rows = new ArrayList<>();
    private final String[] suggestions = new String[3];
    private IosKeyboardService service;
    private SharedPreferences prefs;
    private boolean english = false;
    private boolean upper = false;
    private boolean dark = false;
    private int mode = 0;
    private Key pressed;
    private int pressedSuggestion = -1;
    private final RectF scratch = new RectF();

    public KeyboardView(Context context) {
        super(context);
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        refresh();
    }

    void attach(IosKeyboardService service) {
        this.service = service;
    }

    void refresh() {
        dark = prefs.getBoolean("dark", false);
        rebuild();
        invalidate();
    }

    void setSuggestions(String[] items) {
        Arrays.fill(suggestions, null);
        if (items != null) {
            int count = Math.min(3, items.length);
            for (int i = 0; i < count; i++) suggestions[i] = items[i];
        }
        invalidate();
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private Key key(String label, String value, String kind, float weight) {
        return new Key(label, value, kind, weight);
    }

    private List<Key> row(Key... keys) {
        return new ArrayList<>(Arrays.asList(keys));
    }

    private void rebuild() {
        rows.clear();

        if (mode == 3) {
            rows.add(row(
                    key("😀", "😀", "char", 1), key("😁", "😁", "char", 1),
                    key("😂", "😂", "char", 1), key("🤣", "🤣", "char", 1),
                    key("😊", "😊", "char", 1), key("😍", "😍", "char", 1),
                    key("🥰", "🥰", "char", 1), key("😎", "😎", "char", 1),
                    key("🤔", "🤔", "char", 1), key("😭", "😭", "char", 1)));
            rows.add(row(
                    key("👍", "👍", "char", 1), key("👎", "👎", "char", 1),
                    key("🙏", "🙏", "char", 1), key("❤️", "❤️", "char", 1),
                    key("🔥", "🔥", "char", 1), key("🎉", "🎉", "char", 1),
                    key("✨", "✨", "char", 1), key("💯", "💯", "char", 1),
                    key("😘", "😘", "char", 1), key("🥳", "🥳", "char", 1)));
            rows.add(row(
                    key("😅", "😅", "char", 1), key("💀", "💀", "char", 1),
                    key("😴", "😴", "char", 1), key("🤗", "🤗", "char", 1),
                    key("🫶", "🫶", "char", 1), key("🤝", "🤝", "char", 1),
                    key("👀", "👀", "char", 1), key("✌️", "✌️", "char", 1),
                    key("💖", "💖", "char", 1)));
            rows.add(row(
                    key("ABC", "", "letters", 1.25f),
                    key("", "", "language", 1),
                    key(",", ",", "char", 0.85f),
                    key("space", " ", "space", 4.5f),
                    key(".", ".", "char", 0.85f),
                    key("↵", "", "enter", 1.25f)));
            return;
        }

        if (mode == 0) {
            if (english) {
                rows.add(chars("qwertyuiop"));
                rows.add(chars("asdfghjkl"));
                rows.add(row(
                        key("⇧", "", "shift", 1.35f),
                        letter("z"), letter("x"), letter("c"), letter("v"),
                        letter("b"), letter("n"), letter("m"),
                        key("⌫", "", "delete", 1.35f)));
            } else {
                rows.add(chars("йцукенгшщзх"));
                rows.add(chars("фывапролджэ"));
                rows.add(row(
                        key("⇧", "", "shift", 1.25f),
                        letter("я"), letter("ч"), letter("с"), letter("м"),
                        letter("и"), letter("т"), letter("ь"), letter("б"), letter("ю"),
                        key("⌫", "", "delete", 1.25f)));
            }
            rows.add(bottomRow());
        } else if (mode == 1) {
            rows.add(chars("1234567890"));
            rows.add(chars("@#$%&-+()/"));
            rows.add(row(
                    key("=\\<", "", "symbols", 1.25f), letter("*"), letter("\\"),
                    letter("'"), letter(":"), letter(";"), letter("!"), letter("?"),
                    key("⌫", "", "delete", 1.25f)));
            rows.add(bottomRow());
        } else {
            rows.add(chars("[]{}#%^*+="));
            rows.add(chars("_\\|~<>€£¥"));
            rows.add(row(
                    key("123", "", "numbers", 1.25f),
                    letter("•"), letter("…"), letter("—"), letter("¿"), letter("¡"),
                    letter("©"), letter("®"), key("⌫", "", "delete", 1.25f)));
            rows.add(bottomRow());
        }
    }

    private List<Key> bottomRow() {
        return row(
                key(mode == 0 ? "123" : "ABC", "", mode == 0 ? "numbers" : "letters", 1.25f),
                key("", "", "language", 0.95f),
                key("", "", "emoji", 0.95f),
                key(",", ",", "char", 0.78f),
                key("space", " ", "space", 4.55f),
                key(".", ".", "char", 0.78f),
                key("ввод", "", "enter", 1.25f));
    }

    private List<Key> chars(String value) {
        List<Key> result = new ArrayList<>();
        for (int i = 0; i < value.length(); i++) {
            result.add(letter(value.substring(i, i + 1)));
        }
        return result;
    }

    private Key letter(String value) {
        String shown = upper ? value.toUpperCase(english ? Locale.US : new Locale("ru")) : value;
        return key(shown, shown, "char", 1);
    }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), dp(276));
    }

    @Override protected void onDraw(Canvas canvas) {
        float width = getWidth();
        int board = dark ? 0xFF242831 : 0xFFD1D5DC;
        int face = dark ? 0xFF454B57 : 0xFFFFFFFF;
        int function = dark ? 0xFF373D47 : 0xFFADB4BF;
        int ink = dark ? 0xFFF7F8FA : 0xFF20232A;
        int muted = dark ? 0xFFD0D4DC : 0xFF444A54;

        canvas.drawColor(board);
        drawSuggestionBar(canvas, width, board, muted);

        float horizontalPad = dp(4);
        float horizontalGap = dp(5);
        float verticalGap = dp(7);
        float headerHeight = dp(42);
        float bottomSpace = dp(19);
        float rowHeight = dp(44);
        float totalRowsHeight = rows.size() * rowHeight + (rows.size() - 1) * verticalGap;
        float available = getHeight() - headerHeight - bottomSpace;
        if (totalRowsHeight > available) {
            rowHeight = (available - (rows.size() - 1) * verticalGap) / rows.size();
        }

        float y = headerHeight;
        for (List<Key> row : rows) {
            float weightSum = 0;
            for (Key item : row) weightSum += item.weight;
            float usableWidth = width - 2 * horizontalPad - horizontalGap * (row.size() - 1);
            float x = horizontalPad;

            for (Key item : row) {
                float keyWidth = usableWidth * item.weight / weightSum;
                item.rect.set(x, y, x + keyWidth, y + rowHeight);
                boolean isFunction = !item.kind.equals("char") && !item.kind.equals("space");
                int color = isFunction ? function : face;
                drawKeyFace(canvas, item.rect, color, board);

                if (item.kind.equals("emoji")) {
                    drawSmile(canvas, item.rect.centerX(), item.rect.centerY(), ink);
                } else if (item.kind.equals("language")) {
                    drawGlobe(canvas, item.rect.centerX(), item.rect.centerY(), ink);
                } else {
                    drawKeyLabel(canvas, item, x, y, keyWidth, rowHeight, ink, muted);
                }
                x += keyWidth + horizontalGap;
            }
            y += rowHeight + verticalGap;
        }
    }

    private void drawSuggestionBar(Canvas canvas, float width, int board, int textColor) {
        float header = dp(42);
        paint.clearShadowLayer();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(board);
        canvas.drawRect(0, 0, width, header, paint);

        paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        paint.setTextSize(dp(14.5f));
        paint.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < 3; i++) {
            float x = width * (i + 0.5f) / 3f;
            paint.setColor(textColor);
            String value = suggestions[i] == null ? "" : suggestions[i];
            if (!value.isEmpty()) {
                canvas.drawText(value, x, dp(26) - (paint.ascent() + paint.descent()) / 2f - dp(6), paint);
            }
            if (i > 0) {
                paint.setColor(dark ? 0xFF4A505B : 0xFFB6BBC4);
                canvas.drawRect(width * i / 3f, dp(9), width * i / 3f + dp(1), dp(33), paint);
            }
        }
    }

    private void drawKeyFace(Canvas canvas, RectF rect, int color, int board) {
        scratch.set(rect.left, rect.top + dp(1), rect.right, rect.bottom + dp(2));
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        paint.setShadowLayer(dp(1.2f), 0, dp(1), dark ? 0x66000000 : 0x42000000);
        canvas.drawRoundRect(scratch, dp(5.5f), dp(5.5f), paint);
        paint.clearShadowLayer();
        paint.setColor(color);
        canvas.drawRoundRect(rect, dp(5.5f), dp(5.5f), paint);
    }

    private void drawKeyLabel(Canvas canvas, Key item, float x, float y,
                              float width, float height, int ink, int muted) {
        paint.clearShadowLayer();
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));

        String label = item.label;
        float size = dp(22);
        int color = ink;

        if (item.kind.equals("space")) {
            label = english ? "English (US)" : "Русский";
            size = dp(11.5f);
            color = muted;
        } else if (item.kind.equals("enter")) {
            label = "ввод";
            size = dp(13);
        } else if (item.kind.equals("shift") || item.kind.equals("delete")) {
            size = dp(22);
        } else if (item.label.length() > 3) {
            size = dp(12.5f);
        } else if (item.label.equals("123") || item.label.equals("ABC")) {
            size = dp(14);
        } else if (item.label.equals("⇧") || item.label.equals("⌫")) {
            size = dp(21);
        }

        paint.setTextSize(size);
        paint.setColor(color);
        float baseline = y + height / 2f - (paint.ascent() + paint.descent()) / 2f;
        canvas.drawText(label, x + width / 2f, baseline, paint);
    }

    private void drawSmile(Canvas canvas, float cx, float cy, int color) {
        paint.clearShadowLayer();
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1.7f));
        float radius = dp(7.5f);
        canvas.drawCircle(cx, cy, radius, paint);
        paint.setStyle(Paint.Style.FILL);
        float eye = dp(0.85f);
        canvas.drawCircle(cx - dp(2.6f), cy - dp(2), eye, paint);
        canvas.drawCircle(cx + dp(2.6f), cy - dp(2), eye, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1.35f));
        scratch.set(cx - dp(3.2f), cy - dp(0.3f), cx + dp(3.2f), cy + dp(4.2f));
        canvas.drawArc(scratch, 15, 150, false, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawGlobe(Canvas canvas, float cx, float cy, int color) {
        paint.clearShadowLayer();
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1.45f));
        float radius = dp(7.5f);
        canvas.drawCircle(cx, cy, radius, paint);
        scratch.set(cx - dp(3.2f), cy - radius, cx + dp(3.2f), cy + radius);
        canvas.drawOval(scratch, paint);
        scratch.set(cx - radius, cy - dp(3.2f), cx + radius, cy + dp(3.2f));
        canvas.drawOval(scratch, paint);
        canvas.drawLine(cx - radius, cy, cx + radius, cy, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            pressed = null;
            pressedSuggestion = -1;
            if (y < dp(42)) pressedSuggestion = suggestionAt(x);
            else pressed = findKey(x, y);
            return true;
        }

        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (pressedSuggestion >= 0 && pressedSuggestion == suggestionAt(x)
                    && suggestions[pressedSuggestion] != null && service != null) {
                service.replaceCurrentWord(suggestions[pressedSuggestion]);
            } else {
                Key hit = findKey(x, y);
                if (pressed != null && pressed == hit) perform(pressed);
            }
            pressed = null;
            pressedSuggestion = -1;
            return true;
        }

        if (event.getAction() == MotionEvent.ACTION_CANCEL) {
            pressed = null;
            pressedSuggestion = -1;
            return true;
        }
        return true;
    }

    private int suggestionAt(float x) {
        if (x < 0 || x >= getWidth()) return -1;
        int index = Math.min(2, (int) (x / (getWidth() / 3f)));
        return suggestions[index] == null ? -1 : index;
    }

    private Key findKey(float x, float y) {
        for (List<Key> row : rows) {
            for (Key item : row) {
                if (item.rect.contains(x, y)) return item;
            }
        }
        return null;
    }

    private void perform(Key item) {
        if (prefs.getBoolean("haptic", true)) {
            performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
        }
        if (prefs.getBoolean("sound", false)) {
            android.media.AudioManager audio = (android.media.AudioManager)
                    getContext().getSystemService(Context.AUDIO_SERVICE);
            if (audio != null) audio.playSoundEffect(android.media.AudioManager.FX_KEYPRESS_STANDARD);
        }

        switch (item.kind) {
            case "char":
                if (service != null) service.type(item.value);
                if (upper) {
                    upper = false;
                    rebuild();
                }
                break;
            case "space":
                if (service != null) service.type(" ");
                break;
            case "delete":
                if (service != null) service.deleteOne();
                break;
            case "enter":
                if (service != null) service.enter();
                break;
            case "shift":
                upper = !upper;
                rebuild();
                break;
            case "numbers":
                mode = 1;
                rebuild();
                break;
            case "symbols":
                mode = 2;
                rebuild();
                break;
            case "letters":
                mode = 0;
                rebuild();
                break;
            case "emoji":
                mode = 3;
                rebuild();
                break;
            case "language":
                english = !english;
                upper = false;
                mode = 0;
                rebuild();
                if (service != null) service.setKeyboardLanguage(english);
                break;
        }
        invalidate();
    }
}
