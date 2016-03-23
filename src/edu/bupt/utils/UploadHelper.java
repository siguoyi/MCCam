package edu.bupt.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;

import android.os.AsyncTask;
import android.util.Log;

public class UploadHelper extends AsyncTask<File[], Integer, Object> {

	public String user_upload_url;
	
	public UploadHelper(String addr) {
		user_upload_url = addr;
		Log.d("url", "url: " + user_upload_url);
	}
	
	public void uploadFile(File[] files) {
		if (files.length == 0) {
			Log.i("FileList", "empty!");
			return;
		}
		HttpClient httpClient = new DefaultHttpClient();
		InputStream is = null;
		try {
			HttpPost httpPost = new HttpPost(user_upload_url);
			for (int i=0;i<files.length;i++) {
				Log.d("UploadFile", files[i].getAbsolutePath());
				FileBody bin = new FileBody(files[i]);
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
				publishProgress(i+1);
			}
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

	@Override
	protected Object doInBackground(File[]... params) {
		uploadFile(params[0]);
		return null;
	}
	
	public void updateProgress(int progress) {
	}
	
	public void onFinished() {
	}

	@Override
	protected void onProgressUpdate(Integer... values) {
		super.onProgressUpdate(values);
		updateProgress(values[0].intValue());
	}

	@Override
	protected void onPostExecute(Object result) {
		super.onPostExecute(result);
		onFinished();
	}

}
