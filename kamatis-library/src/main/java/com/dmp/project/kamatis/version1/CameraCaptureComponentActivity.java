package com.dmp.project.kamatis.version1;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.dmp.project.kamatis.version1.controls.RecordingControls;
import com.dmp.project.kamatis.version1.parts.TextureMovieEncoder;
import com.dmp.project.kamatis.version1.utils.CameraSurfaceRenderer;
import com.dmp.project.kamatis.version1.utils.PermissionHelper;
import com.dmp.project.kamatis.version1.utils.VideoUtils;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;

public abstract class CameraCaptureComponentActivity extends AppCompatActivity   implements SurfaceTexture.OnFrameAvailableListener, AdapterView.OnItemSelectedListener, RecordingControls {

    private GLSurfaceView mGLView;
    private CameraSurfaceRenderer mRenderer;
    private Camera mCamera;
    private CameraHandler mCameraHandler;
    //    private boolean mRecordingEnabled;      // controls button state
    private CountDownTimer countDownTimer;
    private int mCameraPreviewWidth, mCameraPreviewHeight;


    private static final String TAG = CameraCaptureComponentActivity.class.toString();

    private static final int CAMERA_ORIENTATION_FRONT =1;
    private static final int CAMERA_ORIENTATION_BACK = 0;
    private static final int CAMERA_ORIENTATION = CAMERA_ORIENTATION_FRONT;


    // this is static so it survives activity restarts
    private static TextureMovieEncoder sVideoEncoder = new TextureMovieEncoder();
    private VideoResolution videoResolution;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        setContentView(R.layout.activity_grafika_capture);
        super.onCreate(savedInstanceState);
        File outputFile = new File(Environment.getExternalStorageDirectory(), provideInitialVideoName());

        // Define a handler that receives camera-control messages from other threads.  All calls
        // to Camera must be made on the same thread.  Note we create this before the renderer
        // thread, so we know the fully-constructed object will be visible.
        mCameraHandler = new CameraHandler(this);
//        mRecordingEnabled = sVideoEncoder.isRecording();

        // Configure the GLSurfaceView.  This will start the Renderer thread, with an
        // appropriate EGL context.
        mGLView = provideGLSurfaceView();//(GLSurfaceView) findViewById(R.id.cameraPreview_surfaceView);
        mGLView.setEGLContextClientVersion(2);     // select GLES 2.0
        videoResolution = providePreferredVideoResolution();
        mRenderer = new CameraSurfaceRenderer(mCameraHandler, sVideoEncoder, outputFile,videoResolution);

