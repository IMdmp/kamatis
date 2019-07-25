package com.dmp.project.kamatis.version2;

import java.io.IOException;

public interface CameraCaptureController {

    void pauseCamera();
    void resumeCamera();


    void startRecording();
    void startRecording(String videoName);


    void stopRecording();

    void switchCamView();

    void prepareCameraEncoder() throws IOException;
}
