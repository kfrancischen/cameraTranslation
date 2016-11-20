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
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.imgproc.Imgproc;


import android.content.Intent;
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
import java.util.Locale;

import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.google.android.gms.vision.Frame;

public class CameraTranslation extends Activity implements CvCameraViewListener2, View.OnTouchListener {

    /* defining class private variables*/
    // initialize the database
    private DatabaseFile database = new DatabaseFile();

    // initialize UI activities
    private static final String TAG = "translation::Activity";
    private TranslationCameraView mOpenCVCameraView;
    private ImageButton voiceButton;
    private ImageButton searchButton;
    private TextToSpeech voiceTalker;
    private Mat mRgba;
    private Mat mGray;
    private String recognizedText = "";
    private String preText = "";
    private TextRecognizer textRecognizer;
    private GraphicOverlay<OcrGraphic> mGraphicOverLay;

    /* defining class for call back*/
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
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //mOpenCVCameraView = new TranslationCameraView(this, -1);
        //setContentView(mOpenCVCameraView);
        setContentView(R.layout.camera_translation_view);
        /* initializing camera view */
        mOpenCVCameraView = (TranslationCameraView) findViewById(R.id.camera_surface_view);
        mOpenCVCameraView.setVisibility(TranslationCameraView.VISIBLE);
        mOpenCVCameraView.setCvCameraViewListener(this);

        /* initializing graphicOverLay */
        mGraphicOverLay = (GraphicOverlay<OcrGraphic>) findViewById(R.id.graphic_overlay);

        /* initializing voice talker */
        voiceButton = (ImageButton) findViewById(R.id.voice_button);
        voiceTalker = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener(){
            @Override
            public void onInit(int status){
                if(status != TextToSpeech.ERROR){
                    voiceTalker.setLanguage(Locale.US);
                }
            }
        });
        /* initializing search start button */
        searchButton = (ImageButton) findViewById(R.id.search_button);
        /* initializing voice button */
        voiceButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                // TODO: here we can put a function for voicing
                /* just an example of hello world */
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
            Log.e(TAG, "Detector dependencies are not avaible");
            // check storage
            IntentFilter lowStorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = registerReceiver(null, lowStorageFilter) != null;
            if (hasLowStorage) {
                Log.e(TAG, "Low storage!");
            }

        }

    }


    public boolean onTouch(View view, MotionEvent event){
        mOpenCVCameraView.focusOnTouch(event);
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
            Log.e(TAG, "OpenCV library found inside package. Using it!");
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
    }

    public void onCameraViewStopped(){
        mGray.release();
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame){

        Log.i(TAG, "got frame");
        mRgba = inputFrame.rgba();
        //mGray = inputFrame.gray();

        /* the following is implemented using Google service */
        //mGraphicOverLay.clear();
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
            }

        }
        if(!recognizedText.equals(preText)){
            mGraphicOverLay.clear();
            for(int i = 0; i < items.size(); i++){
                OcrGraphic graphic = new OcrGraphic(mGraphicOverLay, items.valueAt(i));
                mGraphicOverLay.add(graphic);
            }
            preText = recognizedText;
        }
        return mRgba;
    }

}
