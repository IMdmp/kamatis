package com.dmp.project.kamatis2.version2;

import android.app.ActionBar;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;


import com.dmp.project.kamatis.version2.CameraCaptureComponent2;
import com.dmp.project.kamatis.version2.CameraCaptureController;
import com.dmp.project.kamatis.version2.CameraGLView;
import com.dmp.project.kamatis.version2.VideoResolution;
import com.dmp.project.kamatis2.R;
import java.util.LinkedList;
import java.util.Queue;

import timber.log.Timber;

public class Version2TestActivity extends AppCompatActivity implements RecordingMechanism4.CountdownListener {
    private CameraGLView cameraGlViewPreviewDisplay;
    private Button startRecordingButton,takeScreenshotButton;
    private boolean isRecording;
    private CameraCaptureController cameraCaptureController;
    private Queue<RecordingMechanism4> recordingMechanismQueue = new LinkedList<>();
    private  CameraGLView.CameraSurfaceRenderer cameraSurfaceRenderer;
    private ImageView ivImage;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View decorView = getWindow().getDecorView();
// Hide the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);


        Timber.d("assing persmisson.");
        setContentView(R.layout.activity_version2test);
        cameraGlViewPreviewDisplay = findViewById(R.id.cameraGlView);
        startRecordingButton = findViewById(R.id.btn_start_recording);
        takeScreenshotButton = findViewById(R.id.btn_take_screenshot);
        ivImage = findViewById(R.id.iv_image);
        isRecording = false;
        startCamera();

        
        
        initializeCountdowns();


    }

    private void initializeCountdowns() {
        recordingMechanismQueue.add(new RecordingMechanism4(cameraCaptureController,this,"hello"));
        recordingMechanismQueue.add(new RecordingMechanism4(cameraCaptureController, this,"hi"));
        recordingMechanismQueue.add(new RecordingMechanism4(cameraCaptureController,this,"hey"));


    }


    private void startCamera() {
        Timber.d("starting camera.");
//        VideoResolution videoResolution = new VideoResolution(1080, 1920);
        VideoResolution videoResolution = new VideoResolution(1920, 1080);

        CameraCaptureComponent2 cameraCaptureComponent2 = new CameraCaptureComponent2(cameraGlViewPreviewDisplay, videoResolution, getCacheDir().getPath());
        cameraCaptureController = cameraCaptureComponent2.getCameraCaptureController();
        cameraCaptureComponent2.setDirectoryFolderName("Kamatis Test");
        cameraSurfaceRenderer = cameraCaptureComponent2.getCameraSurfaceRenderer();
        startRecordingButton.setOnClickListener(view -> {
            if(!isRecording){
                startRecordingButton.setText("recording...");
                recordingMechanismQueue.remove().startTimer();
                isRecording= true;
            }
//            if (isRecording) {
//                cameraCaptureController.stopRecording();
//                isRecording = false;?.
//                startRecordingButton.setText("Start Recording");
//            } else {
//                cameraCaptureController.startRecording();
//                isRecording = true;
//                startRecordingButton.setText("Stop Recording");
//            }
        });


        takeScreenshotButton.setOnClickListener(view -> {
            Timber.d("attempting to take a screenshot...");
            cameraSurfaceRenderer.enableScreenshotBitmap = true;

        });
    }

    @Override
    protected void onPause() {
        cameraCaptureController.pauseCamera();
        super.onPause();
    }

    @Override
    protected void onResume() {
        cameraCaptureController.resumeCamera();
        super.onResume();
    }

    @Override
    public void onFinish() {
        if(recordingMechanismQueue.size()>0){

            Timber.d("restarting");
            RecordingMechanism4 recordingMechanism4=  recordingMechanismQueue.remove();
            recordingMechanism4.startTimer();
        }else{
            Timber.d("finished recording!!");
        }
    }
}
