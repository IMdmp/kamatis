package com.dmp.project.kamatis.version2;

public interface CameraCaptureController {

    void pauseCamera();
    void resumeCamera();


    void startRecording();
    void stopRecording();

    void switchCamView();
}
