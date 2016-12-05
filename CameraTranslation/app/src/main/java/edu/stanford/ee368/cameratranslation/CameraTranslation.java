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
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgproc.Imgproc;


import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.content.IntentFilter;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.speech.tts.TextToSpeech;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.google.android.gms.vision.Frame;

public class CameraTranslation extends Activity implements CvCameraViewListener2, View.OnTouchListener {

    /** defining class private variables**/
    // initialize the database
    private DatabaseFile database = new DatabaseFile(this);

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
    private GraphicOverlay<OcrGraphicPlain> mGraphicOverLay;

    private Mat PCAMat;
    private Mat topVectors;
    private List<Mat> databaseDescriptor;

    private RadioGroup radioGroup;
    private static final int ALGO_1 = 0;
    private static final int ALGO_2 = 1;
    private static final int ALGO_3 = 2;
    private static final int ALGO_4 = 3;

    private int algoType = ALGO_1;

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
        mGraphicOverLay = (GraphicOverlay<OcrGraphicPlain>) findViewById(R.id.graphic_overlay);

        /** initializing radio buttons **/
        radioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton radioButton = (RadioButton) findViewById(checkedId);
                String text = radioButton.getText().toString();
                switch (text){
                    case "Algo1":{
                        algoType = ALGO_1;
                        break;
                    }
                    case "Algo2":{
                        algoType = ALGO_2;
                        break;
                    }
                    case "Algo3":{
                        algoType = ALGO_3;
                        break;
                    }
                    case "Algo4":{
                        algoType = ALGO_4;
                        break;
                    }
                    default:
                        break;
                }

