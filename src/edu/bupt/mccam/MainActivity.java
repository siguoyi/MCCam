package edu.bupt.mccam;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.bupt.camera.AutoCapture;
import edu.bupt.camera.CameraActivity;
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
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener {
	
	private static final int SELECT_IMAGES = 1;
	private String serverIp = "http://10.105.32.59/save_file.php";
	private String server_url_reconstruction = "http://10.105.32.59/reconstruction.php?peak_threshold=";
	private String server_url_log = "http://10.105.32.59/loglog.php";
	private static File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
			Environment.DIRECTORY_PICTURES), "MCCam");
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
	
	private String s;
	
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
		initAutoCompleteTextView();
	}
	
	private void initAutoCompleteTextView() {
		sp = getSharedPreferences("server_addr", 0);
		Set<String> history = sp.getStringSet("history", new HashSet<String>());
		String[] histArray = history.toArray(new String[0]);
		tv_adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_dropdown_item_1line, histArray);
		tv_auto.setAdapter(tv_adapter);
		tv_auto.setThreshold(1);
		tv_auto.setOnFocusChangeListener(new OnFocusChangeListener(){
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				AutoCompleteTextView view = (AutoCompleteTextView) v;
				if(hasFocus) {
					view.showDropDown();
				}
			}
		});
	}
	
	private void saveServerAddr(String s) {
		if(!s.equals("")) {
			Set<String> hist = sp.getStringSet("history", new HashSet<String>());
			if(!hist.contains(s)) {
				tv_adapter.add(s);
				Set<String> nhist = new HashSet<String>(hist);
				nhist.add(s);
				sp.edit().putStringSet("history", nhist).commit();
			}
		}
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.bt_capture:
			Intent camIntent = new Intent(MainActivity.this, AutoCapture.class);
			startActivity(camIntent);
			break;		
		case R.id.bt_upload:
			if(isNetworkConnected()){
				bt_upload.setEnabled(false);
				if(mediaStorageDir.exists()) {
					if (mediaStorageDir.list().length > 0) {
						InputServerAddress();
						break;
					}
				}
				bt_upload.setEnabled(true);
				Toast.makeText(getApplicationContext(), "Nothing", Toast.LENGTH_LONG).show();
			} else{
				Toast.makeText(this, "Network is unavailable!",
						Toast.LENGTH_LONG).show();
			}
			break;
		case R.id.bt_reconstruction:
			bt_reconstruction.setEnabled(false);
			InputPeakThreshold();
			break;
		case R.id.bt_result:
			String downloadUrl = "http://60.247.77.137:52002/result/option-0000.obj";
			Pattern p = Pattern.compile(".*result/(.*)");
			Matcher m = p.matcher(downloadUrl);
			if(m.find()){
				s = m.group(1);
			}
			filePath = downloadResult(downloadUrl);
			
			break;
		default: break;
		}
	}
	
	private String downloadResult(String downloadAddr) {
		File fileDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
											+ "/MCCresults");
		if(!fileDir.exists()){
			fileDir.mkdir();
		}
		File file = new File(fileDir, s);
		progressBar.setMax(100);
		progressBar.setVisibility(ProgressBar.VISIBLE);
		new MyDownloadHelper(downloadAddr).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, file);
		return file.getAbsolutePath();
	}

	public boolean isNetworkConnected() {
		ConnectivityManager connManager = (ConnectivityManager) this
				.getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
		return networkInfo != null ? networkInfo.isConnected() : false;
	}
	
	private void InputPeakThreshold() {
		tv_message.setText("");
		final EditText et = new EditText(this);
		et.setHint("1");
		new AlertDialog.Builder(this)
			.setTitle("Input peak threshold")
			.setView(et)
			.setCancelable(false)
			.setPositiveButton(android.R.string.ok,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String s = et.getText().toString();
						String value = s.equals("") ? "10" : s;				
						Log.d("InputValue", " " + value);
						new MyHttpClientTask().execute(server_url_reconstruction + value,
								server_url_log);
					}
				})
			.setNegativeButton(android.R.string.cancel,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						bt_reconstruction.setEnabled(true);
					}
				})
			.show();
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
	
	private void InputServerAddress() {
		ViewGroup vg = (ViewGroup) tv_auto.getParent();
		if(vg != null) {
			vg.removeView(tv_auto);
		}
		new AlertDialog.Builder(this)
			.setTitle("Input server address")
			.setView(tv_auto)
			.setCancelable(false)
			.setPositiveButton(android.R.string.ok,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String s = tv_auto.getText().toString();
						updateServerAddr(s);
						Log.d("InputAddress", " " + serverIp);
						saveServerAddr(s);
						tv_auto.setText("");
						picImage();
					}
				})
			.setNegativeButton(android.R.string.cancel,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						tv_auto.setText("");
						bt_upload.setEnabled(true);
					}
				})
			.show();
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
	
	private class MyUploadHelper extends UploadHelper{
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
			.setPositiveButton(android.R.string.ok,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int which) {
							MainActivity.this.finish();
						}
					})
			.setNegativeButton(android.R.string.cancel,
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
			Intent intent = new Intent(Intent.ACTION_MAIN);
			intent.addCategory(Intent.CATEGORY_LAUNCHER);            
			ComponentName cn = new ComponentName(packageName, className);            
			intent.setComponent(cn);
			intent.putExtra("filePath", filePath);
			startActivity(intent);
		}
		
	}

}
