package edu.bupt.camera;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import edu.bupt.mccam.MainActivity;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.MediaMetadataRetriever;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class FrameLifting extends Service {
	private static final String TAG = "FrameLifting";
	
	private static String filePath;
	private static String phoneMode;
	private static String phoneMake;
	private static float focalLength; 
	
	private static int frameNumber;
	
	private Handler mHandler;

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "onCreate");
		mHandler = new Handler(){
        	@Override
        	public void handleMessage(Message msg) {
        		if(msg.what == 0x123){
        			Log.d(TAG, "Frame Lifting Complete!");
        			Toast.makeText(FrameLifting.this, frameNumber + " frames have been lifted!", Toast.LENGTH_LONG).show();
        			stopSelf();
        		}
        	}
        };
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "onStartCommand");
		filePath = intent.getExtras().getString("filePath");
		phoneMode = intent.getExtras().getString("phoneMode");
		phoneMake = intent.getExtras().getString("phoneMake");
		focalLength = intent.getExtras().getFloat("focalLength");
		Log.d(TAG, "filePath: " + filePath + "\n" 
							+ "phoneMode: " + phoneMode + "\n"
							  + "focalLength: " + focalLength);
		mThread.start();
		return super.onStartCommand(intent, flags, startId);
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	Thread mThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				Message msg = new Message();
	//			String dataPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)+"/"+"RecordVideo"+ "/testVideo.mp4";
				String dataPath = filePath;
				MediaMetadataRetriever retriever = new MediaMetadataRetriever();
				retriever.setDataSource(dataPath);
				// 取得视频的长度(单位为毫秒)
				String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
				// 取得视频的长度(单位为秒)
				int seconds = Integer.valueOf(time) / 1000;
				// 得到每一秒时刻的bitmap比如第一秒,第二秒
				frameNumber = seconds*MainActivity.frameNum;
				for (int i = 1; i <= frameNumber; i++) {
					Log.d(TAG, "framelift : " + i);
					Bitmap bitmap = retriever.getFrameAtTime((i/MainActivity.frameNum)*1000*1000,MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
//					Matrix matrix = new Matrix();
//					matrix.setScale(1, 360/480);
//					Bitmap tempBitmap = Bitmap.createBitmap(bitmap, 0, 0, 640, 360);
					String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
					String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)+"/"+"MCCam"+ File.separator + timeStamp + "_" + i +".jpg";
					FileOutputStream fos = null;
					try {
						fos = new FileOutputStream(path);
						bitmap.compress(CompressFormat.JPEG, 100, fos);
						ExifInterface exif = new ExifInterface(path);
						exif.setAttribute(ExifInterface.TAG_MAKE, phoneMake);
						exif.setAttribute(ExifInterface.TAG_MODEL, phoneMode);
						exif.setAttribute(ExifInterface.TAG_FOCAL_LENGTH, focalLength+"");
						exif.saveAttributes();
						fos.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				msg.what = 0x123;
				mHandler.sendMessage(msg);
			}
		});
	
	public void onDestroy() {
		Log.d(TAG, "onDestroy");
	};
	}
