package edu.bupt.camera;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;

import edu.bupt.camera.CameraActivity.PicRealtimeUpload;
import edu.bupt.mccam.MainActivity;
import edu.bupt.mccam.R;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.PreviewCallback;
import android.media.CamcorderProfile;
import android.media.ExifInterface;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

public class VideoActivity extends Activity implements OnClickListener,SurfaceHolder.Callback,PreviewCallback{
	
	private static String TAG = "VideoActivity";
	private static final float NS2S = 1.0f / 1000000000.0f;
	public static final int MEDIA_TYPE_VIDEO = 2;
	private static final int MEDIA_TYPE_IMAGE = 1;
	
	private static final String packageName = "edu.bupt.framelifting";
	private static final String className = "edu.bupt.framelifting.MainActivity";
	
	private float timestamp;
	private float angle[] = new float[3];
	private ProgressBar progressBar;
	private int rotateProgress = 0;
	private static int captureNum = 24;
	
	private float anglex, angley, anglez;
	private volatile int i = 1;
	private Handler handler;
	
	private Camera mCamera;
	private SurfaceView mSurfaceView;
	private SurfaceHolder mSurfaceHolder;
	private MediaRecorder mMediaRecorder;
	private Button bt_capture;
	
	private boolean isRecording;
	private boolean flag = false;
	
	private static String filePath;
	private static String phoneMode;
	private static String phoneMake;
	private static float focalLength;
	
	private RealFrameTask mRealFrameTask;
	private boolean isPreview =false;
	private boolean touchFlag = true;
//	private ScanThread scan;
	private static volatile boolean isScanThreadAlive;
	
	private volatile int num = 1;
	private Queue<ByteArrayOutputStream> saveQueue;
	private Queue<String> uploadPathQueue;
//	private SaveThread mSaveThread;
//	private UploadThread mUploadThread;
	
	private Timer mTimer;
	private TimerTask mTimerTask;
	private Chronometer mChronometer;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_auto_capture);
		mChronometer = (Chronometer) findViewById(R.id.tv_clock);
		progressBar=(ProgressBar)findViewById(R.id.progressBar_record_progress);
		progressBar.setVisibility(View.GONE);
		bt_capture=(Button) findViewById(R.id.start);
		bt_capture.setOnClickListener(this);
		mCamera = getCameraInstance();
		mSurfaceView = (SurfaceView) findViewById(R.id.SurfaceView);
		isScanThreadAlive = true;
		mTimer = new Timer();
		mTimerTask = new TimerTask() {
			
			@Override
			public void run() {
				if(mCamera != null && isPreview){
					mCamera.setOneShotPreviewCallback(VideoActivity.this);
					Log.d(TAG, "scan");
				}
			}
		};
		
		registerForContextMenu(mChronometer);
		
		mSurfaceView.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(touchFlag){
					mCamera.autoFocus(mAFCallback);
				}
			}
		});
		mSurfaceHolder=mSurfaceView.getHolder();
		mSurfaceHolder.addCallback(this);
		mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	
		phoneMode = Build.MODEL;
		phoneMake = Build.BRAND;
		saveQueue = new LinkedList<ByteArrayOutputStream>();
		uploadPathQueue = new LinkedList<String>();
