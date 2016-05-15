package edu.bupt.camera;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;

import edu.bupt.mccam.MainActivity;
import edu.bupt.mccam.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.media.ExifInterface;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

public class WebCameraActivity extends Activity {
	private static final String TAG = "WebCamera";
	
	SurfaceView sView;
	SurfaceHolder surfaceHolder;
	Camera camera; // 定义系统所用的照相机
	boolean isPreview = false; // 是否在浏览中
	private Button bt_capture;
	private boolean isStart = false;
	private Queue<ByteArrayOutputStream> saveQueue;
	private Queue<String> uploadPathQueue;
	private volatile int num = 1;
	private SaveThread mSaveThread;
	private UploadThread mUploadThread;
	private boolean touchFlag = true;
	
	public static int previewWidth = 640;
	public static int previewHeight = 480;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// 设置全屏
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.webcamera);
		previewWidth = MainActivity.previewWidth;
		previewHeight = MainActivity.previewHeight;
		// 获取IP地址
		bt_capture = (Button) findViewById(R.id.bt_cam);
		bt_capture.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(isStart){
					isStart = false;
					touchFlag = true;
					camera.stopPreview();
					bt_capture.setText("Capture");
					WebCameraActivity.this.finish();
				}else{
					camera.setPreviewCallback(new StreamIt()); // 设置回调的类
					Toast.makeText(WebCameraActivity.this, "Start lifting frame", Toast.LENGTH_SHORT).show();
					bt_capture.setText("Stop");
					touchFlag = false;
					isStart = true;
				}
			}
		});
		sView = (SurfaceView) findViewById(R.id.sView); // 获取界面中SurfaceView组件
		surfaceHolder = sView.getHolder(); // 获得SurfaceView的SurfaceHolder
		saveQueue = new LinkedList<ByteArrayOutputStream>();
		uploadPathQueue = new LinkedList<String>();
		mSaveThread = new SaveThread();
		mUploadThread = new UploadThread();
		// 为surfaceHolder添加一个回调监听器
		surfaceHolder.addCallback(new Callback() {
			@Override
			public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
			}

			@Override
			public void surfaceCreated(SurfaceHolder holder) {
				initCamera(); // 打开摄像头
			}

			@Override
			public void surfaceDestroyed(SurfaceHolder holder) {
				// 如果camera不为null ,释放摄像头
				if (camera != null) {
					if (isPreview)
					camera.stopPreview();
				}
			}
		});
		// 设置该SurfaceView自己不维护缓冲
		surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		sView.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(touchFlag){
					camera.autoFocus(mAFCallback);
				}
			}
		});
		
		Toast.makeText(WebCameraActivity.this, "Current resolution is " + previewWidth + "*" + previewHeight, 
									Toast.LENGTH_LONG).show();
	}

	private AutoFocusCallback mAFCallback = new AutoFocusCallback() {
		@Override
		public void onAutoFocus(boolean success, Camera camera) {
			if(success) {
				Log.d("AutoFocus", "success!");
			}else {
				Log.d("AutoFocus", "failed!");
			}
		}	
	};
	
	private void initCamera() {
		if (!isPreview) {
			camera = Camera.open();
		}
		if (camera != null && !isPreview) {
			try {
				camera.setPreviewDisplay(surfaceHolder); // 通过SurfaceView显示取景画面
				Parameters params = camera.getParameters();
				params.setPreviewFrameRate(20);
				params.setPreviewSize(previewWidth, previewHeight);
				camera.setParameters(params);
				setCameraDisplayOrientation(WebCameraActivity.this, 0, camera);
				camera.startPreview(); // 开始预览
				camera.autoFocus(null); // 自动对焦
				mSaveThread.start();
				if(MainActivity.isAutoUpload){
					mUploadThread.start();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			isPreview = true;
		}
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

class StreamIt implements Camera.PreviewCallback {

	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		Size size = camera.getParameters().getPreviewSize();
		try {
			// 调用image.compressToJpeg（）将YUV格式图像数据data转为jpg格式
			YuvImage image = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
//			Log.d("size", "width: " + size.width + " height: " + size.height);
			if (image != null) {
				ByteArrayOutputStream outstream = new ByteArrayOutputStream();
				image.compressToJpeg(new Rect(0, 0, size.width, size.height), 80, outstream);
				saveQueue.offer(outstream);
				outstream.flush();
				// 启用线程将图像数据发送出去
//				if(MainActivity.isUpload){
//					Thread th = new MyThread(outstream, ip);
//					th.start();
//				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}

public void uploadFile(File file) {
	
	HttpClient httpClient = new DefaultHttpClient();
	InputStream is = null;
	try {
		HttpPost httpPost = new HttpPost(MainActivity.slam_upload);
			Log.d("UploadFile", file.getAbsolutePath());
			FileBody bin = new FileBody(file);
			MultipartEntityBuilder me = MultipartEntityBuilder.create();
			me.addPart("file", bin);
			HttpEntity reqEntity = me.build();
			httpPost.setEntity(reqEntity);
//			Log.i("HttpPost","request " + httpPost.getRequestLine());
			HttpResponse response = httpClient.execute(httpPost);
//			Log.i("HttpResponse",response.getStatusLine().toString());
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
//			Log.i("Result", result);
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
					String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmssSSS").format(new Date());
//					String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)+"/"+"MCCam"+ File.separator + timeStamp + ".jpg";
					String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)+"/"+"MCCam"+ File.separator + timeStamp + "_" + num++ +".jpg";
//					String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)+"/"+"MCCam"+ File.separator + num++ +".jpg";
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

//class MyThread extends Thread {
//	private byte byteBuffer[] = new byte[512];
//	private OutputStream outsocket;
//	private ByteArrayOutputStream myoutputstream;
//	private String ip;
//
//	public MyThread(ByteArrayOutputStream myoutputstream, String ip) {
//		this.myoutputstream = myoutputstream;
//		this.ip = ip;
//		try {
//			myoutputstream.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//
//	public void run() {
//		try {
//			// 将图像数据通过Socket发送出去
//			Socket tempSocket = new Socket(ip, 6000);
//			outsocket = tempSocket.getOutputStream();
//			ByteArrayInputStream inputstream = new ByteArrayInputStream(myoutputstream.toByteArray());
//			int amount;
//			while ((amount = inputstream.read(byteBuffer)) != -1) {
//				outsocket.write(byteBuffer, 0, amount);
//			}
//			myoutputstream.flush();
//			myoutputstream.close();
//			tempSocket.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//
//  }

	@Override
	protected void onDestroy() {
		super.onDestroy();
		camera.release();
	}
}