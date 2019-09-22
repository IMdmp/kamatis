package com.dmp.project.kamatis2.version3;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.dmp.project.kamatis.version2.CameraCaptureComponent2;
import com.dmp.project.kamatis.version2.CameraGLView;
import com.dmp.project.kamatis.version2.VideoResolution;
import com.dmp.project.kamatis2.R;

import java.io.IOException;

public class Version3TestActivity extends AppCompatActivity {


    CameraGLView cameraGLView;
    Button btnRecord;
    boolean isRecording;
    EditText etVideoName;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_version3_test);

        cameraGLView = findViewById(R.id.cameraGlView);
        btnRecord = findViewById(R.id.btn_start_recording);
        etVideoName = findViewById(R.id.et_video_name);
        isRecording = false;

        VideoResolution videoResolution = new VideoResolution(1920,1080);

        CameraCaptureComponent2 cameraCaptureComponent=new CameraCaptureComponent2.Builder(cameraGLView,videoResolution)
                                                        .directory(getCacheDir().getPath())
                                                        .directoryFolderName("version3Test")
                                                        .videoType(CameraCaptureComponent2.VIDEO_TYPE_MP4).build();

//        try {
//            cameraCaptureComponent.prepareEncoder("videoTest1");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        btnRecord.setOnClickListener(view -> {
//            if(isRecording){
//                isRecording = false;
//                cameraCaptureComponent.stopRecording();
//                btnRecord.setText("Start Recording");
//            }else{
//                try {
//                    cameraCaptureComponent.prepareEncoder(etVideoName.getText().toString());
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                isRecording = true;
//                cameraCaptureComponent.startRecording();
//                btnRecord.setText("Stop Recording");
//            }
//        });

    }
}