        mGLView.setRenderer(mRenderer);
        mGLView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        //Log.d(TAG,"onCreate complete: %s", this);
    }

    public abstract String provideInitialVideoName();
    public abstract  VideoResolution providePreferredVideoResolution();

    protected abstract GLSurfaceView provideGLSurfaceView();

    @Override
    protected void onResume() {
        //Log.d(TAG,"onResume -- acquiring camera");
        super.onResume();
        updateControls();

        if (PermissionHelper.hasCameraPermission(this)) {
            if (mCamera == null) {
                //Log.d(TAG,"WITH PERMISSIONS OPENING CAMERA");
                try {
                    retrieveCameraSizeDimensions();
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
                openCamera(videoResolution.width, videoResolution.height);      // updates mCameraPreviewWidth/Height
            }

        } else {
            //Log.d(TAG,"NO PERMISSIONS");
            PermissionHelper.requestCameraPermission(this, true);
        }

        if(PermissionHelper.hasWriteStoragePermission(this)){

        }else{
            PermissionHelper.requestWriteStoragePermission(this);
        }

        mGLView.onResume();
        mGLView.queueEvent(() -> mRenderer.setCameraPreviewSize(mCameraPreviewWidth, mCameraPreviewHeight));
        //Log.d(TAG,"onResume complete: %s", this);
    }

    private void retrieveCameraSizeDimensions( ) throws CameraAccessException {
        CameraManager manager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);

        String cameraId = manager.getCameraIdList()[CAMERA_ORIENTATION];

        CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);


        StreamConfigurationMap map = characteristics
                .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (map == null) {
            throw new RuntimeException("Cannot get available preview/video sizes");
        }

        Size videoRecordingSize = VideoUtils.chooseVideoSize(map.getOutputSizes(MediaRecorder.class));
        Size cameraPreviewSize = VideoUtils.chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                mCameraPreviewWidth, mCameraPreviewHeight, videoRecordingSize);
        //width?mCameraPreviewWidth?
        //height?mCameraPreviewHeight

        Log.d(TAG,"chosen video recording size: %s" + videoRecordingSize);
        Log.d(TAG,"chosen camera preview size: %s " + cameraPreviewSize);
    }


    @Override
    public void startLegitRecording(){
        mGLView.queueEvent(() -> {
            // notify the renderer that we want to change the encoder's state
            mRenderer.changeRecordingState(true);
        });
        updateControls();    }
    public void stopLegitRecording(){
        mGLView.queueEvent(() -> {
            // notify the renderer that we want to change the encoder's state
            mRenderer.changeRecordingState(false);
        });
        updateControls();
    }

    @Override
    protected void onPause() {
        Log.d(TAG,"onPause -- releasing camera");
        super.onPause();
        releaseCamera();
        mGLView.queueEvent(() -> {
            // Tell the renderer that it's about to be paused so it can clean up.
            mRenderer.notifyPausing();
        });
        mGLView.onPause();
        //Log.d(TAG,"onPause complete");
    }

    @Override
    public void setVideoName(String videoName){
        File outputFile = new File(Environment.getExternalStorageDirectory(), videoName);
        mRenderer.setmOutputFile(outputFile);
    }
    /**
     * Updates the on-screen controls to reflect the current state of the app.
     */
    private void updateControls() {
//        Button toggleRelease = (Button) findViewById(R.id.toggleRecording_button);
//        int id = mRecordingEnabled ?
//                R.string.toggleRecordingOff : R.string.toggleRecordingOn;
//        toggleRelease.setText(id);

        //CheckBox cb = (CheckBox) findViewById(R.id.rebindHack_checkbox);
        //cb.setChecked(TextureRender.sWorkAroundContextProblem);
    }

    @Override
    protected void onDestroy() {
        //Log.d(TAG,"onDestroy");
        super.onDestroy();
        mCameraHandler.invalidateHandler();     // paranoia
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (!PermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(this,
                    "Camera permission is needed to run this application", Toast.LENGTH_LONG).show();
//            PermissionHelper.launchPermissionSettings(this);
            PermissionHelper.requestCameraPermission(this, false);

            finish();
        } else {

            openCamera(videoResolution.width, videoResolution.height);      // updates mCameraPreviewWidth/Height

        }
    }
    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        // The SurfaceTexture uses this to signal the availability of a new frame.  The
        // thread that "owns" the external texture associated with the SurfaceTexture (which,
        // by virtue of the context being shared, *should* be either one) needs to call
        // updateTexImage() to latch the buffer.
        //
        // Once the buffer is latched, the GLSurfaceView thread can signal the encoder thread.
        // This feels backward -- we want recording to be prioritized over rendering -- but
        // since recording is only enabled some of the time it's easier to do it this way.
        //
        // Since GLSurfaceView doesn't establish a Looper, this will *probably* execute on
        // the main UI thread.  Fortunately, requestRender() can be called from any thread,
        // so it doesn't really matter.
//        //Log.d(TAG, "ST onFrameAvailable");
        mGLView.requestRender();

    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }
    /**
     * Opens a camera, and attempts to establish preview mode at the specified width and height.
     * <p>
     * Sets mCameraPreviewWidth and mCameraPreviewHeight to the actual width/height of the preview.
     */
    private void openCamera(int desiredWidth, int desiredHeight) {
        if (mCamera != null) {
            throw new RuntimeException("camera already initialized");
        }

        Camera.CameraInfo info = new Camera.CameraInfo();

        // Try to find a front-facing camera (e.g. for videoconferencing).
        int numCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numCameras; i++) {
            //Log.d(TAG,"cjeck camera: %s",i);
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                mCamera = Camera.open(i);
                break;
            }
        }


        if (mCamera == null) {
            //Log.d(TAG,"No front-facing camera found; opening default");
            mCamera = Camera.open();    // opens first back-facing camera
        }
        if (mCamera == null) {
            throw new RuntimeException("Unable to open camera");
        }

        Camera.Parameters parms = mCamera.getParameters();

