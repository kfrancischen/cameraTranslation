package edu.stanford.ee368.cameratranslation;
/**
 * Created by francischen on 11/17/16.
 */

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
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


import android.media.Image;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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

import java.io.IOException;
import java.util.Locale;


public class CameraTranslation extends Activity implements CvCameraViewListener2, View.OnTouchListener {

    /* defining class private variables*/
    private static final String TAG = "translation::Activity";
    private TranslationCameraView mOpenCVCameraView;
    private ImageButton voiceButton;
    private TextToSpeech voiceTalker;
    private Mat mRgba;
    private Mat mGray;

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

        mOpenCVCameraView = (TranslationCameraView) findViewById(R.id.camera_surface_view);
        mOpenCVCameraView.setVisibility(TranslationCameraView.VISIBLE);
        mOpenCVCameraView.setCvCameraViewListener(this);

        voiceButton = (ImageButton) findViewById(R.id.voice_button);
        voiceTalker = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener(){
            @Override
            public void onInit(int status){
                if(status != TextToSpeech.ERROR){
                    voiceTalker.setLanguage(Locale.US);
                }
            }
        });

        voiceButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                // TODO: here we can put a function for voicing
                /* just an example of hello world */
                String str = "hello world";
                voiceTalker.speak(str, TextToSpeech.QUEUE_FLUSH, null, "hello world");

            }
        });
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
    }

    @Override
    public void onResume(){
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy(){
        super.onDestroy();
        if(mOpenCVCameraView != null){
            mOpenCVCameraView.disableView();
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
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();
        // TODO: text recognition
        return mRgba;
    }

}
