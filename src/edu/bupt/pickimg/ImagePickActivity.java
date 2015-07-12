package edu.bupt.pickimg;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

import edu.bupt.mccam.R;
import edu.bupt.pickimg.ImageAdapter.ThumbImage;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;

public class ImagePickActivity extends Activity implements OnClickListener {
	
	private int startIndex, endIndex;
	
	private GridView mImgGrid;
	private ImageAdapter imageAdapter;
	private Button bt_pick;
	
	private ArrayList<ThumbImage> tImages = new ArrayList<ThumbImage>();
	private static File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
			Environment.DIRECTORY_PICTURES), "MCCam");
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_image_pick);
		
		mImgGrid = (GridView)findViewById(R.id.mImgGrid);
		bt_pick = (Button)findViewById(R.id.bt_pick);
		bt_pick.setOnClickListener(this);
		
		imageAdapter = new ImageAdapter(this);
		mImgGrid.setAdapter(imageAdapter);
		mImgGrid.setOnScrollListener(new OnScrollListener(){
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				startIndex = firstVisibleItem;
				endIndex = firstVisibleItem + visibleItemCount;
			}

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				switch(scrollState){
				case OnScrollListener.SCROLL_STATE_IDLE:
					for(;startIndex<endIndex;startIndex++){
						ImageView iv = (ImageView) mImgGrid.findViewWithTag("img" + startIndex);
						if(iv != null){
							//Log.d("FindImageView", "success");
						}
					}
					break;
				case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
				case OnScrollListener.SCROLL_STATE_FLING:
					
					break;
				default: break;
				}
			}
		});
		getImages();
	}
	
	private void getImages(){
		File[] files = mediaStorageDir.listFiles(new FilenameFilter(){
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".jpg");
			}
		});
		for(File f:files){
			ThumbImage t = imageAdapter.new ThumbImage();
			t.imagePath = f.getAbsolutePath();
			//Log.d("File name", " " + t.imagePath);
			t.isChecked = false;
			tImages.add(t);
		}
		imageAdapter.addAll(tImages);
	}

	@Override
	public void onClick(View arg0) {
		ArrayList<String> paths = imageAdapter.getPickedImagePath();
		
		Intent imagePicked = new Intent();
		if(paths != null){
			imagePicked.putStringArrayListExtra("IMAGE_PATHS", paths);
		}
		setResult(RESULT_OK, imagePicked);
		finish();
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		this.finish();
	}
	
}
