package dev.hellpie.mcpe.autodumpe;

import android.os.Environment;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Utils {
	
	private static final String TAG = "AutoDumPE - Utils"; // LogCat Tag
	private static String sdcardPath = Environment.getExternalStorageDirectory().toString(); // Public Internal Storage
	private String appName = "AutoDumPE";
	
	public boolean copyToInternal(File originalFile, String destinationFileName, String folderName) throws IOException {
		
		File destinationFile = new File(sdcardPath + "/" + folderName + "/" + destinationFileName);
		File destinationDir = new File(sdcardPath + "/" + folderName + "/");
		
		if(!originalFile.exists()) {
			Log.e(TAG, "Original File " + originalFile.toString() + " does not exist.");
			return false;
		}

		
		if(!destinationDir.exists()) {
			if(destinationDir.mkdir()) {
				Log.v(TAG, "Folder " + folderName + " not fount in sdcard path. It has been created.");
			} else {
				Log.e(TAG, "Unable to create Folder " + folderName + " in sdcard path. File " + destinationFile.getName() + " will be exported to sdcard root");
				destinationFile = new File(sdcardPath + "/" + destinationFileName);
			}
		} else {
			if(!destinationDir.canWrite()) {
				destinationDir.setWritable(true);
				if(!destinationDir.canWrite()) {
					Log.e(TAG, "Unable to write in " + destinationDir.toString());
					return false;
				}
			}
		}
		
		if(destinationFile.exists() && checkSameMD5(destinationFile, originalFile)) {
			Log.w(TAG, "Destination File " + destinationFile.toString() + " already exist but has same MD5 as the Original File.");
			return true;
		} else if(destinationFile.exists() && !checkSameMD5(destinationFile, originalFile)) {
			if(destinationFile.delete()) {
				Log.e(TAG, "Destination File " + destinationFile.toString() + " already exist. Deleted.");
			} else {
				Log.e(TAG, "Unable to delete already existing Destination File " + destinationFile.toString() + ", stopping.");
				return false;
			}
		}
		
		if(originalFile.length() > Integer.MAX_VALUE) {
			
			InputStream inputStream = null;
			OutputStream outputStream = null;
			
			try {
				
				inputStream = new FileInputStream(originalFile);
				outputStream = new FileOutputStream(destinationFile);
				byte[] buffer = new byte[33554432]; // 32MB
				int lenght;
				
				
				while((lenght = inputStream.read(buffer)) > 0) {
					outputStream.write(buffer, 0, lenght);
				}
				
			} catch(IOException e) {
				
				Log.e(TAG, "There was an error while copying the File using byte[] buffer method. Error is in the next log.");
				Log.e(TAG, e.toString());
				
			} finally {
				if(inputStream != null) {
					try{
						inputStream.close();
					} catch(IOException e) {
						Log.e(TAG, "There was an error while closing the input stream.");
					}
				}
				
				if(outputStream != null) {
					try{
						outputStream.close();
					} catch(IOException e) {
						Log.e(TAG, "There was an error while closing the output stream.");
					}
				}
			}
			
		} else {
			
			FileChannel inputChannel = null;
			FileChannel outputChannel = null;
			
			try{
				
				inputChannel = new FileInputStream(originalFile).getChannel();
				outputChannel = new FileOutputStream(destinationFile).getChannel();
				
				outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
				
			} catch(IOException e) {
				
				Log.e(TAG, "There was an error while copying from a FileChannel to another. Error is in next log output.");
				Log.e(TAG, e.toString());
				
			} finally {
				if(inputChannel != null) {
					try{
						inputChannel.close();
					} catch(IOException e) {
						Log.e(TAG, "There was an error while closing the input channel.");
					}
				}
				
				if(outputChannel != null) {
					try{
						outputChannel.close();
					} catch(IOException e) {
						Log.e(TAG, "There was an error while closing the output channel.");
					}
				}
			}
		}
		
		if(destinationFile.exists() && checkSameMD5(originalFile, destinationFile)) {
			return true;
		} else {
			if(destinationFile.exists()) {
				Log.e(TAG, "Original File and Destination File are not the same.");
			} else {
				Log.e(TAG, "Destination file does not exist.");
			}
			return false;
		}
	}

	public boolean copyToInternal(File originalFile, String destinationFileName) {
		try {
			return copyToInternal(originalFile, destinationFileName, "");
		} catch(IOException e) {
			Log.e(TAG, "IOException thrown by copyToInternal(" + originalFile.toString() + ", " + destinationFileName + "\"\"", e);
			return false;
		}
	}
	
	/*public boolean copyToInternal(File originalFile) {
		
		return false;
	}*/
	
	
	public String getMD5(File fileToCheck) {
		
		/* Thanks to Cyanogenmod 12's System Updater MD5 checksum */
		MessageDigest digest;
		try{
			digest = MessageDigest.getInstance("MD5");
		} catch(NoSuchAlgorithmException e) {
			Log.e(TAG, "Error while getting digest.");
			return null;
		}
		
		InputStream inputStream;
		try{
			inputStream = new FileInputStream(fileToCheck);
		} catch(FileNotFoundException e) {
			Log.e(TAG, "Exception while getting FileInputStream.");
			return null;
		}
		
		byte[] buffer = new byte[16 * 1024];
		int read;
		try{
			while((read = inputStream.read(buffer)) > 0) {
				digest.update(buffer, 0, read);
			}
			byte[] md5sum = digest.digest();
			BigInteger bigInt = new BigInteger(1, md5sum);
			String output = bigInt.toString(16);
			// Fill to 32 chars
			output = String.format("%32s", output).replace(' ', '0');
			Log.v(TAG, "MD5 of " + fileToCheck.toString() + " is " + output);
			return output;
		} catch(IOException e) {
			Log.e(TAG, "Unable to process file for MD5");
			return null;
		} finally {
			try {
				inputStream.close();
			} catch(IOException e) {
				Log.e(TAG, "Unable to close MD5 calculation stream.");
			}
		}
		
	}
	
	public boolean checkSameMD5(File fileToCompare1, File fileToCompare2) {
//		return getMD5(fileToCompare1) == getMD5(fileToCompare2);
		return getMD5(fileToCompare1).equalsIgnoreCase(getMD5(fileToCompare2));
	}
	
	public String getCurrentAppName() { return appName; }
	public void setCurrentAppName(String newAppName) { appName = newAppName; }
	
}
