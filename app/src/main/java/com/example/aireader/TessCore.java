package com.example.aireader;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import com.googlecode.tesseract.android.TessBaseAPI;
import org.opencv.android.OpenCVLoader;
import java.util.Arrays;
import java.util.List;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.CvType;
import org.opencv.imgproc.Imgproc;


public class TessCore {
    static final String TAG = "DBG_" + TessCore.class.getName();
    private Context mCtx;

    public TessCore(Context context) {
        this.mCtx = context;
    }
//return list of words
//    public List<String> detectText(Bitmap bitmap) {
    public String detectText(Bitmap bitmap,String lang){
        // Load OpenCV if not already loaded
        if (!OpenCVLoader.initDebug()) {
            Log.e(TAG, "OpenCV initialization failed.");
            return null;
        }
        Log.d(TAG, "Initialization of TessBaseApi");
        TessDataManager.initTessTrainedData(mCtx,lang);
        TessBaseAPI tessBaseAPI = new TessBaseAPI();
        String path = TessDataManager.getTesseractFolder();
        Log.d(TAG, "Tess folder: " + path);
        tessBaseAPI.setDebug(true);
        if (lang == "eng"){
            tessBaseAPI.init(path, "eng");
            tessBaseAPI.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ");
            tessBaseAPI.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, "!@#$%^&*()_+=-[]}{;:'\"\\|~`,./<>?");
        }else if (lang == "jpn" || lang == "jpn_vert"){
            if (lang == "jpn"){
                tessBaseAPI.init(path, "jpn");
            }else{
                tessBaseAPI.init(path, "jpn_vert");
            }
            String hiraganaWhitelist = "ぁあぃいぅうぇえぉおかがきぎくぐけげこごさざしじすずせぜそぞただちぢつづてでとどなにぬねのはばひびふぶへべほぼまみむめもやゆよらりるれろわをん";
            String katakanaWhitelist = "ァアィイゥウェエォオカガキギクグケゲコゴサザシジスズセゼソゾタダチヂツヅテデトドナニヌネノハバヒビフブヘベホボマミムメモヤユヨラリルレロワヲン";
            String kanjiWhitelist = "一二三四五六七八九十百千万円口目田土"; // Example subset of Kanji
            String fullWhitelist = hiraganaWhitelist + katakanaWhitelist + kanjiWhitelist;
            tessBaseAPI.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, fullWhitelist);

//            tessBaseAPI.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "あいうえおかきくけこ"); // Example for Japanese
//            tessBaseAPI.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, "!@#$%^&*()_+=-[]}{;:'\"\\|~`,./<>?");
        }else if (lang == "kor"){
            tessBaseAPI.init(path, "kor");
            String koreanWhitelist =
                    "가-힣" + // Hangul syllables (Complete range)
                            "ㄱ-ㅎ" + // Initial consonants (complete)
                            "ㅏ-ㅣ" + // Basic vowels (complete)
                            "ㅐ, ㅔ, ㅚ, ㅟ, ㅘ, ㅙ, ㅝ, ㅞ, ㅠ, ㅡ, ㅢ," + // Additional vowels
                            "ㄲ, ㄸ, ㅃ, ㅉ," + // Tensed consonants
                            "가-깋, 곰-굼, 그-귿, 나-닣, 다-딯, 라-맇, 바-빟, 사-싷, 아-잏, 자-짛, 차-칳, 카-킿, 타-퍋, 파-핳"; // Further combinations

            //tessBaseAPI.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, koreanWhitelist);
        }
        //tessBaseAPI.setPageSegMode(TessBaseAPI.PageSegMode.PSM_RAW_LINE);
        Log.d(TAG, "Ended initialization of TessEngine");
        Log.d(TAG, "Running inspection on bitmap");
        tessBaseAPI.setImage(bitmap);
        String inspection = tessBaseAPI.getUTF8Text();
        Log.d(TAG, "=====Got data=====" + inspection);
        tessBaseAPI.end();
        return inspection;
    }
}
