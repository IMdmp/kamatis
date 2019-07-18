package com.dmp.project.kamatis2;

public class MainActivity {

//
//    GLSurfaceView glSurfaceView;
//    AspectFrameLayout aspectFrameLayout;
//    Button startRecordingButton;
//    public   static int VIDEO_RESOLUTION_WIDTH  = 1080;
//    public static int VIDEO_RESOLUTION_HEIGHT  = 1920;
//
//    boolean isRecording;
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        setContentView(R.layout.activity_main);
//
//        isRecording = false;
//
//        aspectFrameLayout = findViewById(R.id.aspectFrameLayout);
//        glSurfaceView = findViewById(R.id.glSurfaceView);
//        startRecordingButton = findViewById(R.id.btn_start_recording);
//
//        startRecordingButton.setOnClickListener(view -> {
//            if(isRecording){
//                stopLegitRecording();
//
//                startRecordingButton.setText("Start Recording");
//                isRecording=!isRecording;
//
//            }else{
//                startLegitRecording();
//                startRecordingButton.setText("Stop Recording");
//                isRecording=!isRecording;
//            }
//        });
//
//        Timber.d("did it work?? %s", Tester.didItWork());
//        super.onCreate(savedInstanceState);
//
//
//    }
//
//    private void askPermission() {
//        Dexter.withActivity(this)
//                .withPermissions(
//                        Manifest.permission.CAMERA,
//                        Manifest.permission.READ_EXTERNAL_STORAGE,
//                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
//                        Manifest.permission.RECORD_AUDIO
//                ).withListener(new MultiplePermissionsListener() {
//            @Override public void onPermissionsChecked(MultiplePermissionsReport report) {/* ... */
//
//
//            }
//
//            @Override public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {/* ... */}
//        }).check();
//    }
//
//    @Override
//    public VideoResolution providePreferredVideoResolution() {
//        return new VideoResolution(VIDEO_RESOLUTION_WIDTH,VIDEO_RESOLUTION_HEIGHT);
//    }
//
//    @Override
//    public String provideInitialVideoName() {
//        return "initial.mp4";
//    }
//
//    @Override
//    protected GLSurfaceView provideGLSurfaceView() {
//        return glSurfaceView;
//    }
//
//    @Override
//    protected AspectFrameLayout provideAspectFrameLayout() {
//        return aspectFrameLayout;
//    }

}

