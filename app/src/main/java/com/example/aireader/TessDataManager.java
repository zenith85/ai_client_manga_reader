package com.example.aireader;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class TessDataManager {
    static final String TAG = "DBG_" + TessDataManager.class.getName();

    private static String tessdir = "tesseract";
    private static String subdir = "tessdata";
    //private static final String filename = "eng.traineddata";
    private static String filename = "kor.traineddata";
    private static String trainedDataPath;
    private static String tesseractFolder;

    public static String getTesseractFolder() {
        return tesseractFolder;
    }
    public static String getTrainedDataPath(){
        return initiated ? trainedDataPath : null;
    }

    private static boolean initiated;

    public static void initTessTrainedData(Context context, String lang){
        if(initiated)
            return;

        if (lang == "eng"){
            filename = "eng.traineddata";
        }else if (lang == "kor"){
            filename = "kore.traineddata";
        }else if (lang == "jpn"){
            filename = "jpn.traineddata";
        }else if (lang == "jpn_vert"){
           filename = "jpn.traineddata";
        } else {
            filename = "eng.traineddata";
        }

        File appFolder = context.getFilesDir();        //할당된 디렉토리 경로 및 임의의 파일명을 지정
        File folder = new File(appFolder, tessdir);        //파일의 경로와 이름을 따로 분리해서 지정할 수 있도록 한 생성자.
        if(!folder.exists())
            folder.mkdir();
        tesseractFolder = folder.getAbsolutePath();

        File subfolder = new File(folder, subdir);
        if(!subfolder.exists())
            subfolder.mkdir();

        File file = new File(subfolder, filename);
        trainedDataPath = file.getAbsolutePath();   // 현재 위치의 절대경로를 tessdata의 path로 지정해준다.
        Log.d(TAG, "Trained data filepath: " + trainedDataPath);

        if(!file.exists()) {
            try {
                FileOutputStream fileOutputStream;          //tessdata를 쓸 filOutputStream을 생성했다.
                byte[] bytes = readRawTrainingData(context , lang);        //raw폴더에서 tessdata를 가져온다.
                if (bytes == null)
                    return;
                fileOutputStream = new FileOutputStream(file);
                fileOutputStream.write(bytes);
                fileOutputStream.close();
                initiated = true;
                Log.d(TAG, "Prepared training data file");
            } catch (FileNotFoundException e) {
                Log.e(TAG, "Error opening training data file\n" + e.getMessage());
            } catch (IOException e) {
                Log.e(TAG, "Error opening training data file\n" + e.getMessage());
            }
        }
        else{
            initiated = true;
        }
    }

    private static byte[] readRawTrainingData(Context context, String lang){

        try {
            //it was eng_traineddata in the reference
//            InputStream fileInputStream = context.getResources()
//                    .openRawResource(R.raw.eng);
            AssetManager assetMgr = context.getAssets();
            InputStream fileInputStream = null;

            if (lang == "eng"){
                fileInputStream = assetMgr.open("eng.traineddata");
            }else if (lang == "kor"){
                fileInputStream = assetMgr.open("kor.traineddata");
            }else if (lang == "jpn"){
                fileInputStream = assetMgr.open("jpn.traineddata");
            }else if (lang == "jpn_vert"){
                fileInputStream = assetMgr.open("jpn_vert.traineddata");
            } else {
                fileInputStream = assetMgr.open("eng.traineddata");
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            byte[] b = new byte[1024];

            int bytesRead;

            while (( bytesRead = fileInputStream.read(b))!=-1){
                bos.write(b, 0, bytesRead);
            }

            fileInputStream.close();

            return bos.toByteArray();

        } catch (FileNotFoundException e) {
            Log.e(TAG, "Error reading raw training data file\n"+e.getMessage());
            return null;
        } catch (IOException e) {
            Log.e(TAG, "Error reading raw training data file\n" + e.getMessage());
        }
        return null;
    }

}
//출처: https://jinseongsoft.tistory.com/42 [진성 소프트:티스토리]