package edu.bupt.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;

import android.os.AsyncTask;
import android.util.Log;

public class DownloadHelper extends AsyncTask<File, Integer, Object> {

	private String downloadAddr;
	private long fileSize;
	
	public DownloadHelper(String downloadAddr){
		this.downloadAddr = downloadAddr;
	}
	
	public void downloadFile(File file){
		HttpGet httpGet = new HttpGet(downloadAddr);  
		HttpClient httpClient = new DefaultHttpClient();
		try {
			FileWriter fw = new FileWriter(file);
			HttpResponse httpResponse = httpClient.execute(httpGet);
			HttpEntity httpEntity = httpResponse.getEntity();
			if(httpEntity != null){
				BufferedReader br = new BufferedReader(new InputStreamReader(httpEntity.getContent()));
				fileSize = httpEntity.getContentLength();
				String line = null;
				int tmpSize = 0;
				while((line = br.readLine()) != null){
					line += "\n";
					fw.write(line);
					tmpSize += line.getBytes().length;
					publishProgress((int)(100*tmpSize/fileSize));
				}
				Log.d("download", "download complete!");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	protected Object doInBackground(File... params) {
		downloadFile(params[0]);
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
