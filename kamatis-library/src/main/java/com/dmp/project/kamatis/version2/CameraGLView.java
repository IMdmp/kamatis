package com.dmp.project.kamatis.version2;
/*
 * FlexCam
 * Sample project to cature audio and video from internal mic/camera and save as MPEG4 file.
 *
 * Copyright (c) 2014-2015 saki t_saki@serenegiant.com
 * Copyright (c) 2017 Cédric Portmann cedric.portmann@gmail.com
 *
 * File name: CameraGLView.java
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * All files in the folder are under this Apache License, Version 2.0.
*/

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.opengl.GLException;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import com.dmp.project.kamatis.BuildConfig;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Sub class of GLSurfaceView to display camera preview and write video frame to capturing surface
 */
public final class CameraGLView extends GLSurfaceView {

	private static final boolean DEBUG = BuildConfig.DEBUG;
	private static final String TAG = "CameraGLView";
	private static final int SCALE_CROP_CENTER = 3;
	static private int mCameraId = 1;
	private final CameraSurfaceRenderer mRenderer;
	CameraSetting cameraSetting = new CameraSetting();
	private int mScaleMode = SCALE_CROP_CENTER;
	private boolean mHasSurface;
	private CameraHandler mCameraHandler = null;
	private int mVideoWidth, mVideoHeight;
	private int mRotation;

	//

	public CameraGLView(final Context context) {
		this(context, null, 0);
	}

