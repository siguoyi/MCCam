package edu.bupt.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.os.AsyncTask;
import android.util.Log;

public class HttpClientHelper extends AsyncTask<String,String,String>{
	
	public String get(String url) {
		String result = " ";
		String lastLine = " ";
		HttpClient httpClient = new DefaultHttpClient();
		HttpGet httpRequest = new HttpGet(url);
		InputStream is = null;
		try {
			HttpResponse httpResponse = httpClient.execute(httpRequest);
			if(httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				is = httpResponse.getEntity().getContent();
				BufferedReader reader = new BufferedReader(new InputStreamReader(is));
				StringBuilder builder = new StringBuilder();
				String line = "";
				while((line=reader.readLine())!=null) {
					builder.append(line + "\n");
					lastLine = line;
				}
				is.close();
				result = builder.toString();
			} else {
				result = "Request error!";
			}
			publishProgress(result);
			httpClient.getConnectionManager().shutdown();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return lastLine;
	}

	@Override
	protected String doInBackground(String... params) {
		Log.d("get request", "reconstruction.php");
		String lastLine = get(params[0]);
		Log.d("get request", "loglog.php");
		if(lastLine.equals("fileok")) {
			while(true) {
				try {
					Thread.sleep(3000);
					lastLine = get(params[1]);
					if (lastLine.contains("okokokokok")) {
						break;
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		return lastLine;
	}

	@Override
	protected void onProgressUpdate(String... values) {
		super.onProgressUpdate(values);
		updateProgress(values[0]);
	}

	@Override
	protected void onPostExecute(String result) {
		super.onPostExecute(result);
		onFinished();
	}
	
	public void onFinished() {
	}
	
	public void updateProgress(String values) {
	}
	
}
