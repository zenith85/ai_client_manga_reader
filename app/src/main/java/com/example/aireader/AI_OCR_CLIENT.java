package com.example.aireader;

import android.graphics.Bitmap;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AI_OCR_CLIENT {

    private static final String SERVER_IP = "192.168.0.228";  // Replace with your actual server IP
    private static final int SERVER_PORT = 65432;  // Port for the server

    // ExecutorService for managing background tasks
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    /**
     * This method sends the image (as byte array) and language to the OCR server and retrieves the OCR result.
     * @param imageData The image data in byte array format.
     * @param language The language for OCR processing (e.g., "en", "ko", "ja").
     * @param callback Callback to receive the result on completion
     */
    public static void sendImageForOcr(byte[] imageData, String language, OcrCallback callback) {
        executorService.submit(() -> {
            try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
                 DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                 InputStream inputStream = socket.getInputStream()) {

                Log.d("AI_OCR_CLIENT", "Connecting to server...");

                // Send the language length as a 4-byte integer
                byte[] languageBytes = language.getBytes("UTF-8");
                outputStream.writeInt(languageBytes.length);
                Log.d("AI_OCR_CLIENT", "Language length: " + languageBytes.length);

                // Send the language string as a byte array
                outputStream.write(languageBytes);
                Log.d("AI_OCR_CLIENT", "Language sent: " + language);

                // Send the image data
                outputStream.writeInt(imageData.length); // Send the length of the image
                outputStream.write(imageData); // Send the actual image
                Log.d("AI_OCR_CLIENT", "Image data sent.");

                // Read the server's response
                ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    responseStream.write(buffer, 0, bytesRead);
                }

                // Return the OCR result as a string
                String result = responseStream.toString("UTF-8");
                Log.d("AI_OCR_CLIENT", "OCR result received.");
                Log.d("AI_OCR_CLIENT", "Received OCR Result: " + result);

                // Pass result to the callback on the main thread
                if (callback != null) {
                    callback.onOcrResult(result);
                }

            } catch (IOException e) {
                Log.e("AI_OCR_CLIENT", "Error during OCR transmission", e);
                if (callback != null) {
                    callback.onError("Error: " + e.getMessage());
                }
            }
        });
    }

    /**
     * This method is a higher-level API that converts the Bitmap to a byte array and calls sendImageForOcr.
     * @param bitmap The bitmap of the image to be processed.
     * @param language The language for OCR processing (e.g., "en", "ko", "ja").
     * @param callback Callback to receive the result
     */
    public static void getOcrText(Bitmap bitmap, String language, OcrCallback callback) {
        // Convert the bitmap to a byte array
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream);
        byte[] byteArray = stream.toByteArray();

        // Call the method to send the image for OCR processing
        sendImageForOcr(byteArray, language, callback);
    }

    /**
     * Callback interface to receive OCR results or errors
     */
    public interface OcrCallback {
        void onOcrResult(String result);

        void onError(String error);
    }
}
