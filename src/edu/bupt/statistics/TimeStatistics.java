package edu.bupt.statistics;

public class TimeStatistics {
	private static final String TAG = TimeStatistics.class.getSimpleName();
	
	public static long uploadStartTime;
	public static long uploadCompleteTime;
	public static long reconstructStartTime;
	public static long reconstructCompleteTime;
	public static long downloadStartTime;
	public static long downloadCompleteTime;
	public static long getUploadStartTime() {
		return uploadStartTime;
	}
	public static void setUploadStartTime(long uploadStartTime) {
		TimeStatistics.uploadStartTime = uploadStartTime;
	}
	public static long getUploadCompleteTime() {
		return uploadCompleteTime;
	}
	public static void setUploadCompleteTime(long uploadCompleteTime) {
		TimeStatistics.uploadCompleteTime = uploadCompleteTime;
	}
	public static long getReconstructStartTime() {
		return reconstructStartTime;
	}
	public static void setReconstructStartTime(long reconstructStartTime) {
		TimeStatistics.reconstructStartTime = reconstructStartTime;
	}
	public static long getReconstructCompleteTime() {
		return reconstructCompleteTime;
	}
	public static void setReconstructCompleteTime(long reconstructCompleteTime) {
		TimeStatistics.reconstructCompleteTime = reconstructCompleteTime;
	}
	public static long getDownloadStartTime() {
		return downloadStartTime;
	}
	public static void setDownloadStartTime(long downloadStartTime) {
		TimeStatistics.downloadStartTime = downloadStartTime;
	}
	public static long getDownloadCompleteTime() {
		return downloadCompleteTime;
	}
	public static void setDownloadCompleteTime(long downloadCompleteTime) {
		TimeStatistics.downloadCompleteTime = downloadCompleteTime;
	}
	
}
