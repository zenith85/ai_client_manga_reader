package com.example.aireader;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

public class Show_Information_dialog extends Dialog {

    private ImageView leftImageView, rightImageView;
    private TextView nextImageText;
    private int[] images; // The array of images
    private int currentImageIndex = 0;

    public Show_Information_dialog(@NonNull Context context) {
        super(context);
        setContentView(R.layout.dialog_image_information);

        // Initialize ImageViews and TextView
        leftImageView = findViewById(R.id.left_image_view);
        rightImageView = findViewById(R.id.right_image_view);
        nextImageText = findViewById(R.id.nextImageText);

        // Set up the images array with your images (example: image1, image2, etc.)
        images = new int[] {
                R.drawable.astrogirl,  // Your first image
                R.drawable.astrogirl1,  // Your second image
                R.drawable.astrogirl2,  // Your third image
                R.drawable.astrogirl3   // Your fourth image
        };

        // Set initial images in the ImageViews
        leftImageView.setImageResource(images[currentImageIndex]);
        //rightImageView.setImageResource(images[(currentImageIndex + 1) % images.length]);

        // Set listener for "Next Tip!" TextView
        nextImageText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeImages();  // Change images on click
            }
        });

        // Set listener for Close Button (X)
        ImageButton closeButton = findViewById(R.id.close_button);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss(); // Close the dialog
            }
        });
    }

    // Method to change images
    private void changeImages() {
        // Increment the image index and reset if necessary
        currentImageIndex = (currentImageIndex + 1) % images.length;

        // Set the new images to the ImageViews
        leftImageView.setImageResource(images[currentImageIndex]);
        //rightImageView.setImageResource(images[(currentImageIndex + 1) % images.length]);
    }
}

