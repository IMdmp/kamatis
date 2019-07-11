package com.dmp.project.kamatis2.version2;

import android.os.CountDownTimer;

import com.dmp.project.kamatis.version2.CameraCaptureController;

import timber.log.Timber;

public class RecordingMechanism4 {

    private CameraCaptureController cameraCaptureController;
    private CountdownListener countdownListener;
    private String videoName;
    private CountDownTimer countDownTimer = new CountDownTimer(3000,1000) {
        @Override
        public void onTick(long l) {
            Timber.d("tick... "+l);
        }

        @Override
        public void onFinish() {
            cameraCaptureController.stopRecording();
            countdownListener.onFinish();

        }
    };
    public RecordingMechanism4(CameraCaptureController cameraCaptureController,CountdownListener countdownListener,String videoName) {
        this.cameraCaptureController = cameraCaptureController;
        this.countdownListener = countdownListener;
        this.videoName = videoName;
    }


    public void startTimer(){
        countDownTimer.start();
        cameraCaptureController.startRecording(videoName);
    }




public interface CountdownListener{
        void onFinish();
    }
}
