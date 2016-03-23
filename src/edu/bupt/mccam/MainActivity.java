package edu.bupt.mccam;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import edu.bupt.camera.CameraActivity;
import edu.bupt.camera.VideoActivity;
import edu.bupt.pickimg.ImagePickActivity;
import edu.bupt.utils.DownloadHelper;
import edu.bupt.utils.HttpClientHelper;
import edu.bupt.utils.UploadHelper;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener {
	private static final String TAG = "MainActivity";
	
	private static final int SELECT_IMAGES = 1;
	public static String serverIp = "http://10.105.32.59/save_file.php";
	private static String server_url_reconstruction = "http://10.105.32.59/reconstruction.php?peak_threshold=";
	private String server_url_log = "http://10.105.32.59/loglog.php";
//	private String download_url = "http://60.247.77.137:52002/result/option-0000.obj";
	private String download_url = "http://10.105.32.59/result/option-0000.ply.csv";
	private static File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
			Environment.DIRECTORY_PICTURES), "MCCam");
	private static File resultsFileDir = new File(Environment.getExternalStorageDirectory()
			.getAbsolutePath() + "/MCCResults");
	private ArrayList<String> paths = new ArrayList<String>();
	
	private Button bt_capture;
	private Button bt_upload;
	private Button bt_reconstruction;
	private Button bt_result;
	private TextView tv_message;
	private ProgressBar progressBar;
	private AutoCompleteTextView tv_auto;
	private ArrayAdapter<String> tv_adapter;
	private String filePath;
	
	private SharedPreferences sp;
	
	private static final String packageName = "edu.buptant.pointscloudviewer";
	private static final String className = "edu.buptant.pointscloudviewer.MainActivity";
		
	public static int frameNum = 1;
	public static float peek_threshold = 0.01f;
//	public static String upload_url = "60.247.77.137:52002";
	public static String upload_url = "";
	private static int capture_mode = 1;	
	private static boolean uploadFlag = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		bt_capture = (Button)findViewById(R.id.bt_capture);
		bt_upload = (Button)findViewById(R.id.bt_upload);
		bt_reconstruction = (Button)findViewById(R.id.bt_reconstruction);
		bt_result = (Button)findViewById(R.id.bt_result);
		progressBar = (ProgressBar)findViewById(R.id.progressBar);
		tv_message = (TextView) findViewById(R.id.tv_message);
		
		progressBar.setVisibility(ProgressBar.GONE);
		bt_capture.setOnClickListener(this);
		bt_upload.setOnClickListener(this);
		bt_reconstruction.setOnClickListener(this);
		bt_result.setOnClickListener(this);
		tv_auto = new AutoCompleteTextView(this);
//		initAutoCompleteTextView();
		
		Log.d("MainActivity", "Frame numbers: " + frameNum + " Peek threshold: " + peek_threshold);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		int id = item.getItemId();
		switch (id) {
		case R.id.frame_lifting:
			showFrameSettingDialog();
			break;
		case R.id.peek_threshold:
			showThresholdSettingDialog();
			break;	
		case R.id.upload_url:
			showUrlSettingDialog();
			break;	
		case R.id.camera:
			capture_mode = 0;
			item.setChecked(true);
			Toast.makeText(this, "Camera Mode", Toast.LENGTH_SHORT).show();
			Log.d("Capture Mode", "Camera Mode");
			break;
		case R.id.video:
			capture_mode = 1;
			item.setChecked(true);
			Toast.makeText(this, "Video Mode", Toast.LENGTH_SHORT).show();
			Log.d("Capture Mode", "Video Mode");
			break;
		case R.id.clear_history:
			new AlertDialog.Builder(this)
			.setTitle("Clear history?")
			.setPositiveButton("ok",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int which) {
							File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)+"/"+"MCCam");
							deleteFile(file);
							if(!file.exists()){
								file.mkdirs();
							}
						}
					})
			.setNegativeButton("cancel",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int which) {
						}
					})
			.show();
			break;
		default:
			break;
		}
		return true;
	}
	
	private void showFrameSettingDialog(){
		AlertDialog dialog = null;
		AlertDialog.Builder builder = null;
		View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.frame_dialog, null);
		final EditText et_num = (EditText) view.findViewById(R.id.et_frame_dialog);
		builder = new AlertDialog.Builder(MainActivity.this);
		builder.setTitle("Input frame interval£º");
		builder.setView(view);
		builder.setPositiveButton("Confirm",new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				int num = Integer.parseInt(et_num.getText().toString());
				frameNum = num;
				Log.d("frame number", "Frame numbers: " + frameNum);
			}
		}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});

		dialog = builder.create();
		dialog.show();
	}
		
	private void showThresholdSettingDialog(){
		AlertDialog dialog = null;
		AlertDialog.Builder builder = null;
		View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.threshold_dialog, null);
		final EditText et_num = (EditText) view.findViewById(R.id.et_threshold_dialog);
		builder = new AlertDialog.Builder(MainActivity.this);
		builder.setTitle("Input peek threshold£º");
		builder.setView(view);
		builder.setPositiveButton("Confirm",new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				float threshold = Float.parseFloat(et_num.getText().toString());
				peek_threshold = threshold;
				Log.d("peek threshold", "Peek threshold: " + peek_threshold);
			}
		}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});

		dialog = builder.create();
		dialog.show();
	}
	
	private void showUrlSettingDialog(){
		AlertDialog dialog = null;
		AlertDialog.Builder builder = null;
		View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.url_dialog, null);
		final EditText et_num = (EditText) view.findViewById(R.id.et_url_dialog);
		builder = new AlertDialog.Builder(MainActivity.this);
		builder.setTitle("Input upload url£º");
		builder.setView(view);
		builder.setPositiveButton("Confirm",new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				upload_url = et_num.getText().toString();
				Log.d("upload url", "Upload Url: " + upload_url);
			}
		}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});

		dialog = builder.create();
		dialog.show();
	}
	
