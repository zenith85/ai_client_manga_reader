package com.example.aireader;

import android.widget.TextView;

public interface AICallback {
    void showTextDialog(String title, String text);
    void onAIResponse(String response);
    void onError(String error);
}
