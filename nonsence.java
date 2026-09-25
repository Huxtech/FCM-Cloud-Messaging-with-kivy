
// STEP 1: Change 'extends View' to 'extends android.widget.EditText'
class DummyEdit extends android.widget.EditText implements View.OnKeyListener {
    InputConnection ic;

    public DummyEdit(Context context) {
        super(context);
        setFocusableInTouchMode(true);
        setFocusable(true);
        setOnKeyListener(this);
        
        // Hide the cursor and background lines so it doesn't draw on top of your game canvas
        this.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        this.setTextColor(android.graphics.Color.TRANSPARENT);
        this.setCursorVisible(false);
    }

    @Override
    public boolean onCheckIsTextEditor() {
        return true;
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        return SDLActivity.handleKeyEvent(v, keyCode, event, ic);
    }

    @Override
    public boolean onKeyPreIme (int keyCode, KeyEvent event) {
        if (event.getAction()==KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
            if (SDLActivity.mTextEdit != null && SDLActivity.mTextEdit.getVisibility() == View.VISIBLE) {
                SDLActivity.onNativeKeyboardFocusLost();
            }
        }
        return super.onKeyPreIme(keyCode, event);
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        outAttrs.inputType = SDLActivity.keyboardInputType;
        outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI | EditorInfo.IME_FLAG_NO_FULLSCREEN;
        outAttrs.imeOptions &= ~EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING;

        // FIXED: Link your customized connection logic here so JNI calls are fired
        ic = new SDLInputConnection(this, true);
        return ic;
    }
}


class SDLInputConnection extends BaseInputConnection {

    protected EditText mEditText;
    protected String mCommittedText = "";

    public SDLInputConnection(View targetView, boolean fullEditor) {
        super(targetView, fullEditor);
        // FIXED: Assign mEditText directly to targetView (which is your DummyEdit extending EditText)
        if (targetView instanceof EditText) {
            mEditText = (EditText) targetView;
        } else {
            mEditText = new EditText(SDL.getContext());
        }
    }

    @Override
    public Editable getEditable() {
        // FIXED: Point directly to the active layout text buffer to preserve SwiftKey suggestions
        if (mEditText != null) {
            return mEditText.getText();
        }
        return mEditText.getEditableText();
    }

    @Override
    public boolean sendKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
            if (SDLActivity.onNativeSoftReturnKey()) {
                return true;
            }
        }
        return super.sendKeyEvent(event);
    }

    @Override
    public boolean commitText(CharSequence text, int newCursorPosition) {
        if (!super.commitText(text, newCursorPosition)) {
            return false;
        }
        updateText();
        return true;
    }

    @Override
    public boolean setComposingText(CharSequence text, int newCursorPosition) {
        if (!super.setComposingText(text, newCursorPosition)) {
            return false;
        }
        updateText();
        return true;
    }

    @Override
    public boolean deleteSurroundingText(int beforeLength, int afterLength) {
        if (Build.VERSION.SDK_INT <= 29 /* Android 10.0 (Q) */) {
            if (beforeLength > 0 && afterLength == 0) {
                while (beforeLength-- > 0) {
                    nativeGenerateScancodeForUnichar('\b');
                }
                return true;
           }
        }

        if (!super.deleteSurroundingText(beforeLength, afterLength)) {
            return false;
        }
        updateText();
        return true;
    }

    protected void updateText() {
        final Editable content = getEditable();
        if (content == null) {
            return;
        }

        String text = content.toString();
        int compareLength = Math.min(text.length(), mCommittedText.length());
        int matchLength, offset;

        /* Backspace over characters that are no longer in the string */
        for (matchLength = 0; matchLength < compareLength; ) {
            int codePoint = mCommittedText.codePointAt(matchLength);
            if (codePoint != text.codePointAt(matchLength)) {
                break;
            }
            matchLength += Character.charCount(codePoint);
        }
        /* FIXME: This doesn't handle graphemes, like '🌬️' */
        for (offset = matchLength; offset < mCommittedText.length(); ) {
            int codePoint = mCommittedText.codePointAt(offset);
            nativeGenerateScancodeForUnichar('\b');
            offset += Character.charCount(codePoint);
        }

        if (matchLength < text.length()) {
            String pendingText = text.subSequence(matchLength, text.length()).toString();
            for (offset = 0; offset < pendingText.length(); ) {
                int codePoint = pendingText.codePointAt(offset);
                if (codePoint == '\n') {
                    if (SDLActivity.onNativeSoftReturnKey()) {
                        return;
                    }
                }
                /* Higher code points don't generate simulated scancodes */
                if (codePoint < 128) {
                    nativeGenerateScancodeForUnichar((char)codePoint);
                }
                offset += Character.charCount(codePoint);
            }
            SDLInputConnection.nativeCommitText(pendingText, 0);
        }
        mCommittedText = text;
    }

    public static native void nativeCommitText(String text, int newCursorPosition);

    public static native void nativeGenerateScancodeForUnichar(char c);
}