//	private void initAutoCompleteTextView() {
//		sp = getSharedPreferences("server_addr", 0);
//		Set<String> history = sp.getStringSet("history", new HashSet<String>());
//		String[] histArray = history.toArray(new String[0]);
//		tv_adapter = new ArrayAdapter<String>(this,
//				android.R.layout.simple_dropdown_item_1line, histArray);
//		tv_auto.setAdapter(tv_adapter);
//		tv_auto.setThreshold(1);
//		tv_auto.setOnFocusChangeListener(new OnFocusChangeListener(){
//			@Override
//			public void onFocusChange(View v, boolean hasFocus) {
//				AutoCompleteTextView view = (AutoCompleteTextView) v;
//				if(hasFocus) {
//					view.showDropDown();
//				}
//			}
//		});
//	}
	
	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.bt_capture:
			if(capture_mode == 0){
				Intent cameraIntent = new Intent(MainActivity.this, CameraActivity.class);
				startActivity(cameraIntent);
			}else{
				Intent videoIntent = new Intent(MainActivity.this, VideoActivity.class);
				startActivity(videoIntent);
			}
			break;		
		case R.id.bt_upload:
			if(isNetworkConnected()){
				bt_upload.setEnabled(false);
				if(mediaStorageDir.exists()) {
					if (mediaStorageDir.list().length > 0) {
						updateServerAddr(upload_url);
						Log.d("InputAddress", upload_url);
						picImage();
						break;
					}
				}
				bt_upload.setEnabled(true);
				Toast.makeText(getApplicationContext(), "Nothing to upload", Toast.LENGTH_LONG).show();
			} else{
				Toast.makeText(this, "Network is unavailable!",
						Toast.LENGTH_LONG).show();
			}
			break;
		case R.id.bt_reconstruction:
			if(uploadFlag){
				uploadFlag = false;
				bt_reconstruction.setEnabled(false);
				new MyHttpClientTask().execute(server_url_reconstruction + peek_threshold,
						server_url_log);
			}else{
				Toast.makeText(this, "Nothing to reconstruct", Toast.LENGTH_SHORT).show();
			}
