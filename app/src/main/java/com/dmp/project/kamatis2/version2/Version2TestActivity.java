package com.dmp.project.kamatis2.version2;

import android.Manifest;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.dmp.project.kamatis.version1.VideoResolution;
import com.dmp.project.kamatis.version2.CameraCaptureComponent2;
import com.dmp.project.kamatis.version2.CameraCaptureController;
import com.dmp.project.kamatis.version2.CameraGLView;
import com.dmp.project.kamatis2.R;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import timber.log.Timber;

public class Version2TestActivity extends AppCompatActivity implements RecordingMechanism4.CountdownListener {
    private CameraGLView cameraGlViewPreviewDisplay;
    private Button startRecordingButton;
    private boolean isRecording;
    private CameraCaptureController cameraCaptureController;
    private Queue<RecordingMechanism4> recordingMechanismQueue = new LinkedList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Timber.d("assing persmisson.");
        setContentView(R.layout.activity_version2test);
        cameraGlViewPreviewDisplay = findViewById(R.id.cameraGlView);
        startRecordingButton = findViewById(R.id.btn_start_recording);

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
        VideoResolution videoResolution = new VideoResolution(1920, 1080);

        CameraCaptureComponent2 cameraCaptureComponent2 = new CameraCaptureComponent2(cameraGlViewPreviewDisplay, videoResolution, getCacheDir().getPath());
        cameraCaptureController = cameraCaptureComponent2.getCameraCaptureController();

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
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraCaptureController.pauseCamera();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraCaptureController.resumeCamera();
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