//		scan = new ScanThread();
//		mSaveThread = new SaveThread();
//		mUploadThread = new UploadThread();
		handler=new Handler(){
			@Override
			public void handleMessage(Message msg){
				
				if(msg.what==2){
					Toast.makeText(VideoActivity.this,"Capture Complete!",Toast.LENGTH_SHORT).show();
					
					Intent intent = new Intent(VideoActivity.this, FrameLifting.class);
					intent.putExtra("filePath", filePath);
					intent.putExtra("phoneMode", phoneMode);
					intent.putExtra("phoneMake", phoneMake);
					intent.putExtra("focalLength", focalLength);
//					startService(intent);
					VideoActivity.this.finish();
				}
			}
		};
		
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		if(holder.getSurface()==null){
			return;
		}
		mCamera.stopPreview();
		try {
			mCamera.setPreviewDisplay(holder);
			setCameraDisplayOrientation(this, 0, mCamera);
			Parameters params = mCamera.getParameters();
			params.setPreviewSize(MainActivity.previewWidth, MainActivity.previewHeight);
			mCamera.setParameters(params);
			mCamera.startPreview();
		} catch (IOException e) {
			e.printStackTrace();
			Log.d("Surface Changed", e.getMessage());
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		
		if(mCamera==null){
			mCamera=getCameraInstance();
		}
		try {
			mCamera.setPreviewDisplay(holder);
			mCamera.startPreview();
//			mSaveThread.start();
//			if(MainActivity.isAutoUpload){
//				mUploadThread.start();
//			}
			
		} catch (Exception e) {
			e.printStackTrace();
			Log.d("surfaceCreated", "error setting camera preview" + e.getMessage());
		}
		
	}

	private Camera getCameraInstance() {
		Camera c=null;
		try {
			c=Camera.open();
		} catch (Exception e) {
			e.printStackTrace();
			Log.d("Open Camera", "failed");
		}
		return c;
	}
	
	private boolean prepareVideoRecorder(){
		if (mCamera == null) {
			mCamera=getCameraInstance();
		}
		mMediaRecorder=new MediaRecorder();
		// Step 1: Unlock and set camera to MediaRecorder
//		setCameraParameters();
		mCamera.unlock();
		mMediaRecorder.setCamera(mCamera);
		// Step 2: Set sources
		mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
		mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		// Step 3: Set a CamcorderProfile (requires API Level 8 or higher instead of setting format and encoding)
		mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
		// Step 4: Set output file
		mMediaRecorder.setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO).toString());
		mMediaRecorder.setVideoSize(MainActivity.previewWidth, MainActivity.previewHeight);
		// Step 5: Set the preview output
		mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());
		 // Step 6: Prepare configured MediaRecorder
		
		try {
			mMediaRecorder.prepare();
		} catch (IllegalStateException e) {
			e.printStackTrace();
			Log.d("TAG", "IllegalStateException preparing MediaRecorder: " + e.getMessage());
			releaseMediaRecorder();
		} catch (IOException e) {
			Log.d("TAG", "IOException preparing MediaRecorder: " + e.getMessage());
			e.printStackTrace();
			releaseMediaRecorder();
		}	
		return true;	
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
		int rotationDegrees;
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			rotationDegrees = (info.orientation + degrees) % 360;
			rotationDegrees = (360 - rotationDegrees) % 360; // compensate the mirror
		} else {
			rotationDegrees = (info.orientation - degrees + 360) % 360;
		}
		camera.setDisplayOrientation(rotationDegrees);
	}
	
	private void releaseMediaRecorder(){
		if(mMediaRecorder!=null){
			mMediaRecorder.reset();// clear recorder configuration
			mMediaRecorder.release();// release the recorder object
			mMediaRecorder=null;
			mCamera.lock();// lock camera for later use
		}
	}
	
	private void releaseCamera(){
		if(mCamera!=null){
			mCamera.release();// release the camera for other applications
			mCamera=null;
		}
	}
	private static File getOutputMediaFile(int type){
		File mediaStorageDir=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)+"/"+"RecordVideo");
		if(!mediaStorageDir.exists()){
			if(!mediaStorageDir.mkdirs()){
				Log.d("getOutputMediaFile", "failed to create directory");
				return null;
			}
		}
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		File mediaFile;
		if(type == MEDIA_TYPE_VIDEO){
			mediaFile = new File(mediaStorageDir.getPath() + 
					File.separator + "VID_" + timeStamp + ".mp4");
			filePath = mediaFile.getAbsolutePath();
		} else {
			return null;
		}
		return mediaFile;	
	}
	
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		releaseMediaRecorder();
		releaseCamera();
	}

	
	protected void onPause(){
		super.onPause();
		flag=false;
		releaseMediaRecorder();// if you are using MediaRecorder, release it first
		releaseCamera();// release the camera immediately on pause event
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()){
		case R.id.start:
			if(isRecording){
				stopRecorder();
				isPreview = false;
				touchFlag = true;
				mTimer.cancel();
				mChronometer.stop();
				mChronometer.setVisibility(View.GONE);
			}else{
//				mCamera.setOneShotPreviewCallback(VideoActivity.this);
				isPreview = true;
				if(prepareVideoRecorder()){
					mChronometer.setVisibility(View.VISIBLE);
					mChronometer.setBase(SystemClock.elapsedRealtime());
					mChronometer.start();
					mTimer.schedule(mTimerTask, 0,1000);
					touchFlag = false;
					mMediaRecorder.start();
					bt_capture.setText("Stop");
					mCamera.autoFocus(mAFCallback);
//					new Thread(scan).start();
					isRecording = true;
				}else{
					releaseMediaRecorder();
				}
			}
		}
		
	}
	
	private AutoFocusCallback mAFCallback = new AutoFocusCallback() {
		@Override
		public void onAutoFocus(boolean success, Camera camera) {
			if(success) {
				Log.d("AutoFocus", "success!");
					try {
						Parameters parameters = mCamera.getParameters();
						focalLength = parameters.getFocalLength();
						Log.d(TAG, "FocalLength: " + focalLength);
					} catch(Exception e) {
						e.printStackTrace();
					}
			}else {
				Log.d("AutoFocus", "failed!");
			}
		}	
	};
	
	private void stopRecorder(){
		handler.sendEmptyMessage(2);
		mMediaRecorder.stop();// stop the recording
		releaseMediaRecorder();// release the MediaRecorder object
		mCamera.lock();// take camera access back from MediaRecorder
		bt_capture.setText("Capture");
		isRecording = false;
		flag = false;
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
			params.setPreviewSize(MainActivity.previewWidth, MainActivity.previewHeight);
			
			double screenRatio = (double) dm.heightPixels/dm.widthPixels;
			double previewRatio = (double) Math.max(MainActivity.previewHeight,MainActivity.previewWidth)/
					Math.min(MainActivity.previewHeight,MainActivity.previewWidth);
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
			mCamera.setParameters(params);
		}
	}
	
	private DisplayMetrics getScreenSize(){
		DisplayMetrics dm = new DisplayMetrics();
		dm = getResources().getDisplayMetrics();
		return dm;
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
	public void onPreviewFrame(byte[] data, Camera camera) {
		Log.d(TAG, "onPreviewFrame");
		if(mRealFrameTask != null){
			switch (mRealFrameTask.getStatus()) {
			case RUNNING:
				return;

			case PENDING:
				mRealFrameTask.cancel(false);
				break;
			}
		}
		mRealFrameTask = new RealFrameTask(data);
		mRealFrameTask.execute((Void)null);
	}
	
	private class RealFrameTask extends AsyncTask<Void, Void, Void>{
		
		private byte[] data;
		
		public RealFrameTask(byte[] data) {
			this.data = data; 
		}

		@Override
		protected Void doInBackground(Void... params) {
			Log.d(TAG, "realFrameTask");
			Size size = mCamera.getParameters().getPreviewSize();
			final int width = size.width;
			final int height = size.height;
			Log.d(TAG, "width: " + width +" height: " + height);
			final YuvImage image = new YuvImage(data, ImageFormat.NV21, width, height, null);
			ByteArrayOutputStream os = new ByteArrayOutputStream(data.length);
			if(!image.compressToJpeg(new Rect(0, 0, width, height), 100, os)){
				return null;
			}
//			saveQueue.offer(os);
			try {
				os.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
			byte[] tmp = os.toByteArray();
			Bitmap bitmap = BitmapFactory.decodeByteArray(tmp, 0, tmp.length);
			String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
			String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)+"/"+"MCCam"+ File.separator + timeStamp + "_" + i++ +".jpg";
			File file = new File(path);
			if(!file.exists()){
				try {
					file.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			FileOutputStream fos;
			try {
				fos = new FileOutputStream(file);
				bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
				ExifInterface exif = new ExifInterface(path);
				exif.setAttribute(ExifInterface.TAG_MAKE, phoneMake);
				exif.setAttribute(ExifInterface.TAG_MODEL, phoneMode);
				exif.setAttribute(ExifInterface.TAG_FOCAL_LENGTH, focalLength+"");
				exif.saveAttributes();
				fos.flush();
				fos.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if(MainActivity.isAutoUpload){
				uploadFile(file);
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			
		}
	}
	
	class SaveThread extends Thread{
		@Override
		public void run() {
			super.run();
			while(true){
				while(!saveQueue.isEmpty()){
					synchronized(this){
						Log.d(TAG, "num: " + num);
						ByteArrayOutputStream bos = saveQueue.poll();
						byte[] tmp = bos.toByteArray();
						Bitmap bitmap = BitmapFactory.decodeByteArray(tmp, 0, tmp.length);
						String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
						String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)+"/"+"MCCam"+ File.separator + timeStamp + "_" + num++ +".jpg";
						File file = new File(path);
						if(!file.exists()){
							try {
								file.createNewFile();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
						FileOutputStream fos;
						try {
							fos = new FileOutputStream(file);
							bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
							ExifInterface exif = new ExifInterface(path);
							exif.setAttribute(ExifInterface.TAG_MAKE, phoneMake);
							exif.setAttribute(ExifInterface.TAG_MODEL, phoneMode);
							exif.setAttribute(ExifInterface.TAG_FOCAL_LENGTH, focalLength+"");
							exif.saveAttributes();
							fos.flush();
							fos.close();
							if(MainActivity.isAutoUpload){
								uploadPathQueue.offer(path);
							}
						} catch (FileNotFoundException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}

	class UploadThread extends Thread{
		@Override
		public void run() {
			super.run();
			while(true){
				while(!uploadPathQueue.isEmpty()){
					synchronized (this) {
						File file = new File(uploadPathQueue.poll());
						uploadFile(file);
					}
				}
			}
		}
	}
	
	public void uploadFile(File file) {
		
		HttpClient httpClient = new DefaultHttpClient();
		InputStream is = null;
		try {
			HttpPost httpPost = new HttpPost(MainActivity.sfm_upload);
				Log.d("UploadFile", file.getAbsolutePath());
				FileBody bin = new FileBody(file);
				MultipartEntityBuilder me = MultipartEntityBuilder.create();
				me.addPart("file", bin);
				HttpEntity reqEntity = me.build();
				httpPost.setEntity(reqEntity);
				Log.i("HttpPost","request " + httpPost.getRequestLine());
				HttpResponse response = httpClient.execute(httpPost);
				Log.i("HttpResponse",response.getStatusLine().toString());
				HttpEntity resEntity = response.getEntity();
				if (resEntity != null) {
					Log.i("resEntity","response content length: " + resEntity.getContentLength());
				}
				is = resEntity.getContent();
				BufferedReader br = new BufferedReader(new InputStreamReader(is));
				String line = "";
				String result = "";
				while ((line = br.readLine()) != null) {
					result += line;
				}
				Log.i("Result", result);
			if(is != null) is.close();
		} catch (ClientProtocolException e){
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (httpClient != null){
				httpClient.getConnectionManager().shutdown();
			}
		}
	}
	
//	class ScanThread implements Runnable{
//
//		@Override
//		public void run() {
//				while((!Thread.currentThread().isInterrupted()) && isScanThreadAlive){
//					try {
//						if(mCamera != null && isPreview){
//							mCamera.setOneShotPreviewCallback(VideoActivity.this);
//							Log.d(TAG, "scan");
//						}
//						Thread.sleep((long) (MainActivity.frameNum*1000));
//					} catch (InterruptedException e) {
//						e.printStackTrace();
//						Thread.currentThread().interrupt();
//					}
//			}
//		}
//	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		isScanThreadAlive = false;
		Log.d(TAG, "onDestroy "+isScanThreadAlive);
	}
}