//			InputPeakThreshold();
			break;
		case R.id.bt_result:
			new AlertDialog.Builder(this)
			.setTitle("Download the newest model?")
			.setPositiveButton("ok",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int which) {
							downloadResult(download_url);
						}
					})
			.setNegativeButton("cancel",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int which) {
							startPointCloudViewer(filePath);
						}
					})
			.show();
			
			break;
		default: break;
		}
	}
	
	private void startPointCloudViewer(String filePath) {
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);            
		ComponentName cn = new ComponentName(packageName, className);            
		intent.setComponent(cn);
		intent.putExtra("filePath", filePath);
		startActivity(intent);
	}
	
	private void downloadResult(String downloadAddr) {
		if(!resultsFileDir.exists()){
			resultsFileDir.mkdir();
		}
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		File result = new File(resultsFileDir, "3d_" + timeStamp + ".csv");
		filePath = result.getAbsolutePath();
		progressBar.setMax(100);
		progressBar.setVisibility(ProgressBar.VISIBLE);
		new MyDownloadHelper(downloadAddr).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, result);
	}

	public boolean isNetworkConnected() {
		ConnectivityManager connManager = (ConnectivityManager) this
				.getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
		return networkInfo != null ? networkInfo.isConnected() : false;
	}
	
	private void updateServerAddr(String ip) {
		if (!ip.equals("")){
			serverIp = "http://" + ip + "/save_file.php";
			server_url_reconstruction = "http://" + ip + "/reconstruction.php?peak_threshold=";
			server_url_log = "http://" + ip + "/loglog.php";
		} else {
			serverIp = "http://10.105.32.59/save_file.php";
			server_url_reconstruction = "http://10.105.32.59/reconstruction.php?peak_threshold=";
			server_url_log = "http://10.105.32.59/loglog.php";
		}
		
	}
	
	private void picImage() {
		Intent intent = new Intent(this, ImagePickActivity.class);
		startActivityForResult(intent, SELECT_IMAGES);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch(requestCode){
		case SELECT_IMAGES:
			bt_upload.setEnabled(true);
			if(resultCode == RESULT_OK){
				paths = data.getStringArrayListExtra("IMAGE_PATHS");
				if(!paths.isEmpty()) {
					File[] files = new File[paths.size()];
					for(int i=0;i<paths.size();i++) {
						files[i] = new File(paths.get(i));
					}
					tv_message.setText("Uploading : 0 / " + paths.size() + "\n");
					progressBar.setMax(files.length);
					progressBar.setVisibility(ProgressBar.VISIBLE);
					//new MyUploadHelper(serverIp).execute(files);
					new MyUploadHelper(serverIp).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, files);
					break;
				}
			} 
			Toast.makeText(getApplicationContext(), "Nothing to do", Toast.LENGTH_LONG).show();
			break;
		}
	}
	
	public void deleteFile(File file) { 
        if (file.exists() == false) { 
            Log.d(TAG, "file is not existed!"); 
            return; 
        } else { 
            if (file.isFile()) { 
                file.delete(); 
                return; 
            } 
            if (file.isDirectory()) { 
                File[] childFile = file.listFiles(); 
                if (childFile == null || childFile.length == 0) { 
                    file.delete(); 
                    return; 
                } 
                for (File f : childFile) { 
                    deleteFile(f); 
                } 
                file.delete(); 
            } 
        } 
    } 
	
	public class MyUploadHelper extends UploadHelper{
		public MyUploadHelper(String addr) {
			super(addr);
		}

		@Override
		public void updateProgress(int progress) {
			progressBar.setProgress(progress);
			tv_message.setText(generateProgressInfo(progress));
		}
		
		private String generateProgressInfo(int progress) {
			String result = "Uploading : " + progress + " / " + progressBar.getMax() + "\n";
			for (int i=0;i<progress;i++) {
				result += paths.get(i) + "\n";
			}
			return result;
		}

		@Override
		public void onFinished() {
			Toast.makeText(getApplicationContext(), "Upload finished", Toast.LENGTH_LONG).show();
			uploadFlag = true;
			progressBar.setVisibility(ProgressBar.GONE);
			progressBar.setProgress(0);
		}
	}
	
	private class MyHttpClientTask extends HttpClientHelper {
		@Override
		public void updateProgress(String values) {
			tv_message.setText(values);
		}

		@Override
		public void onFinished() {
			bt_reconstruction.setEnabled(true);
			Toast.makeText(getApplicationContext(), "finished reconstruction", Toast.LENGTH_LONG).show();	
		}
	}

	@Override
	public void onBackPressed() {
		new AlertDialog.Builder(this)
			.setTitle(R.string.exit)
			.setPositiveButton("Confirm",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int which) {
							MainActivity.this.finish();
						}
					})
			.setNegativeButton("Cancel",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int which) {
						}
					})
			.show();
	}
	
	private class MyDownloadHelper extends DownloadHelper {

		public MyDownloadHelper(String downloadAddr) {
			super(downloadAddr);
		}

		@Override
		public void updateProgress(int progress) {
			progressBar.setProgress(progress);
			tv_message.setText(generateProgressInfo(progress));
		}
		
		private String generateProgressInfo(int progress) {
			String result = "Downloading : " + (progress*100) / progressBar.getMax() + "%\n";
			return result;
		}

		@Override
		public void onFinished() {
			Toast.makeText(getApplicationContext(), "Download finished", Toast.LENGTH_LONG).show();
			progressBar.setVisibility(ProgressBar.GONE);
			progressBar.setProgress(0);
			startPointCloudViewer(filePath);
		}
		
	}

}
