package com.dmp.project.kamatis.version2;

import android.graphics.Camera;
import android.os.Environment;
import android.util.Log;

import com.dmp.project.kamatis.BuildConfig;
import com.dmp.project.kamatis.version2.encoder.MediaAudioEncoder;
import com.dmp.project.kamatis.version2.encoder.MediaEncoder;
import com.dmp.project.kamatis.version2.encoder.MediaMuxerWrapper;
import com.dmp.project.kamatis.version2.encoder.MediaVideoEncoder;

import java.io.IOException;

public class CameraCaptureComponent2 {
    private static final String TAG = "CameraCaptureComponent2";
    private static final boolean DEBUG = BuildConfig.DEBUG;
    private final boolean frontAndBackCamEnabled;
    private final CameraGLView cameraGlViewPreviewDisplay;
    private String directoryFolderName;
    public static final String VIDEO_TYPE_MP4 = ".mp4";
    private String directory;
    private String videoName;
    private VideoResolution videoResolution;
    private MediaMuxerWrapper mMuxer;
    private int cameraId = 1;

    public CameraGLView.CameraSurfaceRenderer getCameraSurfaceRenderer() {
        return cameraSurfaceRenderer;
    }

    private CameraGLView.CameraSurfaceRenderer cameraSurfaceRenderer;


    private CameraCaptureComponent2(Builder builder) {
        this.directory = null;
        this.directoryFolderName = null;
        this.cameraGlViewPreviewDisplay = builder.cameraGLView;
        this.directory = builder.directory;
        this.directoryFolderName = builder.directoryFolderName;
        this.videoResolution = builder.videoResolution;

        frontAndBackCamEnabled = cameraGlViewPreviewDisplay.isFrontAndBackCamEnabled();
        cameraSurfaceRenderer = cameraGlViewPreviewDisplay.getmRenderer();
        this.cameraGlViewPreviewDisplay.setVideoSize(videoResolution.getWidth(), videoResolution.getHeight());


        if(directory == null){
            directory = Environment.getExternalStorageDirectory().getPath();
        }
        if(directoryFolderName ==null){
            directoryFolderName = "CameraCaptureFolder";
        }
    }

    public static class Builder {
        private CameraGLView cameraGLView; //important
        private String directory;
        private String videoType;
        private String directoryFolderName;
        private VideoResolution videoResolution;


        public Builder(CameraGLView cameraGLView, VideoResolution videoResolution) {
            this.cameraGLView = cameraGLView;
            this.videoResolution = videoResolution;
        }



        public Builder directory(String directory) {
            this.directory = directory;

            return this;
        }

        public Builder videoType(String videoType) {
            this.videoType = videoType;

            return this;
        }

        public Builder directoryFolderName(String directoryFolderName) {
            this.directoryFolderName = directoryFolderName;

            return this;
        }

        public CameraCaptureComponent2 build() {
            return new CameraCaptureComponent2(this);

        }


    }


    public void pauseCamera(){
        cameraGlViewPreviewDisplay.onPause();
    }

    public void resumeCamera(){
        cameraGlViewPreviewDisplay.onResume();
    }

    public void prepareEncoder(String videoName) throws IOException {
        mMuxer = new MediaMuxerWrapper(videoName, directory, VIDEO_TYPE_MP4);

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
        mMuxer.setDirectoryFolderName(directoryFolderName);
        mMuxer.prepare();
    }

    public void startRecording(){
            startRecordingCamera();
    }

    public void stopRecording(){
             stopRecordingCamera();
    }
    public void switchCamView() {
            if (frontAndBackCamEnabled) {
                if (cameraId == 1) cameraId = 0;
                else if (cameraId == 0) cameraId = 1;
                cameraGlViewPreviewDisplay.resetPreview(cameraId);
            }else{
                Log.e(TAG,"cannot switch cam view");
            }
        }

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

    private final MediaEncoder.TimingListener mTimingListener = new MediaEncoder.TimingListener() {

        @Override
        public void onTimingStarted(MediaEncoder encoder, long startTime) {
            if (encoder instanceof MediaAudioEncoder) {
                Log.d("Timing", "StartTime: " + startTime);
//                startingTime = startTime;
//                showProgressTime();
            }
        }

        @Override
        public void onTimingStopped(MediaEncoder encoder, long endTime) {
            if (encoder instanceof MediaAudioEncoder) {
                Log.d("Timing", "EndTime: " + endTime);
//                legacyTime = totalTimePassedMs;
//                timer.cancel();
//                timer.purge();
            }
        }
    };



    public void setDirectoryFolderName(String directoryFolderName) {
        this.directoryFolderName = directoryFolderName;
    }

    /**
     * start resourcing
     * This is a sample project and call this on UI thread to avoid being complicated
     * but basically this should be called on private thread because preparing
     * of encoder is heavy work
     */
    private void startRecordingCamera() {
        if (DEBUG) Log.v(TAG, "startRecording:");

        if(mMuxer == null){
            throw new RuntimeException("Encoder not set. have you called prepareEncoder? ");
        }else{
            mMuxer.startRecording();
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
