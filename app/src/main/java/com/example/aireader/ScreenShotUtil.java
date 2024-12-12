package com.example.aireader;
// ScreenshotUtil.java
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;

public class ScreenShotUtil {
    private static String TAG = "ScreenShotUtil";
    public static Bitmap takeScreenshot(Activity activity) {
        View rootView = activity.getWindow().getDecorView().getRootView();
        rootView.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(rootView.getDrawingCache());
        rootView.setDrawingCacheEnabled(false);
        return bitmap;
    }

    public static Bitmap cropScreenshot(Bitmap screenshot, Rect rect, View DrawingView) {
        // Ensure the rectangle is within bounds
        if (screenshot == null) {
            Log.d(TAG, "Screenshot is null, cannot crop.");
            return null;
        }
        // Get the location of the DrawingView on the screen
        int[] location = new int[2];
        DrawingView.getLocationOnScreen(location);

        // Calculate the absolute position of the rectangle on the screen
        int left = Math.max(rect.left + location[0], 0);
        int top = Math.max(rect.top + location[1], 0);
        int right = Math.min(left + rect.width(), screenshot.getWidth());
        int bottom = Math.min(top + rect.height(), screenshot.getHeight());

        // Ensure the coordinates are valid
        if (left >= right || top >= bottom) {
            Log.d(TAG, "Invalid crop area dimensions.");
            return null;
        }

        return Bitmap.createBitmap(screenshot, left, top, right - left, bottom - top);
    }
}
