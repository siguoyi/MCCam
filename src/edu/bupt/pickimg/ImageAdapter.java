package edu.bupt.pickimg;

import java.util.ArrayList;

import edu.bupt.mccam.R;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;

public class ImageAdapter extends BaseAdapter {

	private ArrayList<ThumbImage> data = new ArrayList<ThumbImage>();
	private LayoutInflater inflater;
	public ImageAdapter(Context context) {
		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public int getCount() {
		return data.size();
	}

	@Override
	public Object getItem(int position) {
		return data.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final ViewHolder viewHolder;
		if(convertView == null){
			convertView = inflater.inflate(R.layout.image_item, parent, false);
			viewHolder = new ViewHolder();
			viewHolder.imageView = (ImageView) convertView.findViewById(R.id.thumbnail);
			viewHolder.checkBox = (CheckBox) convertView.findViewById(R.id.checkBox);
			convertView.setTag(viewHolder);
		}else {
			viewHolder = (ViewHolder) convertView.getTag();
			viewHolder.imageView.setImageResource(R.drawable.no_media);
		}
		viewHolder.position = position;
		viewHolder.imageView.setTag("img" + position);;
		viewHolder.checkBox.setTag(position);
		viewHolder.checkBox.setId(position);
		viewHolder.checkBox.setChecked(data.get(position).isChecked);
		viewHolder.checkBox.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				CheckBox box = (CheckBox) v;
				int id = box.getId();
				boolean t = data.get(id).isChecked;
				box.setChecked(!t);
				data.get(id).isChecked = !t;
			}
		});
		new MyImageLoader(position, viewHolder)
			.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, data.get(position).imagePath);
		return convertView;
	}
	
	public class ViewHolder{
		int position;
		ImageView imageView;
		CheckBox checkBox;
	}
	
	public class ThumbImage{
		public String imagePath;
		public boolean isChecked;
	}
	
	public void addAll(ArrayList<ThumbImage> tImages) {
		if(tImages != null){
			data.addAll(tImages);
			notifyDataSetChanged();
		}
	}
	
	public void setCheck(int position, boolean check) {
		if(position < data.size()){
			data.get(position).isChecked = check;
		}
	}

	public void selectAll(){
		if(!data.isEmpty()) {
			for(ThumbImage ti:data){
				ti.isChecked = true;
			}
			//notifyDataSetChanged();
		}
	}
	
	public void deSelectAll(){
		if(!data.isEmpty()) {
			for(ThumbImage ti:data){
				ti.isChecked = false;
			}
			//notifyDataSetChanged();
		}
	}
	
	public ArrayList<String> getPickedImagePath() {
		if(!data.isEmpty()){
			ArrayList<String> paths = new ArrayList<String>();
			for(ThumbImage t:data) {
				if(t.isChecked) {
					paths.add(t.imagePath);
				}
			}
			return paths;
		}
		return null;
	}

	private Bitmap getBitmap(String path, int width, int height) {
		BitmapFactory.Options opts = null;
		opts = new BitmapFactory.Options();
		opts.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(path, opts);
		final int minSideLength = Math.min(width, height);
		opts.inSampleSize = computeSampleSize(opts, minSideLength, width * height);
		opts.inJustDecodeBounds = false;
		opts.inInputShareable = true;
		opts.inPurgeable = true;
		return BitmapFactory.decodeFile(path, opts);
	}
	
	private static int computeSampleSize(BitmapFactory.Options options,
			int minSideLength, int maxNumOfPixels) {
		int initialSize = computeInitialSampleSize(options, minSideLength,
				maxNumOfPixels);
		int roundedSize;
		if (initialSize <= 8) {
			roundedSize = 1;
			while (roundedSize < initialSize) {
				roundedSize <<= 1;
			}
		} else {
			roundedSize = (initialSize + 7) / 8 * 8;
		}
		return roundedSize;
	}

	private static int computeInitialSampleSize(BitmapFactory.Options options,
			int minSideLength, int maxNumOfPixels) {
		double w = options.outWidth;
		double h = options.outHeight;

		int lowerBound = (maxNumOfPixels == -1) ? 1 : (int) Math.ceil(Math
				.sqrt(w * h / maxNumOfPixels));
		int upperBound = (minSideLength == -1) ? 128 : (int) Math.min(
				Math.floor(w / minSideLength), Math.floor(h / minSideLength));
		if (upperBound < lowerBound) {
			return lowerBound;
		}
		if ((maxNumOfPixels == -1) && (minSideLength == -1)) {
			return 1;
		} else if (minSideLength == -1) {
			return lowerBound;
		} else {
			return upperBound;
		}
	}
	
	private class MyImageLoader extends AsyncTask<String, Void, Bitmap> {
		private int mPosition;
		private ViewHolder mHolder;
		
		public MyImageLoader(int position, ViewHolder holder){
			mPosition = position;
			mHolder = holder;
		}
		
		@Override
		protected Bitmap doInBackground(String... params) {
			if(mHolder.position == mPosition){
				return getBitmap(params[0], 220, 220);
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Bitmap result) {
			super.onPostExecute(result);
			if(result != null && mHolder.position == mPosition){
				mHolder.imageView.setImageBitmap(result);
			}
		}
		
	}
}
