package com.today.ioskeyboard;

import android.content.Context;
import android.inputmethodservice.InputMethodService;
import android.os.Build;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.textservice.SentenceSuggestionsInfo;
import android.view.textservice.SpellCheckerSession;
import android.view.textservice.SuggestionsInfo;
import android.view.textservice.TextInfo;
import android.view.textservice.TextServicesManager;

import java.util.ArrayList;
import java.util.Locale;

public class IosKeyboardService extends InputMethodService
        implements SpellCheckerSession.SpellCheckerSessionListener {

    private KeyboardView keyboard;
    private SpellCheckerSession spellChecker;
    private boolean english = false;
    private boolean suggestionsAllowed = true;
    private int requestSequence = 0;
    private String requestedWord = "";

    @Override public void onCreate() {
        super.onCreate();
        openSpellChecker();
    }

    @Override public View onCreateInputView() {
        keyboard = new KeyboardView(this);
        keyboard.attach(this);
        return keyboard;
    }

    @Override public void onStartInput(EditorInfo info, boolean restarting) {
        super.onStartInput(info, restarting);
        suggestionsAllowed = isSafeTextField(info);
        requestSuggestions();
    }

    @Override public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        if (keyboard != null) keyboard.refresh();
        requestSuggestions();
    }

    @Override public void onFinishInput() {
        super.onFinishInput();
        requestedWord = "";
        requestSequence++;
        publishSuggestions(new String[0]);
    }

    @Override public void onDestroy() {
        if (spellChecker != null) {
            spellChecker.close();
            spellChecker = null;
        }
        super.onDestroy();
    }

    private boolean isSafeTextField(EditorInfo info) {
        if (info == null) return true;
        int inputType = info.inputType;
        int inputClass = inputType & android.text.InputType.TYPE_MASK_CLASS;
        int variation = inputType & android.text.InputType.TYPE_MASK_VARIATION;
        if (inputClass != android.text.InputType.TYPE_CLASS_TEXT) return false;
        if ((inputType & android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0) return false;
        return variation != android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                && variation != android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                && variation != android.text.InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
                && variation != android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                && variation != android.text.InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS
                && variation != android.text.InputType.TYPE_TEXT_VARIATION_URI;
    }

    private void openSpellChecker() {
        if (spellChecker != null) {
            spellChecker.close();
            spellChecker = null;
        }
        try {
            TextServicesManager manager = (TextServicesManager)
                    getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE);
            if (manager != null) {
                Locale locale = english ? Locale.US : new Locale("ru", "RU");
                spellChecker = manager.newSpellCheckerSession(null, locale, this, true);
            }
        } catch (Exception ignored) {
            spellChecker = null;
        }
        publishSuggestions(new String[0]);
    }

    void setKeyboardLanguage(boolean useEnglish) {
        if (english == useEnglish) return;
        english = useEnglish;
        openSpellChecker();
        requestSuggestions();
    }

    void type(String text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ic.commitText(text, 1);
            requestSuggestions();
        }
    }

    void deleteOne() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        if (Build.VERSION.SDK_INT >= 24) ic.deleteSurroundingTextInCodePoints(1, 0);
        else ic.deleteSurroundingText(1, 0);
        requestSuggestions();
    }

    void replaceCurrentWord(String replacement) {
        if (replacement == null || replacement.isEmpty()) return;
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        CharSequence before = ic.getTextBeforeCursor(96, 0);
        if (before == null || before.length() == 0) return;
        int start = before.length();
        while (start > 0 && Character.isLetterOrDigit(before.charAt(start - 1))) start--;
        if (start == before.length()) return;
        ic.beginBatchEdit();
        ic.deleteSurroundingText(before.length() - start, 0);
        ic.commitText(replacement, 1);
        ic.endBatchEdit();
        requestSuggestions();
    }

    void enter() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        int action = getCurrentInputEditorInfo() == null ? EditorInfo.IME_ACTION_NONE
                : (getCurrentInputEditorInfo().imeOptions & EditorInfo.IME_MASK_ACTION);
        if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
            if (!ic.performEditorAction(action)) ic.commitText("\n", 1);
        } else {
            ic.commitText("\n", 1);
        }
        publishSuggestions(new String[0]);
    }

    void requestSuggestions() {
        if (!suggestionsAllowed || spellChecker == null) {
            requestedWord = "";
            publishSuggestions(new String[0]);
            return;
        }
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) {
            publishSuggestions(new String[0]);
            return;
        }
        CharSequence before = ic.getTextBeforeCursor(96, 0);
        if (before == null || before.length() == 0) {
            publishSuggestions(new String[0]);
            return;
        }
        int start = before.length();
        while (start > 0 && Character.isLetterOrDigit(before.charAt(start - 1))) start--;
        String word = before.subSequence(start, before.length()).toString();
        if (word.length() < 2) {
            requestedWord = "";
            publishSuggestions(new String[0]);
            return;
        }
        requestedWord = word;
        int sequence = ++requestSequence;
        try {
            spellChecker.getSuggestions(new TextInfo[] { new TextInfo(word, 0, sequence) }, 5, false);
        } catch (Exception ignored) {
            publishSuggestions(new String[0]);
        }
    }

    private void publishSuggestions(String[] items) {
        if (keyboard != null) keyboard.post(() -> keyboard.setSuggestions(items));
    }

    @Override public void onGetSuggestions(SuggestionsInfo[] results) {
        if (results == null || results.length == 0) return;
        ArrayList<String> found = new ArrayList<>();
        for (SuggestionsInfo info : results) {
            if (info == null || info.getSequence() != requestSequence) continue;
            int count = info.getSuggestionsCount();
            for (int i = 0; i < count && found.size() < 3; i++) {
                String candidate = info.getSuggestionAt(i);
                if (candidate != null && !candidate.equalsIgnoreCase(requestedWord)
                        && !found.contains(candidate)) found.add(candidate);
            }
        }
        publishSuggestions(found.toArray(new String[0]));
    }

    @Override public void onGetSentenceSuggestions(SentenceSuggestionsInfo[] results) {
        // Word suggestions are requested explicitly for the word at the cursor.
    }
}
