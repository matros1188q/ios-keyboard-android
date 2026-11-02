package com.today.ioskeyboard;

import android.inputmethodservice.InputMethodService;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputConnection;
import android.content.Context;
import android.view.inputmethod.EditorInfo;

public class IosKeyboardService extends InputMethodService {
    private KeyboardView keyboard;

    @Override public View onCreateInputView() {
        keyboard = new KeyboardView(this);
        keyboard.attach(this);
        return keyboard;
    }

    @Override public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        if (keyboard != null) keyboard.refresh();
    }

    void type(String text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) ic.commitText(text, 1);
    }

    void deleteOne() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        ic.deleteSurroundingTextInCodePoints(1, 0);
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
    }

    void showLanguagePicker() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) switchToNextInputMethod(false);
    }
}
