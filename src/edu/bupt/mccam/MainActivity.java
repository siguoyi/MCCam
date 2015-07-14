package edu.bupt.mccam;

import java.io.File;
import java.util.ArrayList;

import edu.bupt.camera.CameraActivity;
import edu.bupt.pickimg.ImagePickActivity;
import edu.bupt.utils.UploadHelper;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener {
	
	private static final int SELECT_IMAGES = 1;
	private String serverIp = null;
	
	private Button bt_capture;
	private Button bt_upload;
	private ProgressBar progressBar;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		bt_capture = (Button)findViewById(R.id.bt_capture);
		bt_upload = (Button)findViewById(R.id.bt_upload);
		progressBar = (ProgressBar)findViewById(R.id.progressBar);
		
		progressBar.setVisibility(ProgressBar.GONE);
		bt_capture.setOnClickListener(this);
		bt_upload.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.bt_capture:
			Intent camIntent = new Intent(MainActivity.this, CameraActivity.class);
			startActivity(camIntent);
			break;		
		case R.id.bt_upload:
			InputServerAddress();
			break;
		default: break;
		}
	}
	
	private void InputServerAddress() {
		final EditText et = new EditText(this);
		et.setHint("10.105.37.224");
		new AlertDialog.Builder(this)
			.setTitle("Input server address")
			.setView(et)
			.setPositiveButton(android.R.string.ok,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int which) {
							String s = et.getText().toString();
							serverIp = s.equals("") ? null : "http://" + s + "/save_file.php";				
							Log.d("InputAddress", " " + serverIp);
							picImage();
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
	
	private void picImage() {
		Intent intent = new Intent(this, ImagePickActivity.class);
		startActivityForResult(intent, SELECT_IMAGES);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch(requestCode){
		case SELECT_IMAGES:
			if(resultCode == RESULT_OK){
				ArrayList<String> paths = data.getStringArrayListExtra("IMAGE_PATHS");
				if(!paths.isEmpty()) {
					File[] files = new File[paths.size()];
					for(int i=0;i<paths.size();i++) {
						files[i] = new File(paths.get(i));
					}
					progressBar.setVisibility(ProgressBar.VISIBLE);
					new MyUploadHelper(serverIp).execute(files);
				} else {
					Toast.makeText(getApplicationContext(), "Nothing to do", Toast.LENGTH_LONG).show();
				}
			}
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
		}

		@Override
		public void onFinished() {
			Toast.makeText(getApplicationContext(), "Upload finished", Toast.LENGTH_LONG).show();
			progressBar.setVisibility(ProgressBar.GONE);
			progressBar.setProgress(0);
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