package edu.stanford.ee368.cameratranslation;
/**
 * Created by francischen on 11/17/16.
 */

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.imgproc.Imgproc;


import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.content.IntentFilter;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;
import android.util.SparseArray;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.jar.Manifest;

import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.google.android.gms.vision.Frame;

public class CameraTranslation extends Activity implements CvCameraViewListener2, View.OnTouchListener {

    /** defining class private variables**/
    // initialize the database
    private DatabaseFile database = new DatabaseFile();

    // initialize UI activities
    private static final String TAG = "translation::Activity";
    private TranslationCameraView mOpenCVCameraView;
    private ImageButton voiceButton;
    private ImageButton searchButton;
    private boolean isSearchButtonPressed = false;

    private TextToSpeech voiceTalker;
    private Mat mRgba;
    private Mat mGray;
    private String recognizedText = "";
    private TextRecognizer textRecognizer;
    private GraphicOverlay<OcrGraphic> mGraphicOverLay;

    private Mat PCAMat;
    private Mat topVectors;

    /** defining class for call back **/
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public  void onManagerConnected(int status){
            switch (status){
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCVCameraView.setOnTouchListener(CameraTranslation.this);
                    mOpenCVCameraView.enableView();
                }break;
                default:
                {
                    super.onManagerConnected(status);
                }break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState){
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //mOpenCVCameraView = new TranslationCameraView(this, -1);
        //setContentView(mOpenCVCameraView);
        int permissionCheck = ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA);
        if(permissionCheck != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, 0);
        }
        setContentView(R.layout.camera_translation_view);
        /** initializing camera view **/
        mOpenCVCameraView = (TranslationCameraView) findViewById(R.id.camera_surface_view);
        mOpenCVCameraView.setVisibility(TranslationCameraView.VISIBLE);
        mOpenCVCameraView.setCvCameraViewListener(this);

        /** initializing graphicOverLay **/
        mGraphicOverLay = (GraphicOverlay<OcrGraphic>) findViewById(R.id.graphic_overlay);

        /** initializing search start button **/
        searchButton = (ImageButton) findViewById(R.id.search_button);
        searchButton.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event){
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    if(voiceTalker.isSpeaking()){
                        voiceTalker.stop();
                    }
                    mGraphicOverLay.clear();
                    recognizedText = "";
                    isSearchButtonPressed = true;
                    // this line is changed for block detection
                    // isSearchButtonPressed = !isSearchButtonPressed;
                }
                else if(event.getAction() == MotionEvent.ACTION_UP){
                    // this line is changed for block detection
                    isSearchButtonPressed = false;
                }
                return false;
            }
        });


        /** initializing voice talker **/
        voiceTalker = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener(){
            @Override
            public void onInit(int status){
                if(status != TextToSpeech.ERROR){
                    //voiceTalker.setLanguage(Locale.US);
                    voiceTalker.setLanguage(Locale.CHINESE);
                }
            }
        });

        /** initializing voice button **/
        voiceButton = (ImageButton) findViewById(R.id.voice_button);
        voiceButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                if(voiceTalker.isSpeaking()){
                    voiceTalker.stop();
                }
                if(recognizedText.length() != 0) {
                    Log.i(TAG, "onClick: text to speech successful");
                    voiceTalker.speak(recognizedText, TextToSpeech.QUEUE_FLUSH, null, "translation");
                }
                else{
                    Log.i(TAG, "onClick: text to speech failed. Text is empty.");
                }
            }
        });

        Context context = getApplicationContext();
        textRecognizer = new TextRecognizer.Builder(context).build();

        // check whether the text recognizer is operational
        if(!textRecognizer.isOperational()) {
            Log.e(TAG, "Detector dependencies are not available");
            // check storage
            IntentFilter lowStorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = registerReceiver(null, lowStorageFilter) != null;
            if (hasLowStorage) {
                Log.e(TAG, "Low storage!");
            }

        }
    }

    /** add autofocus and autozoom **/
    public boolean onTouch(View view, MotionEvent event){
        if(event.getPointerCount() > 1){
            mOpenCVCameraView.zoomOnTouch(event);
        }
        else {
            mOpenCVCameraView.focusOnTouch(event);
            // touch to clear recognition results
            mGraphicOverLay.clear();
            recognizedText = "";
        }
        if(voiceTalker.isSpeaking()){
            voiceTalker.stop();
        }
        return true;
    }

    @Override
    public void onPause(){
        super.onPause();
        if(mOpenCVCameraView != null){
            mOpenCVCameraView.disableView();
        }
        if(voiceTalker != null){
            voiceTalker.stop();
            voiceTalker.shutdown();
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.e(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.i(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy(){
        super.onDestroy();
        if(mOpenCVCameraView != null){
            mOpenCVCameraView.disableView();
        }
        if(voiceTalker != null){
            voiceTalker.stop();
            voiceTalker.shutdown();
        }

    }

    public void onCameraViewStarted(int width, int height){
        mGray = new Mat();
        mRgba = new Mat();
        PCAMat = new Mat();
        topVectors = new Mat();
    }

    public void onCameraViewStopped(){
        mGray.release();
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame){
        //Log.i(TAG, "got frame");
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();
        if(!isSearchButtonPressed){
            //Log.i(TAG, "search button not pressed");
            return mRgba;
        }
        Log.i(TAG, "search button is pressed");

        //onGoogleServiceDirect(mRgba);
        //onFeatureDetectorAndGoogleService(mRgba, mGray);
        // this line uses PCA for the whole image
        onImagePCA(mGray);
        return mRgba;
    }


    //*******************************************************************//
    /**
     the following is implemented using Google service
     **/

    private void onGoogleServiceDirect(Mat mRgba){
        recognizedText = "";
        Bitmap bitMap = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.ARGB_8888);

        Utils.matToBitmap(mRgba, bitMap);
        Frame frame = new Frame.Builder().setBitmap(bitMap).build();
        SparseArray<TextBlock> items = textRecognizer.detect(frame);
        for(int i = 0; i < items.size(); i++) {
            TextBlock textBlock = items.valueAt(i);
            if (textBlock != null && textBlock.getValue() != null) {
                Log.i("OcrDetectorProcessor", "Text detected! " + textBlock.getValue());
                recognizedText = recognizedText.concat(textBlock.getValue());
                OcrGraphic graphic = new OcrGraphic(mGraphicOverLay, items.valueAt(i));
                mGraphicOverLay.add(graphic);

            }
        }
    }

    //*******************************************************************//
    /**
     * the following is implemented using OpenCV feature detector plus google service
     */

    private void onFeatureDetectorAndGoogleService(Mat mRgba, Mat mGray){
        Scalar CONTOUR_COLOR = new Scalar(255);
        MatOfKeyPoint matKeyPoint = new MatOfKeyPoint();
        List<KeyPoint> listOfKeyPoints;
        KeyPoint keyPoint;
        Mat mask = Mat.zeros(mGray.size(), CvType.CV_8UC1);
        int rectanX1, rectanX2, rectanY1, rectanY2;
        FeatureDetector detector = FeatureDetector.create(FeatureDetector.MSER);
        detector.detect(mGray, matKeyPoint);
        listOfKeyPoints = matKeyPoint.toList();
        for(int i = 0; i < listOfKeyPoints.size(); i++){
            keyPoint = listOfKeyPoints.get(i);
            rectanX1 = (int)(keyPoint.pt.x - 0.5 * keyPoint.size);
            rectanY1 = (int)(keyPoint.pt.y - 0.5 * keyPoint.size);
            rectanX2 = (int)(keyPoint.size);
            rectanY2 = (int)(keyPoint.size);
            if(rectanX1 <= 0)
                rectanX1 = 1;
            if(rectanY1 <= 0)
                rectanY1 = 1;
            if((rectanX1 + rectanX2) > mGray.width())
                rectanX2 = mGray.width() - rectanX1;
            if((rectanY1 + rectanY2) > mGray.height())
                rectanY2 = mGray.height() - rectanY1;

            Rect rectant = new Rect(rectanX1, rectanY1, rectanX2, rectanY2);
            try{
                Mat roi = new Mat(mask, rectant);
                roi.setTo(CONTOUR_COLOR);
            }
            catch (Exception ex){
                Log.e(TAG, "mat roi error " + ex.getMessage());
            }

        }
        Mat morbyte = new Mat();
        Mat hierachy = new Mat();
        Mat kernel = new Mat(1, 50, CvType.CV_8UC1, Scalar.all(255));
        List<MatOfPoint> contour = new ArrayList<>();
        Scalar zeros = new Scalar(0, 0, 0);

        Imgproc.morphologyEx(mask, morbyte, Imgproc.MORPH_DILATE, kernel);
        Imgproc.findContours(morbyte, contour, hierachy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);
        recognizedText = "";
        for(int i = 0; i < contour.size(); i++){
            Rect rectant = Imgproc.boundingRect(contour.get(i));

            if( rectant.area() < 200 || rectant.width < rectant.height){
                Mat roi = new Mat(morbyte, rectant);
                roi.setTo(zeros);
                continue;
            }

            Point bottomRight = rectant.br();
            Point topLeft = rectant.tl();
            Imgproc.rectangle(mRgba, bottomRight, topLeft, CONTOUR_COLOR, 3);

            /*
            The following is using Google service. Here we need to implement our own algorithm
             */
            Mat subImage = mRgba.submat((int)topLeft.y, (int)bottomRight.y, (int)topLeft.x, (int)bottomRight.x);
            Bitmap bitMap = Bitmap.createBitmap(subImage.cols(), subImage.rows(), Bitmap.Config.ARGB_8888);

            Utils.matToBitmap(subImage, bitMap);
            Frame frame = new Frame.Builder().setBitmap(bitMap).build();
            SparseArray<TextBlock> items = textRecognizer.detect(frame);
            for(int j = 0; j < items.size(); j++) {
                TextBlock textBlock = items.valueAt(j);
                if (textBlock != null && textBlock.getValue() != null) {
                    Log.i("OcrDetectorProcessor", "Text detected! " + textBlock.getValue());
                    recognizedText = recognizedText.concat(textBlock.getValue());
                }
            }
        }
    }
    //*******************************************************************//
    /**
     * the following is implemented using PCA algorithm for the whole image
     */

    private void onDatabasePCA() {
        List<String> fileNames = database.getEnglishVocabulary();
        Mat ensemble = new Mat();
        for(int i = 0; i < fileNames.size(); i++){
            int resourceId = getResources().getIdentifier(fileNames.get(i), "raw", getPackageName());
            try {
                Mat image = Utils.loadResource(this, resourceId, Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);
                image.convertTo(image, CvType.CV_64FC1);
                Imgproc.resize(image, image, new Size(480, 360));
                //Core.normalize(image, image);
                ensemble.push_back(image.reshape(0, 1));
            }
            catch(Exception ex){
                Log.e(TAG, "database error " + ex.getMessage());
                return;
            }

        }
        ensemble = ensemble.t(); // size N * L
        Mat SKMat = new Mat();
        Core.gemm(ensemble.t(), ensemble, 1, new Mat(), 0, SKMat); // this computes image^T * image
        Log.w(TAG, Integer.toString(SKMat.width()) + "\t" + Integer.toString(SKMat.height()));

        Mat eigValues = new Mat();
        Mat eigVectors = new Mat();
        Core.eigen(SKMat, eigValues, eigVectors);
        //topVectors = new Mat();
        //PCAMat = new Mat();
        Core.gemm(ensemble, eigVectors, 1, new Mat(), 0, topVectors);
        for(int i = 0; i < topVectors.width(); i++){
            Mat thisCol = topVectors.col(i);
            Core.normalize(thisCol, thisCol);
        }
        topVectors = topVectors.t(); // size L * N
        Core.gemm(topVectors, ensemble, 1, new Mat(), 0, PCAMat); // size L * L
    }

    private void onImagePCA(Mat mGray) {
        onDatabasePCA();
        mGray.convertTo(mGray, CvType.CV_64FC1);
        Imgproc.resize(mGray, mGray, new Size(480, 360));
        mGray = mGray.reshape(0, 1);
        //Core.normalize(mGray, mGray);
        Mat projection = new Mat();
        Core.gemm(topVectors, mGray.t(), 1, new Mat(), 1, projection); // size L * 1
        double maxSimilarity = 0;
        int maxIndex = 0;

        for(int i = 0; i < PCAMat.width(); i++){
            double similarity = Math.abs(projection.dot(PCAMat.col(i)))
                    / (Math.sqrt(projection.dot(projection)) * Math.sqrt(PCAMat.col(i).dot(PCAMat.col(i))));
            if(similarity > maxSimilarity){
                maxIndex = i;
                maxSimilarity = similarity;
                Log.i(TAG, "similarity" + Double.toString(similarity));
            }
        }
        Log.i(TAG, "the detected word is " + database.getEnglishByIndex(maxIndex));
        recognizedText = database.getEnglishByIndex(maxIndex) + database.getChineseByIndex(maxIndex);
    }

    //*******************************************************************//
    /**
     * the following is implemented using PCA algorithm for the letters
     */

    private void onLetterPCA(Mat mRgba) throws IOException {
        onDatabasePCA();
    }
}
