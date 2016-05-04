package edu.bupt.statistics;

public class TimeStatistics {
	private static final String TAG = TimeStatistics.class.getSimpleName();
	
	public static int uploadStartTime;
	public static int uploadCompleteTime;
	public static int reconstructStartTime;
	public static int reconstructCompleteTime;
	public static int downloadStartTime;
	public static int downloadCompleteTime;
	public static int getUploadStartTime() {
		return uploadStartTime;
	}
	public static void setUploadStartTime(int uploadStartTime) {
		TimeStatistics.uploadStartTime = uploadStartTime;
	}
	public static int getUploadCompleteTime() {
		return uploadCompleteTime;
	}
	public static void setUploadCompleteTime(int uploadCompleteTime) {
		TimeStatistics.uploadCompleteTime = uploadCompleteTime;
	}
	public static int getReconstructStartTime() {
		return reconstructStartTime;
	}
	public static void setReconstructStartTime(int reconstructStartTime) {
		TimeStatistics.reconstructStartTime = reconstructStartTime;
	}
	public static int getReconstructCompleteTime() {
		return reconstructCompleteTime;
	}
	public static void setReconstructCompleteTime(int reconstructCompleteTime) {
		TimeStatistics.reconstructCompleteTime = reconstructCompleteTime;
	}
	public static int getDownloadStartTime() {
		return downloadStartTime;
	}
	public static void setDownloadStartTime(int downloadStartTime) {
		TimeStatistics.downloadStartTime = downloadStartTime;
	}
	public static int getDownloadCompleteTime() {
		return downloadCompleteTime;
	}
	public static void setDownloadCompleteTime(int downloadCompleteTime) {
		TimeStatistics.downloadCompleteTime = downloadCompleteTime;
	}
	
}
