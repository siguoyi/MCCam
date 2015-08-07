package edu.bupt.mccam;

import java.io.File;
import java.util.ArrayList;

import edu.bupt.camera.CameraActivity;
import edu.bupt.pickimg.ImagePickActivity;
import edu.bupt.utils.HttpClientHelper;
import edu.bupt.utils.UploadHelper;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
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
	private TextView tv_message;
	private ProgressBar progressBar;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		bt_capture = (Button)findViewById(R.id.bt_capture);
		bt_upload = (Button)findViewById(R.id.bt_upload);
		bt_reconstruction = (Button)findViewById(R.id.bt_reconstruction);
		progressBar = (ProgressBar)findViewById(R.id.progressBar);
		tv_message = (TextView) findViewById(R.id.tv_message);
		
		progressBar.setVisibility(ProgressBar.GONE);
		bt_capture.setOnClickListener(this);
		bt_upload.setOnClickListener(this);
		bt_reconstruction.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.bt_capture:
			Intent camIntent = new Intent(MainActivity.this, CameraActivity.class);
			startActivity(camIntent);
			break;		
		case R.id.bt_upload:
			bt_upload.setEnabled(false);
			if(mediaStorageDir.exists()) {
				if (mediaStorageDir.list().length > 0) {
					InputServerAddress();
					break;
				}
			}
			bt_upload.setEnabled(true);
			Toast.makeText(getApplicationContext(), "Nothing", Toast.LENGTH_LONG).show();
			break;
		case R.id.bt_reconstruction:
			bt_reconstruction.setEnabled(false);
			InputPeakThreshold();
		default: break;
		}
	}
	
	private void InputPeakThreshold() {
		tv_message.setText("");
		final EditText et = new EditText(this);
		et.setHint("10");
		new MyAlertDialog(this,et,"Input peak threshold",bt_reconstruction,
			new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String s = et.getText().toString();
					String value = s.equals("") ? "10" : s;				
					Log.d("InputValue", " " + value);
					new MyHttpClientTask().execute(server_url_reconstruction + value,
							server_url_log);
				}
			}).show();
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
		final EditText et = new EditText(this);
		et.setHint("10.105.32.59:80");
		new MyAlertDialog(this,et,"Input server address",bt_upload,
			new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String s = et.getText().toString();
					updateServerAddr(s);
					Log.d("InputAddress", " " + serverIp);
					picImage();
				}
			}).show();
	}
	
	private class MyAlertDialog {
		AlertDialog.Builder dialog;
		Button bt_triger;
		
		protected MyAlertDialog(Context context, EditText et, String title, Button bt,
				DialogInterface.OnClickListener okListener) {
			bt_triger = bt;
			dialog = new AlertDialog.Builder(context)
				.setTitle(title)
				.setView(et)
				.setCancelable(false)
				.setPositiveButton(android.R.string.ok, okListener)
				.setNegativeButton(android.R.string.cancel,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int which) {
							bt_triger.setEnabled(true);
						}
					});
		}
		public void show() {
			dialog.show();
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

}