	public CameraGLView(final Context context, final AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public CameraGLView(final Context context, final AttributeSet attrs, final int defStyle) {
		super(context, attrs);
		if (DEBUG) Log.v(TAG, "CameraGLView:");
		mRenderer = new CameraSurfaceRenderer(this);
		setEGLContextClientVersion(2);	// GLES 2.0, API >= 8
		setRenderer(mRenderer);
/*		// the frequency of refreshing of camera preview is at most 15 fps
		// and RENDERMODE_WHEN_DIRTY is better to reduce power consumption */
	}

	@Override
	public void onResume() {
		if (DEBUG) Log.v(TAG, "onResume:");
		super.onResume();
		if (mHasSurface) {
			if (mCameraHandler == null) {
				if (DEBUG) Log.v(TAG, "surface already exist");
				startPreview();
			}
		}
	}

	@Override
	public void onPause() {
		if (DEBUG) Log.v(TAG, "onPause:");
		if (mCameraHandler != null) {
			// just request stop prviewing
			mCameraHandler.stopPreview(false);
		}
		super.onPause();
	}

	public void setVideoSize(final int width, final int height) {


		Log.d(TAG,"set video size. w" +width + "h: "+height);
		if ((mRotation % 180) == 0) {
			mVideoWidth = width;
			mVideoHeight = height;
		} else {
			mVideoWidth = height;
			mVideoHeight = width;
		}
		queueEvent(new Runnable() {
			@Override
			public void run() {
				mRenderer.updateViewport();
			}
		});

		mRenderer.setSizeParameters(width,height);
	}

	public int getVideoWidth() {
		return mVideoWidth;
	}

	public int getVideoHeight() {
		return mVideoHeight;
	}

	public CameraSurfaceRenderer getmRenderer() {
		return mRenderer;
	}

	public SurfaceTexture getSurfaceTexture() {
		if (DEBUG) Log.v(TAG, "getSurfaceTexture:");
		return mRenderer != null ? mRenderer.mSTexture : null;
	}

	public void resetPreview(int mCameraId) {
		CameraGLView.mCameraId = mCameraId;
		stopPreview();
		startPreview();
	}

	@Override
	public void surfaceDestroyed(final SurfaceHolder holder) {
		if (DEBUG) Log.v(TAG, "surfaceDestroyed:");
		if (mCameraHandler != null) {
			// wait for finish previewing here
			// otherwise camera try to display on un-exist Surface and some error will occure
			mCameraHandler.stopPreview(true);
		}
		mCameraHandler = null;
		mHasSurface = false;
		mRenderer.onSurfaceDestroyed();
		super.surfaceDestroyed(holder);
	}

	public void setVideoEncoder(final MediaVideoEncoder encoder) {
		if (DEBUG) Log.v(TAG, "setVideoEncoder:tex_id=" + mRenderer.hTex + ",encoder=" + encoder);
		queueEvent(() -> {
			synchronized (mRenderer) {
				if (encoder != null) {
					encoder.setEglContext(EGL14.eglGetCurrentContext(), mRenderer.hTex);
				}
				mRenderer.mVideoEncoder = encoder;
			}
		});
	}

//********************************************************************************
//********************************************************************************
	private synchronized void startPreview() {
		if (mCameraHandler == null) {
			final CameraThread thread = new CameraThread(this);
			thread.start();
			mCameraHandler = thread.getHandler();
		}
		mCameraHandler.startPreview(getVideoWidth(), getVideoHeight()/*width, height*/);
	}

	private synchronized void stopPreview() {
		if (DEBUG) Log.v(TAG, "surfaceDestroyed:");
		if (mCameraHandler != null) {
			// wait for finish previewing here
			// otherwise camera try to display on un-exist Surface and some error will occure
			mCameraHandler.stopPreview(true);
		}
	}

	public boolean isFrontAndBackCamEnabled() {
		Camera mCamera = Camera.open(0);
		Camera.Parameters params = mCamera.getParameters();
		cameraSetting.backCameraSizeList = params.getSupportedPreviewSizes();
		mCamera.release();
		mCamera = Camera.open(1);
		params = mCamera.getParameters();
		cameraSetting.frontCameraSizeList = params.getSupportedPreviewSizes();
		mCamera.release();

		List<Camera.Size> rawRecordingSize = cameraSetting.getAllCommonSizesList();
		return rawRecordingSize.size() > 0;
	}




	/**
	 * GLSurfaceViewのRenderer
	 */
	public static final class CameraSurfaceRenderer
		implements GLSurfaceView.Renderer,
					SurfaceTexture.OnFrameAvailableListener {	// API >= 11

		private final WeakReference<CameraGLView> mWeakParent;
		private final float[] mStMatrix = new float[16];
		private final float[] mMvpMatrix = new float[16];
		private SurfaceTexture mSTexture;	// API >= 11
		private int hTex;
		private GLDrawer2D mDrawer;
		private MediaVideoEncoder mVideoEncoder;
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

		private final void updateViewport() {
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
			// draw to preview screen
			mDrawer.draw(hTex, mStMatrix);
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

	/**
	 * Handler class for asynchronous camera operation
	 */
	private static final class CameraHandler extends Handler {
		private static final int MSG_PREVIEW_START = 1;
		private static final int MSG_PREVIEW_STOP = 2;
		private CameraThread mThread;

		public CameraHandler(final CameraThread thread) {
			mThread = thread;
		}

		public void startPreview(final int width, final int height) {
			sendMessage(obtainMessage(MSG_PREVIEW_START, width, height));
		}

		/**
		 * request to stop camera preview
		 * @param needWait need to wait for stopping camera preview
		 */
		public void stopPreview(final boolean needWait) {
			synchronized (this) {
				sendEmptyMessage(MSG_PREVIEW_STOP);
				if (needWait && mThread.mIsRunning) {
					try {
						if (DEBUG) Log.d(TAG, "wait for terminating of camera thread");
						wait();
					} catch (final InterruptedException e) {
					}
				}
			}
		}

		/**
		 * message handler for camera thread
		 */
		@Override
		public void handleMessage(final Message msg) {
			switch (msg.what) {
			case MSG_PREVIEW_START:
				mThread.startPreview(msg.arg1, msg.arg2);
				break;
			case MSG_PREVIEW_STOP:
				mThread.stopPreview();
				synchronized (this) {
					notifyAll();
				}
				Looper.myLooper().quit();
				mThread = null;
				break;
			default:
				throw new RuntimeException("unknown message:what=" + msg.what);
			}
		}
	}



	/**
	 * Thread for asynchronous operation of camera preview
	 */
	private static final class CameraThread extends Thread {
    	private final Object mReadyFence = new Object();
    	private final WeakReference<CameraGLView> mWeakParent;
    	private CameraHandler mHandler;
    	private volatile boolean mIsRunning = false;
		private Camera mCamera;
		private boolean mIsFrontFace;


		public CameraThread(final CameraGLView parent) {
			super("Camera thread");
    		mWeakParent = new WeakReference<CameraGLView>(parent);
    	}

		private static Camera.Size getClosestSupportedSize(List<Camera.Size> supportedSizes, final int requestedWidth, final int requestedHeight) {
			return Collections.min(supportedSizes, new Comparator<Camera.Size>() {

				private int diff(final Camera.Size size) {
					return Math.abs(requestedWidth - size.width) + Math.abs(requestedHeight - size.height);
				}

				@Override
				public int compare(final Camera.Size lhs, final Camera.Size rhs) {
					return diff(lhs) - diff(rhs);
				}
			});

		}

    	public CameraHandler getHandler() {
            synchronized (mReadyFence) {
            	try {
            		mReadyFence.wait();
            	} catch (final InterruptedException e) {
                }
            }
            return mHandler;
    	}

    	/**
    	 * message loop
    	 * prepare Looper and create Handler for this thread
    	 */
		@Override
		public void run() {
            if (DEBUG) Log.d(TAG, "Camera thread start");
            Looper.prepare();
            synchronized (mReadyFence) {
                mHandler = new CameraHandler(this);
                mIsRunning = true;
                mReadyFence.notify();
            }
            Looper.loop();
            if (DEBUG) Log.d(TAG, "Camera thread finish");
            synchronized (mReadyFence) {
                mHandler = null;
                mIsRunning = false;
            }
		}

		/**
		 * start camera preview
		 * @param width
		 * @param height
		 */
		private final void startPreview(final int width, final int height) {
			if (DEBUG) Log.v(TAG, "startPreview: w: " +width + " h: " + height );
			final CameraGLView parent = mWeakParent.get();
			if ((parent != null) && (mCamera == null)) {
				// This is a sample project so just use 0 as camera ID.
				// it is better to selecting camera is available
				try {
					mCamera = Camera.open(mCameraId);
					final Camera.Parameters params = mCamera.getParameters();
					final List<String> focusModes = params.getSupportedFocusModes();
					if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
						params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
					} else if(focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
						params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
					} else {
						if (DEBUG) Log.i(TAG, "Camera does not support autofocus");
					}
					// let's try fastest frame rate. You will get near 60fps, but your device become hot.
					final List<int[]> supportedFpsRange = params.getSupportedPreviewFpsRange();
//					final int n = supportedFpsRange != null ? supportedFpsRange.size() : 0;
//					int[] range;
//					for (int i = 0; i < n; i++) {
//						range = supportedFpsRange.get(i);
//						Log.i(TAG, String.format("supportedFpsRange(%d)=(%d,%d)", i, range[0], range[1]));
//					}
					final int[] max_fps = supportedFpsRange.get(supportedFpsRange.size() - 1);
					Log.i(TAG, String.format("fps:%d-%d", max_fps[0], max_fps[1]));
					params.setPreviewFpsRange(max_fps[0], max_fps[1]);
					params.setRecordingHint(true);

					final Camera.Size recordingSize = getClosestSupportedSize(
							parent.cameraSetting.getAllCommonSizesList(), width, height);
//
					params.setPreviewSize(recordingSize.width, recordingSize.height);
					Log.d(TAG,"setting parms2.0: w"+recordingSize.width + ";h:"+recordingSize.height);

//					params.setPreviewSize(width, height);

//					// request closest picture size for an aspect ratio issue on Nexus7
//					final Camera.Size pictureSize = getClosestSupportedSize(
//							parent.cameraSetting.getAllCommonSizesList(), width, height);
//
//					params.setPictureSize(pictureSize.width, pictureSize.height);

					// rotate camera preview according to the device orientation
					setRotation(params);


//					Log.d(TAG,"setting parms: w"+params.getPreviewSize().width + ";h:"+params.getPreviewSize().height);
					mCamera.setParameters(params);

					// get the actual preview size
					final Camera.Size previewSize = mCamera.getParameters().getPreviewSize();


					Log.i(TAG, String.format("previewSize(%d, %d)", previewSize.width, previewSize.height));
					// adjust view size with keeping the aspect ration of camera preview.
					// here is not a UI thread and we should request parent view to execute.
					parent.post(new Runnable() {
						@Override
						public void run() {
							parent.setVideoSize(previewSize.width, previewSize.height);
						}
					});
					final SurfaceTexture st = parent.getSurfaceTexture();
					st.setDefaultBufferSize(previewSize.width, previewSize.height);
					mCamera.setPreviewTexture(st);
				} catch (final IOException e) {
					Log.e(TAG, "startPreview:", e);
					if (mCamera != null) {
						mCamera.release();
						mCamera = null;
					}
				} catch (final RuntimeException e) {
					Log.e(TAG, "startPreview:", e);
					if (mCamera != null) {
						mCamera.release();
						mCamera = null;
					}
				}
				if (mCamera != null) {
					// start camera preview display
					mCamera.startPreview();
				}
			}
		}

		/**
		 * stop camera preview
		 */
		private void stopPreview() {
			if (DEBUG) Log.v(TAG, "stopPreview:");
			if (mCamera != null) {
				mCamera.stopPreview();
		        mCamera.release();
		        mCamera = null;
			}
			final CameraGLView parent = mWeakParent.get();
			if (parent == null) return;
			parent.mCameraHandler = null;
		}

		/**
		 * rotate preview screen according to the device orientation
		 * @param params
		 */
		private final void 	setRotation(final Camera.Parameters params) {
			if (DEBUG) Log.v(TAG, "setRotation:");
			final CameraGLView parent = mWeakParent.get();
			if (parent == null) return;

			final Display display = ((WindowManager)parent.getContext()
				.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
			final int rotation = display.getRotation();
			int degrees = 0;
			switch (rotation) {
				case Surface.ROTATION_0: degrees = 0; break;
				case Surface.ROTATION_90: degrees = 90; break;
				case Surface.ROTATION_180: degrees = 180; break;
				case Surface.ROTATION_270: degrees = 270; break;
			}
			// get whether the camera is front camera or back camera
			final Camera.CameraInfo info =
					new android.hardware.Camera.CameraInfo();
				android.hardware.Camera.getCameraInfo(mCameraId, info);
			mIsFrontFace = (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT);
			if (mIsFrontFace) {	// front camera
				degrees = (info.orientation + degrees) % 360;
				degrees = (360 - degrees) % 360;  // reverse
			} else {  // back camera
				degrees = (info.orientation - degrees + 360) % 360;
			}
			// apply rotation setting
			mCamera.setDisplayOrientation(degrees);



			parent.mRotation = degrees;
			Log.d(TAG,"degress: "+ degrees);

			// XXX This method fails to call and camera stops working on some devices.
//			params.setRotation(degrees);
		}

	}
}
