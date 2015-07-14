package edu.bupt.camera;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import edu.bupt.mccam.R;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff.Mode;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

public class CameraActivity extends Activity implements OnClickListener, SurfaceHolder.Callback {
	
	private static final int MEDIA_TYPE_IMAGE = 1;
	private static int rotationDegrees = 0;
	private boolean onTouchFocus = true;
	private File newFile = null;
	private static File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
			Environment.DIRECTORY_PICTURES), "MCCam");
		
	private Camera mCamera;
	private SurfaceView cameraSurfaceView;
	private SurfaceView rectSurfaceView;
	private SurfaceHolder mHolder;
	private SurfaceHolder rectHolder;
	private Button bt_cam;
	private Button bt_gallery;
	
	private MediaScannerConnection conn;
	
	private PictureCallback mPicture = new PictureCallback(){
		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			camera.stopPreview();
			new SaveImageTask().execute(data);
			camera.startPreview();
		}	
	};
	
	private SurfaceHolder.Callback rectCallback = new SurfaceHolder.Callback(){
		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			Canvas canvas = holder.lockCanvas();
			drawRect(canvas, 0.625, 0.4375, 0.125, Color.RED);
			holder.unlockCanvasAndPost(canvas);
		}
		
		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width,
				int height) {		
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
		}
	};
	
	private AutoFocusCallback mAFCallback = new AutoFocusCallback() {
		@Override
		public void onAutoFocus(boolean success, Camera camera) {
			if(success) {
				Log.d("AutoFocus", "success!");
				if(!onTouchFocus) {
					try {
						camera.takePicture(null, null, mPicture);	
					} catch(Exception e) {
						e.printStackTrace();
					}
				}
			}else {
				Log.d("AutoFocus", "failed!");
			}
			bt_cam.setEnabled(true);
		}	
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_camera);
		
		bt_cam = (Button)findViewById(R.id.bt_cam);
		bt_gallery = (Button)findViewById(R.id.bt_gallery);
		bt_cam.setOnClickListener(this);
		bt_gallery.setOnClickListener(this);
		
		cameraSurfaceView = (SurfaceView)findViewById(R.id.camera_preview);
		rectSurfaceView = (SurfaceView)findViewById(R.id.rect_surfaceview);
		
		mCamera = getCameraInstance();
		// Install a SurfaceHolder.Callback so we get notified when the
		// underlying surface is created and destroyed.
		mHolder = cameraSurfaceView.getHolder();
		mHolder.addCallback(this);
		// deprecated setting, but required on Android versions prior to 3.0
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		
		rectHolder = rectSurfaceView.getHolder();
		rectHolder.setFormat(PixelFormat.TRANSPARENT);
		rectSurfaceView.setZOrderOnTop(true);
		rectHolder.addCallback(rectCallback);
		rectHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}
	
	public static Camera getCameraInstance(){
		Camera c = null;
		try {
			c = Camera.open();
		}catch(Exception e){
			
		}
		return c;
	}
	
	private void drawRect(Canvas canvas, double widthRatio, double heightRatio, double marginTopRatio, int color) {
		if (canvas != null) {
			DisplayMetrics dm = getScreenSize();
			Log.d("ScreenSize", "w:" + dm.widthPixels + "h:" + dm.heightPixels );
			canvas.drawColor(0, Mode.CLEAR);
			Paint paint = new Paint();
			paint.setStyle(Paint.Style.STROKE);
			paint.setColor(color);
			paint.setStrokeWidth(3);
			canvas.drawRect((float)(dm.widthPixels*(1-widthRatio)/2), (float)(dm.heightPixels*marginTopRatio), 
				(float)(dm.widthPixels*(1+widthRatio)/2), (float)(dm.heightPixels*(heightRatio + marginTopRatio)), paint);
		}
	}
	
	private DisplayMetrics getScreenSize(){
		DisplayMetrics dm = new DisplayMetrics();
		dm = getResources().getDisplayMetrics();
		return dm;
	}
	
	private class SaveImageTask extends AsyncTask<byte[], Void, File> {
		@Override
		protected File doInBackground(byte[]... params) {
			return saveImage(params[0]);
		}
		
		@Override
		protected void onPostExecute(File result) {
			if (result != null) {
				try {
					ExifInterface exif = new ExifInterface(result.getAbsolutePath());
					Log.d("orientation", " " + exif.getAttribute(ExifInterface.TAG_ORIENTATION));
					exif.setAttribute(ExifInterface.TAG_ORIENTATION, "" + ExifInterface.ORIENTATION_ROTATE_90);
					exif.saveAttributes();
				} catch (IOException e) {
					e.printStackTrace();
				}
				Toast.makeText(getApplicationContext(), "Picture saved", Toast.LENGTH_LONG).show();
				newFile = result;
				scanImages(result.getAbsolutePath(), false);
			}
		}
		
		private File saveImage(byte[] data) {
			File file = getOutputMediaFile(MEDIA_TYPE_IMAGE);
			if(file == null){
				Log.d("pictureCallback", "Error creating media file, check storage permission");
				return null;
			}
			Log.d("FileName", " " + file.getName());
			//Bitmap img = rotateImage(params[0], rotationDegrees);
			FileOutputStream fos;
			try {
				fos = new FileOutputStream(file);
				//img.compress(Bitmap.CompressFormat.JPEG, 90, fos);
				fos.write(data);
				fos.flush();
				fos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return file;
		}
	}
	
	/*this rotation method will cause EXIF header loss*/
	private static Bitmap rotateImage(byte[] data, float degrees) {
		Bitmap src, dst;
		src = BitmapFactory.decodeByteArray(data, 0, data.length);
		Matrix m = new Matrix();
		m.reset();
		m.postRotate(degrees);
		dst = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), m, true);
		if (!src.isRecycled()){
			src.recycle();
			src = null;
			System.gc();
		}
		return dst;
	}
	
	private static File getOutputMediaFile(int type){
		if(!mediaStorageDir.exists()){
			if(!mediaStorageDir.mkdirs()){
				Log.d("getOutputMediaFile", "failed to create directory");
				return null;
			}
		}
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		File mediaFile;
		if(type == MEDIA_TYPE_IMAGE){
			mediaFile = new File(mediaStorageDir.getPath() + 
					File.separator + "IMG_" + timeStamp + ".jpg");
		} else {
			return null;
		}
		return mediaFile;	
	}
	
	private void releaseCamera(){
		if(mCamera != null){
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode==KeyEvent.KEYCODE_BACK){
			releaseCamera();
			this.finish();
			return true;
		}
		return false;
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.bt_cam:
			onTouchFocus = false;
			bt_cam.setEnabled(false);
			mCamera.autoFocus(mAFCallback);
			break;
		case R.id.bt_gallery:
			String[] filelist = mediaStorageDir.list();
			String s = (newFile != null) ? 
				newFile.getAbsolutePath() : (filelist.length == 0) ?
					null : mediaStorageDir.getAbsolutePath() + "/" + filelist[filelist.length-1];
			Log.d("scanPath", " " + s);
			if (s != null) {
				scanImages(s, true);
			} else {
				Toast.makeText(getApplicationContext(), "Nothing", Toast.LENGTH_LONG).show();
			}
			break;
		}
	}
	
	private void scanImages(final String scanPath, final boolean open) {
		conn = new MediaScannerConnection(this, 
			new MediaScannerConnectionClient() {
				@Override
				public void onScanCompleted(String path, Uri uri) {
					try {
						if (uri != null && open) {
							Intent intent = new Intent();
							intent.setAction(Intent.ACTION_VIEW);
							intent.setDataAndType(uri, "image/jpeg");
							startActivity(intent);
						}
					} finally {
						conn.disconnect();
						conn = null;
					}
				}
				@Override
				public void onMediaScannerConnected() {
					try {
						conn.scanFile(scanPath, "image/jpeg");
					}catch(Exception e) {
						e.printStackTrace();
					}
				}
			});
		
		conn.connect();
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		onTouchFocus = true;
		if (mCamera != null) {
			mCamera.autoFocus(mAFCallback);
		}
		return true;
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if(mCamera == null) {
			mCamera = getCameraInstance();
		}
		// The Surface has been created, now tell the camera where to draw the preview.
		try {
			mCamera.setPreviewDisplay(holder);
			mCamera.startPreview();
		}catch(IOException e) {
			Log.d("surfaceCreated", "error setting camera preview" + e.getMessage());
		}	
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		if(holder.getSurface() == null){
			return;
		}	
		try {
			// stop preview before making changes
			mCamera.stopPreview();
			
			mCamera.setPreviewDisplay(holder);
			setCameraParameters();
			setCameraDisplayOrientation(this, 0, mCamera);
			
			mCamera.startPreview();
		}catch(Exception e) {
			Log.d("surfaceChanged", e.getMessage());
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		releaseCamera();
	}
	
	public static void setCameraDisplayOrientation(Activity activity, int cameraId, Camera camera) {
		Camera.CameraInfo info = new Camera.CameraInfo();
		Camera.getCameraInfo(cameraId, info);
		int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
		int degrees = 0;
		switch (rotation) {
		case Surface.ROTATION_0:
			degrees = 0;
			break;
		case Surface.ROTATION_90:
			degrees = 90;
			break;
		case Surface.ROTATION_180:
			degrees = 180;
			break;
		case Surface.ROTATION_270:
			degrees = 270;
			break;
		}
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			rotationDegrees = (info.orientation + degrees) % 360;
			rotationDegrees = (360 - rotationDegrees) % 360; // compensate the mirror
		} else {
			rotationDegrees = (info.orientation - degrees + 360) % 360;
		}
		camera.setDisplayOrientation(rotationDegrees);
	}
	
	private void setCameraParameters(){
		if (mCamera != null){
			Camera.Parameters params = mCamera.getParameters();
			List<String> focusModes = params.getSupportedFocusModes();
			for (String s:focusModes){
				Log.d("FocusMode", s);
			}
			if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
				params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
			}
			
			DisplayMetrics dm = getScreenSize();
			List<Camera.Size> previewSizes = params.getSupportedPreviewSizes();
			Camera.Size mPreviewSize = getOptimalPreviewSize(previewSizes, dm.widthPixels, dm.heightPixels);
			Log.d("OptimalPreviewSize", mPreviewSize.width + "X" + mPreviewSize.height);
			params.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
			
			double screenRatio = (double) dm.heightPixels/dm.widthPixels;
			double previewRatio = (double) Math.max(mPreviewSize.height,mPreviewSize.width)/
					Math.min(mPreviewSize.height,mPreviewSize.width);
			int width, height;
			if (screenRatio > previewRatio){
				height = dm.heightPixels;
				width = (int) (height/previewRatio);
			} else {
				width = dm.widthPixels;
				height = (int) (width*previewRatio);
			}
			Log.d("setFrameLayout", "w:" + width + "h:" + height);
			FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(width, height);
			lp.gravity = Gravity.CENTER;
			cameraSurfaceView.setLayoutParams(lp);
			
			List<Camera.Size> picSizes = params.getSupportedPictureSizes();
			Camera.Size mSize = picSizes.get(0);
			for(Camera.Size s:picSizes){
				if(s.width > mSize.width){
					mSize = s;
				}
			}
			params.setPictureSize(mSize.width, mSize.height);
			mCamera.setParameters(params);
		}
	}
	
	private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
		final double ASPECT_TOLERANCE = 0.1;
		double targetRatio = (double) w / h;

		if (sizes == null)
			return null;

		Camera.Size optimalSize = null;
		double minDiff = Double.MAX_VALUE;

		int targetHeight = h;

		for (Camera.Size size : sizes) {
			double ratio = (double) size.width / size.height;
			if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
				continue;
			if (Math.abs(size.height - targetHeight) < minDiff) {
				optimalSize = size;
				minDiff = Math.abs(size.height - targetHeight);
			}
		}

		if (optimalSize == null) {
			minDiff = Double.MAX_VALUE;
			for (Camera.Size size : sizes) {
				if (Math.abs(size.height - targetHeight) < minDiff) {
					optimalSize = size;
					minDiff = Math.abs(size.height - targetHeight);
				}
			}
		}
		return optimalSize;
	}
	
	@Override
	protected void onPause() {
		releaseCamera();
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mCamera == null){
			mCamera = getCameraInstance();
		}
	}

}