//        CameraUtils.choosePreviewSize(parms, desiredWidth, desiredHeight);

        // Give the camera a hint that we're recording video.  This can have a big
        // impact on frame rate.
        parms.setRecordingHint(true);

        // leave the frame rate set to default
        mCamera.setParameters(parms);

        int[] fpsRange = new int[2];
        Camera.Size mCameraPreviewSize = parms.getPreviewSize();
        parms.getPreviewFpsRange(fpsRange);
        String previewFacts = mCameraPreviewSize.width + "x" + mCameraPreviewSize.height;
        if (fpsRange[0] == fpsRange[1]) {
            previewFacts += " @" + (fpsRange[0] / 1000.0) + "fps";
        } else {
            previewFacts += " @[" + (fpsRange[0] / 1000.0) +
                    " - " + (fpsRange[1] / 1000.0) + "] fps";
        }
//        TextView text = (TextView) findViewById(R.id.cameraParams_text);
//        text.setText(previewFacts);
        //Log.d(TAG,"preview facts: "+previewFacts);

        mCameraPreviewWidth = mCameraPreviewSize.width;
        mCameraPreviewHeight = mCameraPreviewSize.height;
        Log.d(TAG,"mCameraPreviewWidth: %s; mCameraPreviewHeight:%s"+ mCameraPreviewWidth + mCameraPreviewHeight);


        AspectFrameLayout layout = provideAspectFrameLayout(); //(AspectFrameLayout) findViewById(R.id.cameraPreview_afl);
        Display display = ((WindowManager)getSystemService(WINDOW_SERVICE)).getDefaultDisplay();

        if(display.getRotation() == Surface.ROTATION_0) {
            mCamera.setDisplayOrientation(90);
            layout.setAspectRatio((double) mCameraPreviewHeight / mCameraPreviewWidth);
        } else if(display.getRotation() == Surface.ROTATION_270) {
            layout.setAspectRatio((double) mCameraPreviewHeight/ mCameraPreviewWidth);
            mCamera.setDisplayOrientation(180);
        } else {
            // Set the preview aspect ratio.
            layout.setAspectRatio((double) mCameraPreviewWidth / mCameraPreviewHeight);
        }
    }

    protected abstract AspectFrameLayout provideAspectFrameLayout();

    /**
     * Stops camera preview, and releases the camera to the system.
     */
    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
            //Log.d(TAG,"releaseCamera -- done");
        }
    }
    /**
     * Handles camera operation requests from other threads.  Necessary because the Camera
     * must only be accessed from one thread.
     * <p>
     * The object is created on the UI thread, and all handlers run there.  Messages are
     * sent from other threads, using sendMessage().
     */
    public static class CameraHandler extends Handler {
        public static final int MSG_SET_SURFACE_TEXTURE = 0;

        // Weak reference to the Activity; only access this from the UI thread.
        private WeakReference<CameraCaptureComponentActivity> mWeakActivity;

        public CameraHandler(CameraCaptureComponentActivity activity) {
            mWeakActivity = new WeakReference<CameraCaptureComponentActivity >(activity);
        }

        /**
         * Drop the reference to the activity.  Useful as a paranoid measure to ensure that
         * attempts to access a stale Activity through a handler are caught.
         */
        public void invalidateHandler() {
            mWeakActivity.clear();
        }

        @Override  // runs on UI thread
        public void handleMessage(Message inputMessage) {
            int what = inputMessage.what;
            //Log.d(TAG,"CameraHandler [" + this + "]: what=" + what);

            CameraCaptureComponentActivity activity = mWeakActivity.get();
            if (activity == null) {
                Log.w(TAG,"CameraHandler.handleMessage: activity is null");
                return;
            }

            switch (what) {
                case MSG_SET_SURFACE_TEXTURE:
                    activity.handleSetSurfaceTexture((SurfaceTexture) inputMessage.obj);
                    break;
                default:
                    throw new RuntimeException("unknown msg " + what);
            }
        }
    }


    /**
     * Connects the SurfaceTexture to the Camera preview output, and starts the preview.
     */
    private void handleSetSurfaceTexture(SurfaceTexture st) {
        st.setOnFrameAvailableListener(this);
        try {
            mCamera.setPreviewTexture(st);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        mCamera.startPreview();
    }
}
