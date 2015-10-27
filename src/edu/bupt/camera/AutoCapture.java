package edu.bupt.camera;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import edu.bupt.mccam.R;

import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.media.CamcorderProfile;
import android.media.ExifInterface;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

public class AutoCapture extends Activity implements SensorEventListener,OnClickListener,SurfaceHolder.Callback{
	
	private static String TAG = "AutoCapture";
	private static final float NS2S = 1.0f / 1000000000.0f;
	public static final int MEDIA_TYPE_VIDEO = 2;
	private static final int MEDIA_TYPE_IMAGE = 1;
	
	private static final String packageName = "edu.bupt.framelifting";
	private static final String className = "edu.bupt.framelifting.MainActivity";
	
	private SensorManager sensorManager;
	private Sensor gyroscopeSensor;
	
	private float timestamp;
	private float angle[] = new float[3];
	private ProgressBar progressBar;
	private int rotateProgress = 0;
	private static int captureNum = 24;
	
	private float anglex, angley, anglez;
	
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
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_auto_capture);
		progressBar=(ProgressBar)findViewById(R.id.progressBar_record_progress);
		progressBar.setVisibility(View.VISIBLE);
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		bt_capture=(Button) findViewById(R.id.start);
		bt_capture.setOnClickListener(this);
		mCamera=getCameraInstance();
		mSurfaceView=(SurfaceView) findViewById(R.id.SurfaceView);
		mSurfaceHolder=mSurfaceView.getHolder();
		mSurfaceHolder.addCallback(this);
		mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	
		phoneMode = Build.MODEL;
		phoneMake = Build.BRAND;
		
		handler=new Handler(){
			@Override
			public void handleMessage(Message msg){
				if(msg.what==1){
					progressBar.setProgress(rotateProgress);
				}
				if(msg.what==2){
					Toast.makeText(AutoCapture.this,"Capture Complete!",Toast.LENGTH_SHORT).show();
					
					Intent intent = new Intent(AutoCapture.this, FrameLifting.class);
					intent.putExtra("filePath", filePath);
					intent.putExtra("phoneMode", phoneMode);
					intent.putExtra("phoneMake", phoneMake);
					intent.putExtra("focalLength", focalLength);
					startService(intent);
				}
			}
		};
		
	}
	
	public void init(){
		rotateProgress=0;
		angle[0]=0;
		angle[1]=0;
		angle[2]=0;
		progressBar.setProgress(rotateProgress);
		
	}
	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
			if (timestamp != 0) {
				final float dT = (event.timestamp - timestamp) * NS2S;
				angle[0] += event.values[0] * dT;
				angle[1] += event.values[1] * dT;
				angle[2] += event.values[2] * dT;
				anglex = (float) Math.toDegrees(angle[0]);
				angley= (float) Math.toDegrees(angle[1]);
				anglez = (float) Math.toDegrees(angle[2]);
				flag = true;
				
				if(rotateProgress < 100){
					rotateProgress = Math.abs((int)(((float)angley/360)*100));
					handler.sendEmptyMessage(1);
				}else{
					stopRecorder();
				}
			}
			timestamp = event.timestamp;
		}
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
		mCamera.unlock();
		mMediaRecorder.setCamera(mCamera);
		// Step 2: Set sources
		mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
		mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		// Step 3: Set a CamcorderProfile (requires API Level 8 or higher instead of setting format and encoding)
		mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
		// Step 4: Set output file
		mMediaRecorder.setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO).toString());
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
		sensorManager.unregisterListener(this);
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
			}else{
				// initialize video camera
				if(prepareVideoRecorder()){
					mMediaRecorder.start();
					bt_capture.setText("Stop");
					mCamera.autoFocus(mAFCallback);
					isRecording = true;
					init();
					sensorManager.registerListener(this, gyroscopeSensor,
							SensorManager.SENSOR_DELAY_GAME);
				}else{
					releaseMediaRecorder();
				}
			}
		}
		
	}
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		
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
		sensorManager.unregisterListener(this);
		handler.sendEmptyMessage(2);
		mMediaRecorder.stop();// stop the recording
		releaseMediaRecorder();// release the MediaRecorder object
		mCamera.lock();// take camera access back from MediaRecorder
		bt_capture.setText("Capture");
		isRecording = false;
		flag = false;
		init();
	}
}
