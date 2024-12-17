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
import org.json.JSONException;
import org.json.JSONObject;

public class DrawingView extends View {
    private static String TAG = "DrawingView ";
    public static boolean select_mode;
    private Paint paint;
    public static String LANG_DIRECTION = "ENGENG";
    private Rect currentRect;
    private List<Rect> rectangles = new ArrayList<>();
    private List<Bitmap> snippets = new ArrayList<>();
    private Bitmap imageBitmap; // The bitmap of the image from which to capture snippets
    private Activity activity; // Declare Activity variable
    private boolean isTemporaryText = false;


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
            if (rect!=null){
                canvas.drawRect(rect, paint); // Draw the rectangle
            }

            // If there is a snippet corresponding to this rectangle, draw it
            if (snippets.size() > i && snippets.get(i) != null) {
                Bitmap snippet = snippets.get(i);
                if (snippet!=null){
                    // Ensure the snippet is scaled to fit the rectangle
                    canvas.drawBitmap(snippet, null, rect, null); // Draw the snippet
                }
            }
        }

        // Draw the current rectangle being drawn
        if (currentRect != null) {
            canvas.drawRect(currentRect, paint);
            // If it's temporary, draw "TRANSLATING" inside the current rect
            if (isTemporaryText) {
                // Create a paint object for the background (white)
                Paint bgPaint = new Paint();
                bgPaint.setColor(Color.WHITE);
                bgPaint.setStyle(Paint.Style.FILL);
                canvas.drawRect(currentRect,bgPaint);
                Paint textPaint = new Paint();
                textPaint.setColor(Color.BLACK);
                textPaint.setTextSize(30);
                textPaint.setTextAlign(Paint.Align.CENTER);
                // Calculate the x and y position to center the text inside the background
                float x = currentRect.centerX();
                float y = currentRect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2;
                canvas.drawText("TRANSLATING", x, y, textPaint);
            }
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
                if (currentRect!=null){
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
                }
                invalidate(); // Redraw the view
                return true;
            case MotionEvent.ACTION_UP:
                // Store the rectangle and take a screenshot
                rectangles.add(currentRect); // Store the rectangle
                // Capture the area as a screenshot when finger released after drawing a rectangle
                Bitmap fullscreenshot = ScreenShotUtil.takeScreenshot(activity);

                Bitmap snippet = ScreenShotUtil.cropScreenshot(fullscreenshot, currentRect, this);

                if (snippet != null) {
                    isTemporaryText = true;
                    /**
                    * After capturing the screen shot exactly on the size of the rectangle we will do this routine:
                    * 1. Send the captured snippet to OCR and get the text back
                    * 2. Fill it with white color
                    * 3. Send the image to the server for OCR and get the text using the callback
                    * 4. get back the text and put it inside the box (rect)
                    */
                    AI_OCR_CLIENT.getOcrText(snippet, LANG_DIRECTION, new AI_OCR_CLIENT.OcrCallback() {
                        @Override
                        public void onOcrResult(String result) {
                            isTemporaryText = false;
                            // Log OCR result or handle it as needed
                            Log.d(TAG, "OCR result: " + result);
                            try {
                                JSONObject jsonResponse = new JSONObject(result); // Assuming 'result' is a JSON string like {"translated_result": "translated text here"}
                                String translatedText = jsonResponse.getString("translated_result");// Extract the translated result
                                activity.runOnUiThread(new Runnable() { // Ensure UI updates happen on the main thread
                                    @Override
                                    public void run() {
                                        // This will be executed on the main thread
                                        if (currentRect!=null){
                                            fillSnippet(currentRect, translatedText); // Update the UI with the translated text
                                            copyTextToClipboard(translatedText); // Copy translated text to clipboard
                                            currentRect = null;
                                        }
                                        invalidate(); // If you need to invalidate the UI
                                    }
                                });
                            } catch (JSONException e) {
                                Log.e(TAG, "Error parsing JSON: " + e.getMessage());
                                // Handle parsing error, e.g., show an error message to the user
                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        // Handle the error case on the main thread
                                        if (currentRect!=null){
                                            fillSnippet(currentRect, "Error parsing response");
                                        }
                                        currentRect = null;
                                        invalidate();
                                    }
                                });
                            }
                        }

                        @Override
                        public void onError(String error) {
                            // Handle any errors
                            Log.e(TAG, "OCR Error: " + error);
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    // Handle the error on the main thread
                                    if (currentRect!=null){
                                        fillSnippet(currentRect, "OCR failed");
                                    }
                                    currentRect = null;
                                    invalidate();
                                }
                            });
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

    public void clearRectangles() { // Clear all rectangles and snippets
        rectangles.clear();
        snippets.clear();
        invalidate(); // Redraw the view
    }

    public List<Rect> getRectangles() {
        return rectangles;
    }
    // Make the snippet filled with white color to prepare to populate it with texts from OCR
    private void fillSnippet(Rect rect, String text) {
        Bitmap snippet = Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.ARGB_8888);        // Create a bitmap for the snippet with proper dimensions
        Canvas snippetCanvas = new Canvas(snippet);
        Paint fillPaint = new Paint();        // Create a paint object for white color
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(Color.WHITE);
        snippetCanvas.drawRect(0, 0, rect.width(), rect.height(), fillPaint);
        TextPaint textPaint = new TextPaint();// Create a TextPaint object for the text
        textPaint.setColor(Color.BLACK);
        textPaint.setAntiAlias(true);
        int textSize = 30; // Start with a large text size
        textPaint.setTextSize(textSize);
        StaticLayout staticLayout;// Measure the text and decrease the text size until it fits within the rectangle
        do {
            textPaint.setTextSize(textSize);
            staticLayout = new StaticLayout(text, textPaint, rect.width(),
                    Layout.Alignment.ALIGN_CENTER, 1.0f, 0.0f, false); // ALIGN_CENTER for center alignment
            textSize--;
        } while (staticLayout.getHeight() > rect.height() && textSize > 1); // Stop when it fits
        float y = (rect.height() - staticLayout.getHeight()) / 2; // Center Y position  // Draw the wrapped text on the canvas
        snippetCanvas.save();
        snippetCanvas.translate(0, y); // Translate vertically
        staticLayout.draw(snippetCanvas);
        snippetCanvas.restore();
        snippets.add(snippet); // Store the snippet bitmap in the snippets list
    }


    public void copyTextToClipboard(String text) {
        ClipboardManager clipboard = ContextCompat.getSystemService(getContext(), ClipboardManager.class);        // Get the ClipboardManager system service using ContextCompat
        android.content.ClipData clip = android.content.ClipData.newPlainText("OCR Text", text);        // Create a ClipData object with the text to be copied
        clipboard.setPrimaryClip(clip);        // Set the ClipData to the clipboard
        Toast.makeText(this.getContext(), "Text copied to clipboard", Toast.LENGTH_SHORT).show(); // Optionally show a toast message
    }

}
