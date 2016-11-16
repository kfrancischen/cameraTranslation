package edu.stanford.ee368.cameratranslation;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class CameraTranslation extends AppCompatActivity implements CvCameraViewListener2 {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onPause(){

    }

    @Override
    public void onResume(){

    }

    publid void onDestroy(){

    }

    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame){

    }
}
