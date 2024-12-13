package com.example.aireader;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.provider.MediaStore;
import org.opencv.android.OpenCVLoader;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.view.ScaleGestureDetector;
import android.view.View;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;

import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity implements AICallback {

    private static String TAG = "MAIN_ACTIVITY ";
    private static final int PICK_FILE_REQUEST = 1;
    private ImageView chosenImageView;
    private DrawingView drawingView;
    private Bitmap currentBitmap;
    private PdfRenderer pdfRenderer;
    private PdfRenderer.Page currentPage;
    private int currentPageIndex = 0;
    private float currentScale = 1.0f;
    private Uri fileUri;
    private AlertDialog textDialog;

    private Spinner lang_conv_spinner;

    private float originalScale = 1.0f;
    private float originalX = 0.0f;
    private float originalY = 0.0f;

    // Variables for tracking touch position
    private float previousX = 0f;
    private float previousY = 0f;

    private View floatingTool;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Here you show the information dialogue
        // Show the custom dialog
        Show_Information_dialog customDialog = new Show_Information_dialog(this);
        customDialog.show();
        // Hide the status bar and navigation bar for full-screen mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // Hide status bar
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            decorView.setSystemUiVisibility(uiOptions);
        }
        // Its the begining, select mode is just false
        drawingView.select_mode = false;
        // Initialize the ImageView and set up the drawing view
        chosenImageView = new ImageView(this);
        FrameLayout pdfContainer = findViewById(R.id.pdf_container);
        pdfContainer.addView(chosenImageView);

        drawingView = findViewById(R.id.drawings_view);

        // Inflate the floating tool layout and add it to the main layout
        floatingTool = LayoutInflater.from(this).inflate(R.layout.floating_tool, null);
        ConstraintLayout mainLayout = findViewById(R.id.main_layout);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        float xPercent = 0.85f;  // 10% from the left
        float yPercent = 0.7f;  // 20% from the top
        floatingTool.setX(dm.widthPixels*xPercent);
        floatingTool.setY(dm.heightPixels*yPercent);
        // Button references will be accessed only after adding the floating tool to the layout.

        // Set up drag functionality
        setDraggable(floatingTool);

        //Find the left and right buttons
        Button leftButton = findViewById(R.id.button_turn_left);
        Button rightButton = findViewById(R.id.button_turn_right);
        // Set up click listeners for page navigation
        leftButton.setOnClickListener(v -> previousPage());
        rightButton.setOnClickListener(v -> nextPage());

        // Setup the file import button
        Button importPdfButton = findViewById(R.id.button_import_pdf);
        importPdfButton.setOnClickListener(v -> openFile());

        // Setup aitool button to summon the floating tool
        Button aitoolbtn = findViewById(R.id.aitool);
        aitoolbtn.setOnClickListener(v -> {
            v.setActivated(!v.isActivated()); // Toggle the selected state
            if (v.isActivated()){
                Log.d(TAG,"floating_tool is added to the view!");
                mainLayout.addView(floatingTool);
                // Initialize zoom buttons
                Button zoomInButton = findViewById(R.id.PlusButton);
                Button zoomOutButton = findViewById(R.id.MinusButton);
                zoomInButton.setOnClickListener(view -> zoomIn()); // Zoom In Button Action
                zoomOutButton.setOnClickListener(view -> zoomOut());   // Zoom Out Button Action
                Button captureButton = findViewById(R.id.Capture);
                captureButton.setOnClickListener(view -> {
                    view.setActivated(!view.isActivated());
                    if (view.isActivated()){
                        drawingView.bringToFront();
                        drawingView.select_mode=true;
                        Log.d(TAG,"Current Selection Mode " +  drawingView.select_mode);
                    }else{
                        drawingView.select_mode=false;
                        Log.d(TAG,"Current Selection Mode " +  drawingView.select_mode);
                    }
                    //drawingView.select_mode = !drawingView.select_mode; // Activate drawing mode in DrawingView class
                    Log.d(TAG,"Current Selection Mode = toggle");
                });

            }else{
                mainLayout.removeView(floatingTool);
                Log.d(TAG,"floating_tool is removed from the view!");
            }
//
        });

        // Setup the refresh button to clear rectangles
        Button refreshButton = findViewById(R.id.Refresh);
        refreshButton.setOnClickListener(v -> {
            drawingView.clearRectangles();  // Clear rectangles from DrawingView
            resetView();  // Reset the zoom and position
            EPDManager _EPDManager = new EPDManager(MainActivity.this);
            _EPDManager.refreshScreen();
        });

        // Spinner initialization
        spinner_init();

        if (!drawingView.select_mode){
            Log.d(TAG,"Current Selection Mode = false");
            chosenImageView.setOnTouchListener((v, event) -> {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        // Store initial touch position for pan movement
                        previousX = event.getRawX();
                        previousY = event.getRawY();
                        break;

                    case MotionEvent.ACTION_MOVE:
                        if (event.getPointerCount() == 1) {
                            // Single finger drag (pan movement)
                            float deltaX = event.getRawX() - previousX;
                            float deltaY = event.getRawY() - previousY;

                            // Apply translation to the image
                            chosenImageView.setTranslationX(chosenImageView.getTranslationX() + deltaX);
                            chosenImageView.setTranslationY(chosenImageView.getTranslationY() + deltaY);

                            // Update previous position for next move
                            previousX = event.getRawX();
                            previousY = event.getRawY();

                            drawingView.clearRectangles();
                        }
                        break;

                    case MotionEvent.ACTION_POINTER_UP:
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        break;
                }
                return true;
            });
        } else{
            Log.d(TAG,"Current Selection Mode = true");
        }
    }



    // Helper method to calculate the distance between two touch points
    private float getPinchDistance(MotionEvent event) {
        float dx = event.getX(0) - event.getX(1);
        float dy = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private void setDraggable(View view) {
        view.setOnTouchListener(new View.OnTouchListener() {
            private float dX, dY;
            private int lastAction;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = v.getX() - event.getRawX();
                        dY = v.getY() - event.getRawY();
                        lastAction = MotionEvent.ACTION_DOWN;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        v.setX(event.getRawX() + dX);
                        v.setY(event.getRawY() + dY);
                        lastAction = MotionEvent.ACTION_MOVE;
                        return true;

                    case MotionEvent.ACTION_UP:
                        if (lastAction == MotionEvent.ACTION_DOWN) {
                            // Handle click if needed
                        }
                        return true;

                    default:
                        return false;
                }
            }
        });
    }

    private void spinner_init() {
        lang_conv_spinner = findViewById(R.id.lang_conv_spinner);
        String[] itemList = getResources().getStringArray(R.array.spinner_values);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, itemList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        lang_conv_spinner.setAdapter(adapter);

        // Spinner item selection handling
        lang_conv_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedValue = parent.getItemAtPosition(position).toString();
                //drawingView.chosen_lang = selectedValue.equals("ENG") ? "en" : (selectedValue.equals("KOR") ? "ko" : "en");
                Toast.makeText(MainActivity.this, "Selected: " + selectedValue, Toast.LENGTH_SHORT).show();
                languageProvider();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null) {
            fileUri = data.getData();
            if (fileUri != null) {
                String fileType = getContentResolver().getType(fileUri);
                if ("application/pdf".equals(fileType)) {
                    openPdfFile(fileUri);
                } else if (fileType != null && fileType.startsWith("image/")) {
                    openImageFile(fileUri);
                } else {
                    Toast.makeText(this, "Unsupported file type", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    private void zoomIn() {
        currentScale *= 1.2f;
        chosenImageView.setScaleX(currentScale);
        chosenImageView.setScaleY(currentScale);
    }
    private void zoomOut() {
        currentScale /= 1.2f;
        chosenImageView.setScaleX(currentScale);
        chosenImageView.setScaleY(currentScale);
    }
    private void openPdfFile(Uri fileUri) {
        try {
            ParcelFileDescriptor fileDescriptor = getContentResolver().openFileDescriptor(fileUri, "r");
            if (fileDescriptor != null) {
                pdfRenderer = new PdfRenderer(fileDescriptor);
                showPage(currentPageIndex);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openImageFile(Uri fileUri) {
        try {
            Bitmap selectedImage = MediaStore.Images.Media.getBitmap(this.getContentResolver(), fileUri);
            showImage(selectedImage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showImage(Bitmap bitmap) {
        if (bitmap != null) {
            chosenImageView.setImageBitmap(bitmap);
            currentBitmap = bitmap;
            drawingView.setImageBitmap(currentBitmap); // Update the drawing view with the new image
            //drawingView.bringToFront(); // Bring drawing view to the front for interaction
            // Store the initial scale and position of the image
            originalScale = 1.0f;  // Default zoom level
            originalX = drawingView.getTranslationX();  // Get the current translation on X axis
            originalY = drawingView.getTranslationY();  // Get the current translation on Y axis
        }
    }

    private void showPage(int index) {
        if (pdfRenderer == null) return;
        if (currentPage != null) {
            currentPage.close();
        }
        currentPage = pdfRenderer.openPage(index);
        Bitmap bitmap = Bitmap.createBitmap(currentPage.getWidth(), currentPage.getHeight(), Bitmap.Config.ARGB_8888);
        currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        chosenImageView.setImageBitmap(bitmap);
    }

    private void openFile() {
        drawingView.clearRectangles();
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        try {
            startActivityForResult(intent, PICK_FILE_REQUEST);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No application found to open files.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case 92: previousPage(); return true;
            case 93: nextPage(); return true;
            default: return super.onKeyDown(keyCode, event);
        }
    }

    private void nextPage() {
        if (pdfRenderer != null && currentPageIndex < pdfRenderer.getPageCount() - 1) {
            currentPageIndex++;
            showPage(currentPageIndex);
        }
    }

    private void previousPage() {
        if (pdfRenderer != null && currentPageIndex > 0) {
            currentPageIndex--;
            showPage(currentPageIndex);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pdfRenderer != null) {
            pdfRenderer.close();
        }
        if (currentPage != null) {
            currentPage.close();
        }
    }

    @Override
    public void showTextDialog(String title, String text) {
    }

    @Override
    public void onAIResponse(String response) {}

    @Override
    public void onError(String error) {}

    // Pinch-to-zoom handling using ScaleGestureDetector
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            currentScale *= scaleFactor;
            currentScale = Math.max(0.1f, Math.min(currentScale, 5.0f)); // Prevent excessive zooming
            chosenImageView.setScaleX(currentScale);
            chosenImageView.setScaleY(currentScale);
            return true;
        }
    }
    // Reset the scale and position when the "R" button is clicked
    private void resetView() {
        // Reset the image view to its initial scale and position
        chosenImageView.setScaleX(originalScale);
        chosenImageView.setScaleY(originalScale);
        chosenImageView.setTranslationX(originalX);
        chosenImageView.setTranslationY(originalY);
        Log.d(TAG,"View Reset");
        // Optionally, reset any other states such as the current scale if you have any
    }

    public void languageProvider(){
        String lang_from = lang_conv_spinner.getSelectedItem().toString();
        //drawingView.chosen_lang = selectedValue.equals("ENG") ? "en" : (selectedValue.equals("KOR") ? "ko" : "en");
        if (lang_from.equals("ENG to KOR")){drawingView.LANG_DIRECTION="ENGKOR";}
        if (lang_from.equals("KOR to ENG")){drawingView.LANG_DIRECTION="KORENG";}
        if (lang_from.equals("ENG")){drawingView.LANG_DIRECTION="ENGENG";}
        if (lang_from.equals("KOR")){drawingView.LANG_DIRECTION="KORKOR";}
        Log.d(TAG,drawingView.LANG_DIRECTION);
    }
}
