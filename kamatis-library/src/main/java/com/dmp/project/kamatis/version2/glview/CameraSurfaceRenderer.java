package com.dmp.project.kamatis.version2.glview;

import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLException;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Debug;
import android.os.Environment;
import android.util.Log;

import com.dmp.project.kamatis.BuildConfig;
import com.dmp.project.kamatis.version2.CameraGLView;
import com.dmp.project.kamatis.version2.encoder.MediaVideoEncoder;
import com.dmp.project.kamatis.version2.gles.GLDrawer2D;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.IntBuffer;
import java.util.Calendar;
import java.util.Objects;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * GLSurfaceViewのRenderer
 */
public final class CameraSurfaceRenderer
        implements GLSurfaceView.Renderer,
        SurfaceTexture.OnFrameAvailableListener {
    private static final boolean DEBUG = BuildConfig.DEBUG;    // API >= 11
    private static final String TAG = CameraSurfaceRenderer.class.getName();

    private final WeakReference<CameraGLView> mWeakParent;
    private final float[] mStMatrix = new float[16];
    private final float[] mMvpMatrix = new float[16];
    public SurfaceTexture mSTexture;	// API >= 11
    public int hTex;
    private GLDrawer2D mDrawer;
    public MediaVideoEncoder mVideoEncoder;
    private volatile boolean requesrUpdateTex = false;
    private boolean flip = true;
    private int mVideoHeight;
    private int mVideoWidth;
    public boolean enableScreenshotBitmap ;



    public CameraSurfaceRenderer(final CameraGLView parent) {
        if (DEBUG) Log.v(TAG, "CameraSurfaceRenderer:");
        mWeakParent = new WeakReference<CameraGLView>(parent);
        Matrix.setIdentityM(mMvpMatrix, 0);
        this.enableScreenshotBitmap = false;
    }

    @Override
    public void onSurfaceCreated(final GL10 unused, final EGLConfig config) {
        if (DEBUG) Log.v(TAG, "onSurfaceCreated:");
        // This renderer required OES_EGL_image_external extension
        final String extensions = GLES20.glGetString(GLES20.GL_EXTENSIONS);	// API >= 8
//			if (DEBUG) Log.i(TAG, "onSurfaceCreated:Gl extensions: " + extensions);
        if (!extensions.contains("OES_EGL_image_external"))
            throw new RuntimeException("This system does not support OES_EGL_image_external.");
        // create textur ID
        hTex = GLDrawer2D.initTex();
        // create SurfaceTexture with texture ID.
        mSTexture = new SurfaceTexture(hTex);
        mSTexture.setOnFrameAvailableListener(this);
        // clear screen with yellow color so that you can see rendering rectangle
        GLES20.glClearColor(1.0f, 1.0f, 0.0f, 1.0f);
        final CameraGLView parent = mWeakParent.get();
        if (parent != null) {
            parent.mHasSurface = true;
        }
        // create object for preview display
        mDrawer = new GLDrawer2D();
        mDrawer.setMatrix(mMvpMatrix, 0);
    }

    @Override
    public void onSurfaceChanged(final GL10 unused, final int width, final int height) {
        if (DEBUG) Log.v(TAG, String.format("onSurfaceChanged:(%d,%d)", width, height));
        // if at least with or height is zero, initialization of this view is still progress.
        if ((width == 0) || (height == 0)) return;
        updateViewport();
        final CameraGLView parent = mWeakParent.get();
        if (parent != null) {
            parent.startPreview();
        }
    }

    /**
     * when GLSurface context is soon destroyed
     */
    public void onSurfaceDestroyed() {
        if (DEBUG) Log.v(TAG, "onSurfaceDestroyed:");
        if (mDrawer != null) {
            mDrawer.release();
            mDrawer = null;
        }
        if (mSTexture != null) {
            mSTexture.release();
            mSTexture = null;
        }
        GLDrawer2D.deleteTex(hTex);
    }

    public final void updateViewport() {
        final CameraGLView parent = mWeakParent.get();
        if (parent != null) {
            final int view_width = parent.getWidth();
            final int view_height = parent.getHeight();
            GLES20.glViewport(0, 0, view_width, view_height);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            final double video_width = parent.mVideoWidth;
            final double video_height = parent.mVideoHeight;
            if (video_width == 0 || video_height == 0) return;
            Matrix.setIdentityM(mMvpMatrix, 0);
            final double view_aspect = view_width / (double)view_height;
            Log.i(TAG, String.format("view(%d,%d)%f,video(%1.0f,%1.0f)", view_width, view_height, view_aspect, video_width, video_height));

            final double req = video_width / video_height;
            int x, y;
            int width, height;
            if (view_aspect > req) {
                // if view is wider than camera image, calc width of drawing area based on view height
                y = 0;
                height = view_height;
                width = (int) (req * view_height);
                x = (view_width - width) / 2;
            } else {
                // if view is higher than camera image, calc height of drawing area based on view width
                x = 0;
                width = view_width;
                height = (int) (view_width / req);
                y = (view_height - height) / 2;
            }
            x=0;
            y=0;
            // set viewport to draw keeping aspect ration of camera image
            if (DEBUG) Log.v(TAG, String.format("xy(%d,%d),size(%d,%d)", x, y, width, height));
            GLES20.glViewport(x, y,  (int) view_width,  (int) view_height);

            if (mDrawer != null)
                mDrawer.setMatrix(mMvpMatrix, 0);
        }
    }

    /**
     * drawing to GLSurface
     * we set renderMode to GLSurfaceView.RENDERMODE_WHEN_DIRTY,
     * this method is only called when #requestRender is called(= when texture is required to update)
     * if you don't set RENDERMODE_WHEN_DIRTY, this method is called at maximum 60fps
     */
    @Override
    public void onDrawFrame(final GL10 unused) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        if (requesrUpdateTex) {
            requesrUpdateTex = false;
            // update texture(came from camera)
            mSTexture.updateTexImage();
            // get texture matrix
            mSTexture.getTransformMatrix(mStMatrix);
        }

        if(mDrawer!=null){
            // draw to preview screen
            mDrawer.draw(hTex, mStMatrix);
        }
        flip = !flip;
        if (flip) {	// ~30fps
            synchronized (this) {
                if (mVideoEncoder != null) {
                    // notify to capturing thread that the camera frame is available.
//						mVideoEncoder.frameAvailableSoon(mStMatrix);
                    mVideoEncoder.frameAvailableSoon(mStMatrix, mMvpMatrix);



                }
            }
        }
        if(enableScreenshotBitmap){

            saveToStorage(Objects.requireNonNull(createBitmapFromGLSurface(0, 0, mVideoWidth, mVideoHeight, unused)));
            Log.d(TAG,"SCREENSHOTTED");
            enableScreenshotBitmap  = false;
        }
    }

    private void saveToStorage(Bitmap bitmapFromGLSurface) {

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmapFromGLSurface.compress(Bitmap.CompressFormat.JPEG, 90, bos);
        byte[] bitmapdata = bos.toByteArray();
        ByteArrayInputStream fis = new ByteArrayInputStream(bitmapdata);

        final Calendar c=Calendar.getInstance();
        long mytimestamp=c.getTimeInMillis();
        String timeStamp=String.valueOf(mytimestamp);
        String myfile="cameraglview"+timeStamp+".jpeg";

        File dir_image = new File(Environment.getExternalStorageDirectory() + File.separator +
                "cameraglviewshot" + File.separator + "image");
        dir_image.mkdirs();

        Log.d(TAG,"DIRECTORY OUTPUT: " +  dir_image.getAbsolutePath());

        try {
            File tmpFile = new File(dir_image,myfile);
            FileOutputStream fos = new FileOutputStream(tmpFile);

            byte[] buf = new byte[1024];
            int len;
            while ((len = fis.read(buf)) > 0) {
                fos.write(buf, 0, len);
            }
            fis.close();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Bitmap createBitmapFromGLSurface(int x, int y, int w, int h, GL10 gl)
            throws OutOfMemoryError {
        int bitmapBuffer[] = new int[w * h];
        int bitmapSource[] = new int[w * h];
        IntBuffer intBuffer = IntBuffer.wrap(bitmapBuffer);
        intBuffer.position(0);

        try {
            gl.glReadPixels(x, y, w, h, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, intBuffer);
            int offset1, offset2;
            for (int i = 0; i < h; i++) {
                offset1 = i * w;
                offset2 = (h - i - 1) * w;
                for (int j = 0; j < w; j++) {
                    int texturePixel = bitmapBuffer[offset1 + j];
                    int blue = (texturePixel >> 16) & 0xff;
                    int red = (texturePixel << 16) & 0x00ff0000;
                    int pixel = (texturePixel & 0xff00ff00) | red | blue;
                    bitmapSource[offset2 + j] = pixel;
                }
            }
        } catch (GLException e) {
            return null;
        }
        return Bitmap.createBitmap(bitmapSource, w, h, Bitmap.Config.ARGB_8888);
    }


    @Override
    public void onFrameAvailable(final SurfaceTexture st) {
        requesrUpdateTex = true;
//			final CameraGLView parent = mWeakParent.get();
//			if (parent != null)
//				parent.requestRender();
    }

    public void setSizeParameters(int width, int height) {
        this.mVideoWidth = width;
        this.mVideoHeight = height;
    }
}