                recognizedText = "";
                mGraphicOverLay.clear();
                if(voiceTalker.isSpeaking()){
                    voiceTalker.stop();
                }
            }
        });


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
                    Log.i(TAG, "onClick: text to speech successful: " + recognizedText);
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

        /*
        These few lines are for the PCA algorithm
         */
        PCAMat = new Mat();
        topVectors = new Mat();
        database.computePCAMats(PCAMat, topVectors);
        topVectors = topVectors.t(); // this line is mysterious. But without this line the size of the topVector is wrong
        //Log.e("this", Integer.toString(topVectors.width()) + Integer.toString(topVectors.height()));

        /*
        These few lines are for the SURF detector algorithm
         */
        databaseDescriptor = new ArrayList<>();
        database.computeDescriptor(databaseDescriptor);
    }

    public void onCameraViewStopped(){
        mGray.release();
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame){
        //Log.i(TAG, "got frame");
        double startTime = System.currentTimeMillis();
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();
        if(!isSearchButtonPressed){
            //Log.i(TAG, "search button not pressed");
            return mRgba;
        }
        Log.i(TAG, "search button is pressed");
        switch (algoType){
            /*
                algorithm 1: directly using Google service
            */
            case ALGO_1:{
                onGoogleServiceDirect(mRgba);
                break;
            }
            /*
                algorithm 2: combining MSER detector with Google service
            */
            case ALGO_2:{
                onFeatureDetectorAndGoogleService(mRgba, mGray);
                break;
            }
            /*
                algorithm 3: using PCA
            */
            case ALGO_3: {
                onImagePCA(mGray);
                break;
            }
            /*
                algorithm 4: using FAST feature detector
            */
            case ALGO_4:{
                onFASTDetector(mGray);
                break;
            }
            default:
                break;
        }
        double finishTime = System.currentTimeMillis();
        Log.i(TAG, "run time is " + Double.toString(finishTime - startTime));
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
                String textString = textBlock.getValue();
                textString = textString.replaceAll("\\W", " ").toLowerCase();
                String[] parts = textString.split("\\s+");
                for(String text : parts){
                    Log.i(TAG, text);
                    if(database.hasThisWord(text)){
                        Log.i("OcrDetectorProcessor", "Text detected! " + text);
                        recognizedText = database.getChineseByEnglish(text);
                        OcrGraphicPlain ocrGraphicsPlain = new OcrGraphicPlain(mGraphicOverLay, recognizedText);
                        mGraphicOverLay.add(ocrGraphicsPlain);
                        return;
                    }
                }
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
            //Imgproc.rectangle(mRgba, bottomRight, topLeft, CONTOUR_COLOR, 3);

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
                    String textString = textBlock.getValue();
                    textString = textString.replaceAll("\\W", " ").toLowerCase();
                    String[] parts = textString.split("\\s+");
                    for(String text : parts){
                        if(database.hasThisWord(text)){
                            Log.i("OcrDetectorProcessor", "Text detected! " + text);
                            recognizedText = database.getChineseByEnglish(text);
                            OcrGraphicPlain ocrGraphicsPlain = new OcrGraphicPlain(mGraphicOverLay, recognizedText);
                            mGraphicOverLay.add(ocrGraphicsPlain);
                            return;
                        }
                    }
                }
            }
        }
    }


    //*******************************************************************//
    /**
     * the following is implemented using PCA algorithm for the whole image
     */
    private void onImagePCA(Mat mGray) {
        //onDatabasePCA();
        mGray.convertTo(mGray, CvType.CV_64FC1);
        Imgproc.resize(mGray, mGray, new Size(480, 270));
        mGray = mGray.reshape(0, 1);
        //Core.normalize(mGray, mGray);
        Mat projection = new Mat();
        Core.gemm(topVectors, mGray.t(), 1, new Mat(), 1, projection); // size L * 1
        double maxSimilarity = 0;
        int maxIndex = 0;

        for(int i = 0; i < PCAMat.width(); i++){
            double similarity = Math.abs(projection.dot(PCAMat.col(i)))
                    / (Math.sqrt(projection.dot(projection)) * Math.sqrt(PCAMat.col(i).dot(PCAMat.col(i))));
            //double similarity = Math.abs(projection.dot(PCAMat.col(i)));
            if(similarity > maxSimilarity){
                maxIndex = i;
                maxSimilarity = similarity;
                Log.i(TAG, "similarity" + Double.toString(similarity));
            }
        }
        Log.i(TAG, "the detected word is " + database.getEnglishByIndex(maxIndex));
        recognizedText = database.getChineseByIndex(maxIndex);
        OcrGraphicPlain ocrGraphicsPlain = new OcrGraphicPlain(mGraphicOverLay, recognizedText);
        mGraphicOverLay.add(ocrGraphicsPlain);
    }

    //*******************************************************************//
    /**
     * the following is implemented using FAST detector
     */
    private void onFASTDetector(Mat mGray){
        Imgproc.resize(mGray, mGray, new Size(800, 450));
        FeatureDetector detector = FeatureDetector.create(FeatureDetector.FAST);
        DescriptorExtractor descriptorExtractor = DescriptorExtractor.create(DescriptorExtractor.ORB);

        // detect key points in the input image
        MatOfKeyPoint sceneKeyPoints = new MatOfKeyPoint();
        Mat sceneDescriptor = new Mat();
        detector.detect(mGray, sceneKeyPoints);
        descriptorExtractor.compute(mGray, sceneKeyPoints, sceneDescriptor);

        // match with input images
        List<String> fileNames = database.getEnglishVocabulary();
        int maxMatch = 0;
        int maxIndex = 0;
        for(int i = 0; i < fileNames.size(); i++){
            List<MatOfDMatch> matches = new LinkedList<>();
            Mat objectDescriptor = databaseDescriptor.get(i);
            DescriptorMatcher descriptorMatcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE);
            descriptorMatcher.knnMatch(objectDescriptor, sceneDescriptor, matches, 2);
            double knnRatio = 0.7;
            int thisMatch = 0;
            for(int j = 0; j < matches.size(); j++){
                MatOfDMatch mathOfDMatch = matches.get(j);
                DMatch[] dmatchArray = mathOfDMatch.toArray();
                DMatch m1 = dmatchArray[0];
                DMatch m2 = dmatchArray[1];
                if(m1.distance <= m2.distance * knnRatio){
                    thisMatch += 1;
                }
            }
            if(thisMatch > maxMatch){
                maxMatch = thisMatch;
                maxIndex = i;
            }
            Log.i(TAG, "number of match is " + Integer.toString(maxMatch));
        }
        Log.i(TAG, "the detected word is " + database.getEnglishByIndex(maxIndex));
        recognizedText = database.getChineseByIndex(maxIndex);
        OcrGraphicPlain ocrGraphicsPlain = new OcrGraphicPlain(mGraphicOverLay, recognizedText);
        mGraphicOverLay.add(ocrGraphicsPlain);
    }
}
