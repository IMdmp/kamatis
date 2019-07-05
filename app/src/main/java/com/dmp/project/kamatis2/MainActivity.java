package com.dmp.project.kamatis2;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.widget.Button;

import com.dmp.project.kamatis.version1.AspectFrameLayout;
import com.dmp.project.kamatis.version1.CameraCaptureComponentActivity;
import com.dmp.project.kamatis.version1.VideoResolution;
import com.dmp.project.kamatis.version1.Tester;

import timber.log.Timber;

public class MainActivity extends CameraCaptureComponentActivity {


    GLSurfaceView glSurfaceView;
    AspectFrameLayout aspectFrameLayout;
    Button startRecordingButton;
    public   static int VIDEO_RESOLUTION_WIDTH  = 1080;
    public static int VIDEO_RESOLUTION_HEIGHT  = 1920;

    boolean isRecording;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_main);

        isRecording = false;


        aspectFrameLayout = findViewById(R.id.aspectFrameLayout);
        glSurfaceView = findViewById(R.id.glSurfaceView);
        startRecordingButton = findViewById(R.id.btn_start_recording);

        startRecordingButton.setOnClickListener(view -> {
            if(isRecording){
                stopLegitRecording();

                startRecordingButton.setText("Start Recording");
                isRecording=!isRecording;

            }else{
                startLegitRecording();
                startRecordingButton.setText("Stop Recording");
                isRecording=!isRecording;
            }
        });

        Timber.d("did it work?? %s", Tester.didItWork());
        super.onCreate(savedInstanceState);


    }

    @Override
    public VideoResolution providePreferredVideoResolution() {
        return new VideoResolution(VIDEO_RESOLUTION_WIDTH,VIDEO_RESOLUTION_HEIGHT);
    }

    @Override
    public String provideInitialVideoName() {
        return "initial.mp4";
    }

    @Override
    protected GLSurfaceView provideGLSurfaceView() {
        return glSurfaceView;
    }

    @Override
    protected AspectFrameLayout provideAspectFrameLayout() {
        return aspectFrameLayout;
    }

}

