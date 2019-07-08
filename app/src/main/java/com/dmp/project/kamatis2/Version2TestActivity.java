package com.dmp.project.kamatis2;

import android.Manifest;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.dmp.project.kamatis.version1.VideoResolution;
import com.dmp.project.kamatis.version2.CameraCaptureComponent2;
import com.dmp.project.kamatis.version2.CameraCaptureController;
import com.dmp.project.kamatis.version2.CameraGLView;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.util.List;

import timber.log.Timber;

public class Version2TestActivity extends AppCompatActivity {
    private CameraGLView cameraGlViewPreviewDisplay;
    private Button startRecordingButton;
    private boolean isRecording;
    private CameraCaptureController cameraCaptureController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Timber.d("assing persmisson.");
        setContentView(R.layout.activity_version2test);
        cameraGlViewPreviewDisplay = findViewById(R.id.cameraGlView);
        startRecordingButton = findViewById(R.id.btn_start_recording);

        isRecording = false;
        startCamera();

    }


    private void startCamera() {
        VideoResolution videoResolution = new VideoResolution(1080, 1920);

        CameraCaptureComponent2 cameraCaptureComponent2 = new CameraCaptureComponent2(cameraGlViewPreviewDisplay, videoResolution, getCacheDir().getPath().toString());
        cameraCaptureController = cameraCaptureComponent2.getCameraCaptureController();

        startRecordingButton.setOnClickListener(view -> {
            if (isRecording) {
                cameraCaptureController.stopRecording();
                isRecording = false;
                startRecordingButton.setText("Start Recording");
            } else {
                cameraCaptureController.startRecording();
                isRecording = true;
                startRecordingButton.setText("Stop Recording");
            }
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
}
