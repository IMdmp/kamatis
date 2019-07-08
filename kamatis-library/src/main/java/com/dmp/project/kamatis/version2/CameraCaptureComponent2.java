package com.dmp.project.kamatis.version2;

import android.util.Log;

import com.dmp.project.kamatis.BuildConfig;
import com.dmp.project.kamatis.version1.VideoResolution;
import com.dmp.project.kamatis.version1.encoder.MediaAudioEncoder;
import com.dmp.project.kamatis.version1.encoder.MediaEncoder;
import com.dmp.project.kamatis.version1.encoder.MediaMuxerWrapper;
import com.dmp.project.kamatis.version1.encoder.MediaVideoEncoder;

import java.io.IOException;

public class CameraCaptureComponent2 {

    private static final String TAG = "CameraCaptureComponent2";

    private static final boolean DEBUG = BuildConfig.DEBUG;
    private final boolean frontAndBackCamEnabled;
    private final CameraGLView cameraGlViewPreviewDisplay;
    private MediaMuxerWrapper mMuxer;
    private int cameraId = 1;
    private String videoName= "test";
    private static final String VIDEO_TYPE_MP4 = ".mp4";
    private final String  directory;

    public CameraCaptureComponent2(CameraGLView cameraGlViewPreviewDisplay, VideoResolution videoResolution,String directory) {
        Log.d(TAG,"directory set to: "+directory);
        this.cameraGlViewPreviewDisplay = cameraGlViewPreviewDisplay;

        this.cameraGlViewPreviewDisplay.setVideoSize(videoResolution.getHeight(), videoResolution.getWidth());
        frontAndBackCamEnabled = cameraGlViewPreviewDisplay.isFrontAndBackCamEnabled();
        this.directory = directory;
    }

    private final CameraCaptureController cameraCaptureController = new CameraCaptureController() {
        @Override
        public void pauseCamera() {
            cameraGlViewPreviewDisplay.onPause();

        }


        @Override
        public void resumeCamera() {
            cameraGlViewPreviewDisplay.onResume();

        }

        @Override
        public void startRecording() {
            startRecordingCamera();
        }

        @Override
        public void stopRecording() {
            stopRecordingCamera();
        }

        @Override
        public void switchCamView() {
            if (frontAndBackCamEnabled) {
                if (cameraId == 1) cameraId = 0;
                else if (cameraId == 0) cameraId = 1;
                cameraGlViewPreviewDisplay.resetPreview(cameraId);
            }else{
                Log.e(TAG,"cannot switch cam view");
            }
        }
    };

    /**
     * callback methods from encoder
     */
    private final MediaEncoder.MediaEncoderListener mMediaEncoderListener = new MediaEncoder.MediaEncoderListener() {

        @Override
        public void onPrepared(final MediaEncoder encoder) {
            if (BuildConfig.DEBUG) Log.v(TAG, "onPrepared:encoder=" + encoder);
            if (encoder instanceof MediaVideoEncoder)
                cameraGlViewPreviewDisplay.setVideoEncoder((MediaVideoEncoder) encoder);
        }

        @Override
        public void onStopped(final MediaEncoder encoder) {
            if (DEBUG) Log.v(TAG, "onStopped:encoder=" + encoder);
            if (encoder instanceof MediaVideoEncoder)
                cameraGlViewPreviewDisplay.setVideoEncoder(null);
        }
    };

    private final  MediaEncoder.TimingListener mTimingListener = new MediaEncoder.TimingListener() {

        @Override
        public void onTimingStarted(MediaEncoder encoder, long startTime) {
            if (encoder instanceof MediaAudioEncoder){
                Log.d("Timing", "StartTime: "+startTime);
//                startingTime = startTime;
//                showProgressTime();
            }
        }

        @Override
        public void onTimingStopped(MediaEncoder encoder, long endTime) {
            if (encoder instanceof MediaAudioEncoder){
                Log.d("Timing", "EndTime: "+endTime);
//                legacyTime = totalTimePassedMs;
//                timer.cancel();
//                timer.purge();
            }
        }
    };



    public CameraCaptureController getCameraCaptureController() {
        return cameraCaptureController;
    }

    /**
     * start resorcing
     * This is a sample project and call this on UI thread to avoid being complicated
     * but basically this should be called on private thread because prepareing
     * of encoder is heavy work
     */
    private void startRecordingCamera() {
        if (DEBUG) Log.v(TAG, "startRecording:");
        try {
//            mRecordButton.setColorFilter(0xffff0000);    // turn red
//            mMuxer = new MediaMuxerWrapper(".mp4");    // if you record audio only, ".m4a" is also OK.
            mMuxer  = new MediaMuxerWrapper(videoName,directory,VIDEO_TYPE_MP4);

            if (true) {
                // for video capturing
                new MediaVideoEncoder(mMuxer, mMediaEncoderListener,
                        mTimingListener,
                        cameraGlViewPreviewDisplay.getVideoWidth(),
                        cameraGlViewPreviewDisplay.getVideoHeight());
            }
            if (true) {
                // for audio capturing
                new MediaAudioEncoder(mMuxer, mMediaEncoderListener, mTimingListener);
            }
            mMuxer.prepare();
            mMuxer.startRecording();
        } catch (final IOException e) {
//            mRecordButton.setColorFilter(0);
            Log.e(TAG, "startCapture:", e);
        }
    }

    /**
     * request stop recording
     */
    private void stopRecordingCamera() {
        if (DEBUG) Log.v(TAG, "stopRecording:mMuxer=" + mMuxer);
//        mRecordButton.setColorFilter(0);    // return to default color
        if (mMuxer != null) {
            mMuxer.stopRecording();
//            parts.add(new File(mMuxer.getOutputPath(), ""));
        }
        mMuxer = null;
        // you should not wait here
    }
}
