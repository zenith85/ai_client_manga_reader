package com.example.aireader;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Environment;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import android.content.ClipboardManager;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

public class DrawingView extends View {
    private static String TAG = "DrawingView ";
    public static boolean select_mode;
    private Paint paint;
    public String chosen_lang;
    private Rect currentRect;
    private List<Rect> rectangles = new ArrayList<>();
    private List<Bitmap> snippets = new ArrayList<>();
    private Bitmap imageBitmap; // The bitmap of the image from which to capture snippets
    private Activity activity; // Declare Activity variable

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        //paint.setStrokeWidth(5);
        if (context instanceof Activity) {
            this.activity = (Activity) context; // Cast context to Activity
        }
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        //paint.setColor(0xFFFF0000); // Red color for the rectangle
    }

    public void setImageBitmap(Bitmap bitmap) {
        this.imageBitmap = bitmap; // Set the image bitmap to be used for cropping
        invalidate(); // Redraw the view
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw all rectangles and their corresponding snippets
        for (int i = 0; i < rectangles.size(); i++) {
            Rect rect = rectangles.get(i);
            canvas.drawRect(rect, paint); // Draw the rectangle

            // If there is a snippet corresponding to this rectangle, draw it
            if (snippets.size() > i && snippets.get(i) != null) {
                Bitmap snippet = snippets.get(i);
                // Ensure the snippet is scaled to fit the rectangle
                canvas.drawBitmap(snippet, null, rect, null); // Draw the snippet
            }
        }

        // Draw the current rectangle being drawn
        if (currentRect != null) {
            canvas.drawRect(currentRect, paint);
        }
    }

    int initialX, initialY;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!select_mode){return false;}

        int x = (int) event.getX();
        int y = (int) event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                initialX = x;
                initialY = y;
                currentRect = new Rect(x, y, x, y); // Start new rectangle
                //MainActivity.drawingView.bringToFront();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (x < initialX) {
                    currentRect.left = x; // Shrink leftwards
                } else {
                    currentRect.right = x; // Expand rightwards
                }

                // If moving up or down from initial position, adjust accordingly
                if (y < initialY) {
                    currentRect.top = y; // Shrink upwards
                } else {
                    currentRect.bottom = y; // Expand downwards
                }
                invalidate(); // Redraw the view
                return true;
            case MotionEvent.ACTION_UP:
                // Store the rectangle and take a screenshot
                rectangles.add(currentRect); // Store the rectangle
                //invalidate();
                // Capture the area as a screenshot when finger released after drawing a rectangle
                Bitmap fullscreenshot = ScreenShotUtil.takeScreenshot(activity);
                Bitmap snippet = ScreenShotUtil.cropScreenshot(fullscreenshot, currentRect, this);

                if (snippet != null) {
                    // After capturing the screen shot exactly on the size of the rectangle we will do this routine:
                    // 1. Send the captured snippet to OCR and get the text back
                    // 2. Fill it with white color
                    // 3. Send the image to the server for OCR and get the text using the callback
                    // 4. get back the text and put it inside the box (rect)

                    AI_OCR_CLIENT.getOcrText(snippet, chosen_lang, new AI_OCR_CLIENT.OcrCallback() {
                        @Override
                        public void onOcrResult(String result) {
                            // Log OCR result or handle it as needed
                            Log.d(TAG, "OCR result: " + result);
                            // Ensure UI updates happen on the main thread
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    // This will be executed on the main thread
                                    fillSnippet(currentRect, result); // Update the UI here
                                    copyTextToClipboard(result);
                                    currentRect = null;
                                    invalidate();
                                }
                            });
                        }
                        @Override
                        public void onError(String error) {
                            // Handle the error if OCR fails
                            currentRect = null;
                            Toast.makeText(getContext(), "UnknownError", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "OCR Error: " + error);
                        }
                    });
                    Log.d(TAG, "Captured snippet: " + snippet.getWidth() + "x" + snippet.getHeight());
                } else {
                    snippets.add(null);
                    Log.d(TAG, "Snippet capture failed.");
                }
                invalidate(); // Redraw the view
                return true;
        }
        return super.onTouchEvent(event);
    }

    private void saveBitmapToFile(Bitmap bitmap, String fileName) {
        if (bitmap == null) {
            Log.d(TAG, "Bitmap is null, cannot save.");
            return;
        }

        // Define the file path and name
        File storageDir = getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File file = new File(storageDir, fileName);

        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out); // Save bitmap as PNG
            Log.d(TAG, "Bitmap saved: " + file.getAbsolutePath());
        } catch (IOException e) {
            Log.d(TAG, "Error saving bitmap: " + e.getMessage());
        }
    }

    public Bitmap captureArea(Rect area) {
        if (imageBitmap == null || area == null) {
            Log.d("DrawingView", "ImageBitmap is null or area is null.");
            return null; // Ensure the bitmap and area are valid
        }

        // Get the scale factors
        float scaleX = (float) imageBitmap.getWidth() / getWidth();
        float scaleY = (float) imageBitmap.getHeight() / getHeight();

        // Scale rectangle coordinates to match imageBitmap size
        int scaledLeft = (int) (area.left * scaleX);
        int scaledTop = (int) (area.top * scaleY);
        int scaledRight = (int) (area.right * scaleX);
        int scaledBottom = (int) (area.bottom * scaleY);

        // Create a new scaled Rect to use for cropping
        Rect scaledArea = new Rect(scaledLeft, scaledTop, scaledRight, scaledBottom);

        // Ensure the scaled area is within the bounds of the imageBitmap
        if (scaledArea.left < 0 || scaledArea.top < 0 ||
                scaledArea.right > imageBitmap.getWidth() ||
                scaledArea.bottom > imageBitmap.getHeight() ||
                scaledArea.width() <= 0 ||
                scaledArea.height() <= 0) {
            Log.d(TAG, "Invalid scaled area dimensions or coordinates.");
            return null;
        }

        // Create the cropped bitmap using scaled coordinates
        try {
            return Bitmap.createBitmap(imageBitmap, scaledArea.left, scaledArea.top, scaledArea.width(), scaledArea.height());
        } catch (Exception e) {
            Log.d(TAG, "Exception while capturing area: " + e.getMessage());
            return null; // Handle any exceptions that might arise during bitmap creation
        }
    }

    // Clear all rectangles and snippets
    public void clearRectangles() {
        rectangles.clear();
        snippets.clear();
        invalidate(); // Redraw the view
    }

    public List<Rect> getRectangles() {
        return rectangles;
    }
    // Make the snippet filled with white color to prepare to populate it with texts from OCR
    private void fillSnippet(Rect rect, String text) {
        // Create a bitmap for the snippet with proper dimensions
        Bitmap snippet = Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.ARGB_8888);
        Canvas snippetCanvas = new Canvas(snippet);

        // Create a paint object for white color
        Paint fillPaint = new Paint();
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(Color.WHITE);
        snippetCanvas.drawRect(0, 0, rect.width(), rect.height(), fillPaint);

        // Create a TextPaint object for the text
        TextPaint textPaint = new TextPaint();
        textPaint.setColor(Color.BLACK);
        textPaint.setAntiAlias(true);

        // Start with a large text size
        int textSize = 30;
        textPaint.setTextSize(textSize);

        // Measure the text and decrease the text size until it fits within the rectangle
        StaticLayout staticLayout;
        do {
            textPaint.setTextSize(textSize);
            staticLayout = new StaticLayout(text, textPaint, rect.width(),
                    Layout.Alignment.ALIGN_CENTER, 1.0f, 0.0f, false); // ALIGN_CENTER for center alignment
            textSize--;
        } while (staticLayout.getHeight() > rect.height() && textSize > 1); // Stop when it fits

        // Draw the wrapped text on the canvas
        float y = (rect.height() - staticLayout.getHeight()) / 2; // Center Y position

        snippetCanvas.save();
        snippetCanvas.translate(0, y); // Translate vertically
        staticLayout.draw(snippetCanvas);
        snippetCanvas.restore();

        // Store the snippet bitmap in the snippets list
        snippets.add(snippet);
    }


    public void copyTextToClipboard(String text) {
        // Get the ClipboardManager system service using ContextCompat
        ClipboardManager clipboard = ContextCompat.getSystemService(getContext(), ClipboardManager.class);

        // Create a ClipData object with the text to be copied
        android.content.ClipData clip = android.content.ClipData.newPlainText("OCR Text", text);

        // Set the ClipData to the clipboard
        clipboard.setPrimaryClip(clip);

        // Optionally show a toast message
        Toast.makeText(this.getContext(), "Text copied to clipboard", Toast.LENGTH_SHORT).show();
    }

}
