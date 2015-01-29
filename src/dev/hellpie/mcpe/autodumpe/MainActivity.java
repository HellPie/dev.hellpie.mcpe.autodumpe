package dev.hellpie.mcpe.autodumpe;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.Toast;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends Activity implements OnClickListener {

	// Define a tag for LogCat
	private static final String TAG = "AutoDumPE";
	
	// Create all the UI elements
	CheckBox DumpSymCB;
	CheckBox DumpFuncsCB;
	CheckBox DumpAsmCB;
	
	RadioButton UseInternalBinRB;
	RadioButton UseAIDEBinRB;
	
	Button DumpButton;
	
	ProgressDialog DumpingDialog;
	
	// Create a new Context
	public Context mContext;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)	{
        super.onCreate(savedInstanceState);
		
        setContentView(R.layout.main);
		
		// Assign UI Elements to XML IDs
		DumpSymCB = (CheckBox) findViewById(R.id.DumpSymbolsCB);
		DumpFuncsCB = (CheckBox) findViewById(R.id.DumpFunctionsCB);
		DumpAsmCB = (CheckBox) findViewById(R.id.DumpAssemblyCB);
		
		UseInternalBinRB = (RadioButton) findViewById(R.id.UseInternalBinaryRB);
		UseAIDEBinRB = (RadioButton) findViewById(R.id.UseAIDEBinaryRB);
		
		DumpButton = (Button) findViewById(R.id.DumpButton);
		
		// Disable non working UI Element
		UseAIDEBinRB.setEnabled(false);
		
		// Enable Default Config
		UseInternalBinRB.setChecked(true);
		DumpSymCB.setChecked(true);
		
		
		// Run the extractor on first run only
		if(isFirstRun() == true) {
			BinaryExtractor mExtractor = new BinaryExtractor();
			mExtractor.execute("");
		}
		
		// Run MCPE Installation check
		new Runnable() {

			@Override
			public void run() {
									
				// Check if MCPE is installed
				try {
					
					PackageInfo mcPkgInfo = getPackageManager().getPackageInfo("com.mojang.minecraftpe", 0);
				
				} catch(PackageManager.NameNotFoundException e) {
					
					// MCPE looks not here. Tell the user
					Toast.makeText(getApplicationContext(), "Looks like MC:PE is not installed.\nYou need to install MC:PE to use this App.\nThe application will now exit.", Toast.LENGTH_SHORT);

					// Make this app commit suicide
					android.os.Process.killProcess(android.os.Process.myPid());

					// Exit with return 1 (usually means error happened)
					System.exit(1);
					
				}

			}

		}.run();
		
		// Assign OnClickListeners
		DumpButton.setOnClickListener(this);
		
	}	
	
	/* Called when a button pointing to it is pressed */
	@Override
	public void onClick(View view) {
		
		DumPEDumper mDumper = new DumPEDumper();
		mDumper.execute("");
		
		
	}
	
	// Bool to store if the back button was pressed once
	boolean doubleBackPressedOnce = false;
	
	// Double press back to exit code
	@Override
	public void onBackPressed() {
		
		// Check if pressed once
		if(doubleBackPressedOnce) {
			
			// Let the system exit for us
			super.onBackPressed();
			return;
			
		}
		
		// First press, set pressed once to true and warn the user
		doubleBackPressedOnce = true;
		Toast.makeText(getApplicationContext(), "Press the back button again to exit", Toast.LENGTH_SHORT).show();
		
		// Undo if more than 2 seconds passed
		new Handler().postDelayed(new Runnable() {
			
			@Override
			public void run() {
				doubleBackPressedOnce = false;
			}
			
		}, 2000);
		
	}
	
	// First run checker
	private boolean isFirstRun() {
		
		// Get an handler for shared XML preferences
		SharedPreferences mPrefs = MainActivity.this.getPreferences(Context.MODE_PRIVATE);
		
		// Read first run pref and if it does not exist return true
		boolean firstRunPref = mPrefs.getBoolean("isFirstRun", true);
		
		if(firstRunPref == true) {
			
			// This means this is the first run since
			// no config value was found
			
			// Save the isFirstRun value as false so we
			// do not run useless code twice
			SharedPreferences.Editor mPrefEdit = mPrefs.edit();
			mPrefEdit.putBoolean("isFirstRun", false);
			mPrefEdit.commit(); // Remember to save changes you idiot (@ HellPie)
			
			// Return true, we still need to tell this time is the first one
			return true;
			
		} else {
			
			// Looks like this is not the first time
			
			return false;
			
		}
		
	}
	
	// Create an internal class to manage AsyncStuff
	private class BinaryExtractor extends AsyncTask<String, Void, String> {

		// Define a new AlertDialog here so we can access it from anywhere in here
		ProgressDialog ExtractionDialog;
		
		@Override
		protected void onPreExecute() {
			
			// Setup and show the ProgressDialog
			ExtractionDialog = new ProgressDialog(MainActivity.this);
			
			ExtractionDialog.setTitle("Extracting Data");
			ExtractionDialog.setMessage("Extracting objdummp binary.\nPlease Wait...");
			ExtractionDialog.setCancelable(false);
			ExtractionDialog.setIndeterminate(true);
			
			// Ended setup, show dialog
			ExtractionDialog.show();
			
		}

		@Override
		protected String doInBackground(String... args) {
			
			// Locate the binary file
			InputStream binaryIStream = MainActivity.this.getResources().openRawResource(R.raw.objdump);
			
			// Try to store the file
			try {
				
				byte[] buffer = new byte[binaryIStream.available()];
			
				// Store binary into buffer
				binaryIStream.read(buffer);
				
				// End the stream of bits
				binaryIStream.close();
				
				// Create an output stream to app data
				FileOutputStream binaryOStream = MainActivity.this.getApplicationContext().openFileOutput("objdump", Context.MODE_PRIVATE);
				
				// Output raw data into the final file
				binaryOStream.write(buffer);
				
				// Close the stream
				binaryOStream.close();
				
				// Get the file and set it to executable
				File mFile = getFileStreamPath("objdump");
				mFile.setExecutable(true);
				
			} catch(IOException e) {
				
				// Log IOException error
				Log.e(TAG, "Error while extracting objdump binary.", e);
				
			}
			
			// Sleep for a couple seconds so the Dialog does not glitch
			try { Thread.sleep(1000); } catch(InterruptedException e) {}
			
			// Done, exit.
			
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
		
			// Close the Dialog
			ExtractionDialog.dismiss();
			
		}
	}
	
	// Create another AsyncTask based class to handle objdump work.
	private class DumPEDumper extends AsyncTask<String, String, String> {
		
		// Create a ProgressDialog here so all member within this AT can access it
		ProgressDialog DumpyDialog;
		
		// Store here objdump path so we can access it everywhere within this AT
		String objdumpPath = getFilesDir().toString() + "objdump";
		
		@Override
		protected void onPreExecute() {
			
			// Config and show the PrpgressDialog
			DumpyDialog = new ProgressDialog(MainActivity.this);
			
			DumpyDialog.setTitle("Auto Dumping in progress...");
			DumpyDialog.setMessage("I am working on it, shut up for a second!");
			DumpyDialog.setCancelable(false);
			DumpyDialog.setIndeterminate(true);
			
			// Show the dialog
			DumpyDialog.show();
		}
		
		@Override
		protected String doInBackground(String... args) {
			
			// Define: packageinfo + lib path + lib path and name
			PackageInfo mcApkInfo;
			String mcLibDir;
			String mcLibPath;

			// Define a counter to fix the library already existing error later on
			int appLibCounter = 0;
			
			// Get MCPE package infos
			try {

				// Store all infos in mcApkInfo
				mcApkInfo = getPackageManager().getPackageInfo("com.mojang.minecraftpe", 0);

				// Get Lib location
				mcLibDir = mcApkInfo.applicationInfo.nativeLibraryDir;

				// Generate lib path
				mcLibPath = mcLibDir + "/libminecraftpe.so";
				
				// Create a file from the full path of the library
				File mcLibFile = new File(mcLibPath);
				
				// Check for file missing
				if(!mcLibFile.exists() || mcLibFile.isDirectory()) {
					
					// Log that the file is either missing or a directory
					Log.e(TAG, "Library file is either missing in MCPE library folder or the path links to a directory.");
					
				}
				
				// Library file is present, we are good to go
				
				// Define an input and an output stream, both undeclared
				FileInputStream mcLibIS = null;
				FileOutputStream appLibOS = null;
				
				// Try to copy the file
				try {
					
					// Define the input stream
					mcLibIS = new FileInputStream(mcLibFile);
					
					// Define a new File
					File appLibFile = new File(getFilesDir() + "/libminecraftpe.so");
					
					// Check for file existance
					while(appLibFile.exists() && !appLibFile.delete()) {
						
						// Log that the app was not able to delete the file
						Log.e(TAG, "Unable to remove " + appLibFile.getName() + " from app files directory.");
						
						// Log that the file is already there
						Log.w(TAG, appLibFile.getName() + " is already present in app files directory.");
						
						// Increment file naming counter
						appLibCounter++;
						
						// Change file name with a new one
						appLibFile = new File(getFilesDir() + "/libminecraftpe-" + appLibCounter + ".so");
						
					}
					
					// Log the name of the file we are saving to
					if(appLibCounter > 0) {
						
						// We used the file naming trick, use libminecraftpe-
						Log.i(TAG, "Library will be save to libminecraftpe-" + appLibCounter + ".so");
					
					} else {
						
						// No tricks here, use libminecraftpe
						Log.i(TAG, "Library will be saved to libminecraftpe.so");
						
					}
					
					// Assign the output file to the output file stream
					appLibOS = new FileOutputStream(appLibFile);
					
					// Try to transfer the files
					try {
						// Create a buffer of 64KB to store each block of the file
						byte[] buffer = new byte[64 * 1024];
						
						// Create a counter to store how many bytes are in the buffer
						int bytesRead;
						
						// Loop untill buffer is empty
						while((bytesRead = mcLibIS.read(buffer)) >= 0) {
							
							// Write buffer to output stream
							appLibOS.write(buffer, 0, bytesRead);
							
						}
					
					} catch(IOException e) {
						
						// Log an error during copy
						Log.e(TAG, "There was an error during the copy of the library.");
						
					}
					
				} catch(FileNotFoundException e) {
					
					// We already loggged the library file is missing in MCPE library folder.
					
				} finally {
					
					// Try to close all the streams
					try{
						
						// If input stream is not empty...
						if(mcLibIS != null) {
							
							// ... then close it
							mcLibIS.close();
							
						}
						
						// If output stream is not empty...
						if(appLibOS != null) {
							
							// ... then close it
							appLibOS.close();
							
						}
						
					} catch(IOException e) {
						
						// Log that there was an error while closing streams
						Log.w(TAG, "There was an error while closing streams.");
						
					}	
				}
				
			} catch(PackageManager.NameNotFoundException e) {

				// Looks like MCPE is not installed and our
				// first check failed... meh, ragequit.

				// Tell the user
				Toast.makeText(getApplicationContext(), "Looks like MC:PE is not installed.\nInstall it from the Play Store\nto use this application.", Toast.LENGTH_SHORT).show();

				// Commit suicide
				android.os.Process.killProcess(android.os.Process.myPid());

				// Exit with error
				System.exit(1);

			}
			
			// Check if we need to dump symbols
			if(DumpSymCB.isChecked()) {
				
				// We do neet to.
				
				// Publish progress
				publishProgress("Dumping Symbols...");
				
				// Create a blank process for later use
				Process odProcess =  null;
				
				// Create a string to define library name
				String appLibName = "libminecraftpe.so";
				
				// TODO: REFACTOR THIS SHIT
				// Try to remove the existing output file if it exists
				File existingFile = new File(getFilesDir() + "/" + "Symbols.txt");
				if(existingFile.exists()) {
					if(existingFile.delete()) {
						Log.e(TAG, "Deleted already existing symbols file.");
					} else {
						Log.e(TAG, "Failed deleting already existing symbols file.");
					}
				}
				
				
				// Try to run objdump
				try {
					
					// Check what file name has the library now
					if(appLibCounter > 0) {
						
						// We had to hardfix the file existing issue, fix the name of the library
						appLibName = "libminecraftpe-" + appLibCounter + ".so";
						
					}
					
					// Tell the process to run a command
					odProcess = Runtime.getRuntime().exec(getFilesDir().toString() + "/objdump -Tt " + getFilesDir().toString() + "/" + appLibName);
					
					// Create an input and an output stream
					InputStream odIS = odProcess.getInputStream();
					FileOutputStream odOS = new FileOutputStream(getFilesDir() + "/Symbols.txt");
					
					// Try to run objdump
					try {
						// Create a buffer of 64KB to store each block of the stdout stream
						byte[] buffer = new byte[64 * 1024];

						// Create a counter to store how many bytes are in the buffer
						int bytesRead;

						// Loop untill buffer is empty
						while((bytesRead = odIS.read(buffer)) >= 0) {

							// Write buffer to output stream
							odOS.write(buffer, 0, bytesRead);

						}

					} catch(IOException e) {

						// Log an error during copy
						Log.e(TAG, "There was an error during the execution of objdump.");

					} finally {
						
						// Try to close the streams
						
						// If input stream is not empty...
						if(odIS != null) {

							// ... then close it
							try {
								
								odIS.close();
							
							} catch(IOException e) {
								
								// Something went wrong
								Log.e(TAG, "Unable to close the InputStream for objdump process.");
								
							}

						}

						// If output stream is not empty...
						if(odOS != null) {

							try {
								
								// ... then close it
								odOS.close();
								
							} catch(IOException e) {
								
								// Something went wrong
								Log.e(TAG, "Something went wrong while closing OutputStream for objdump process.");
								
							}
						}
					}
					
				} catch(IOException e) {}
			}
			
			// Check if we need to dump functions
			if(DumpFuncsCB.isChecked()) {
				
				// We do need too
				
				// Publish progress
				publishProgress("Dumping Functions...");

				// Create a blank process for later use
				Process odProcess =  null;

				// Create a string to define library name
				String appLibName = "libminecraftpe.so";

				// TODO: REFACTOR THIS SHIT
				// Try to remove the existing output file if it exists
				File existingFile = new File(getFilesDir() + "/" + "Functions.txt");
				if(existingFile.exists()) {
					if(existingFile.delete()) {
						Log.e(TAG, "Deleted already existing functions file.");
					} else {
						Log.e(TAG, "Failed deleting already existing functions file.");
					}
				}

				// Try to run objdump
				try {

					// Check what file name has the library now
					if(appLibCounter > 0) {

						// We had to hardfix the file existing issue, fix the name of the library
						appLibName = "libminecraftpe-" + appLibCounter + ".so";

					}

					// Tell the process to run a command
					odProcess = Runtime.getRuntime().exec(getFilesDir().toString() + "/objdump -TC " + getFilesDir().toString() + "/" + appLibName);

					// Create an input and an output stream
					InputStream odIS = odProcess.getInputStream();
					FileOutputStream odOS = new FileOutputStream(getFilesDir() + "/Functions.txt");

					// Try to run objdump
					try {
						// Create a buffer of 64KB to store each block of the stdout stream
						byte[] buffer = new byte[64 * 1024];

						// Create a counter to store how many bytes are in the buffer
						int bytesRead;

						// Loop untill buffer is empty
						while((bytesRead = odIS.read(buffer)) >= 0) {

							// Write buffer to output stream
							odOS.write(buffer, 0, bytesRead);

						}

					} catch(IOException e) {

						// Log an error during copy
						Log.e(TAG, "There was an error during the execution of objdump.");

					} finally {

						// Try to close the streams

						// If input stream is not empty...
						if(odIS != null) {

							// ... then close it
							try {

								odIS.close();

							} catch(IOException e) {

								// Something went wrong
								Log.e(TAG, "Unable to close the InputStream for objdump process.");

							}

						}

						// If output stream is not empty...
						if(odOS != null) {

							try {

								// ... then close it
								odOS.close();

							} catch(IOException e) {

								// Something went wrong
								Log.e(TAG, "Something went wrong while closing OutputStream for objdump process.");

							}
						}
					}

				} catch(IOException e) {}
				
			}
			
			// Check if we need to dump asm
			if(DumpAsmCB.isChecked()) {
				
				// We do need to
				
				// Publish progress
				publishProgress("Dumping Assembly... Eww.. This will be long...");

				// Create a blank process for later use
				Process odProcess =  null;

				// Create a string to define library name
				String appLibName = "libminecraftpe.so";

				// TODO: REFACTOR THIS SHIT
				// Try to remove the existing output file if it exists
				File existingFile = new File(getFilesDir() + "/" + "Assembly.txt");
				if(existingFile.exists()) {
					if(existingFile.delete()) {
						Log.e(TAG, "Deleted already existing assembly file.");
					} else {
						Log.e(TAG, "Failed deleting already existing assembly file.");
					}
				}

				// Try to run objdump
				try {

					// Check what file name has the library now
					if(appLibCounter > 0) {

						// We had to hardfix the file existing issue, fix the name of the library
						appLibName = "libminecraftpe-" + appLibCounter + ".so";

					}

					// Tell the process to run a command
					odProcess = Runtime.getRuntime().exec(getFilesDir().toString() + "/objdump -d " + getFilesDir().toString() + "/" + appLibName);

					// Create an input and an output stream
					InputStream odIS = odProcess.getInputStream();
					FileOutputStream odOS = new FileOutputStream(getFilesDir() + "/Assembly.txt");

					// Try to run objdump
					try {
						// Create a buffer of 64KB to store each block of the stdout stream
						byte[] buffer = new byte[64 * 1024];

						// Create a counter to store how many bytes are in the buffer
						int bytesRead;

						// Loop untill buffer is empty
						while((bytesRead = odIS.read(buffer)) >= 0) {

							// Write buffer to output stream
							odOS.write(buffer, 0, bytesRead);

						}

					} catch(IOException e) {

						// Log an error during copy
						Log.e(TAG, "There was an error during the execution of objdump.");

					} finally {

						// Try to close the streams

						// If input stream is not empty...
						if(odIS != null) {

							// ... then close it
							try {

								odIS.close();

							} catch(IOException e) {

								// Something went wrong
								Log.e(TAG, "Unable to close the InputStream for objdump process.");

							}

						}

						// If output stream is not empty...
						if(odOS != null) {

							try {

								// ... then close it
								odOS.close();

							} catch(IOException e) {

								// Something went wrong
								Log.e(TAG, "Something went wrong while closing OutputStream for objdump process.");

							}
						}
					}

				} catch(IOException e) {}
				
			}
			
			// Publish progress
			publishProgress("Copying all dumped files to internal storage...");

			// TODO: Temp code to copy from data/data to sdcard
			int i = 0;
			File syms = new File(getFilesDir() + "/Symbols.txt");
			File funcs = new File(getFilesDir() + "/Functions.txt");
			File asm = new File(getFilesDir() + "/Assembly.txt");
			String fName = "ShouldNotBeHere.txt";
			String appName = "AutoDumPE";
			Utils x = new Utils();
			
			if(syms.exists()) {
				fName = "Symbols.txt";
				try {
					if(x.copyToInternal(syms, fName, appName)) {
						Log.i(TAG, "Copy of " + syms.toString() + " done.");
					} else {
						Log.e(TAG, "Copy of " + syms.toString() + " failed");
					}
				} catch(IOException e) {
					Log.e(TAG, "Unable to copy " + syms.toString());
				}
			}

			if(funcs.exists()) {
				fName = "Functions.txt";
				try {
					if(x.copyToInternal(funcs, fName, appName)) {
						Log.i(TAG, "Copy of " + funcs.toString() + " completed.");
					} else {
						Log.e(TAG, "Copy of " +  funcs.toString() + " failed.");
					}
				} catch(IOException e) {
					Log.e(TAG, "Unable to copy " + funcs.toString());
				}
			}

			if(asm.exists()) {
				fName = "Assembly.txt";
				try {
					if(x.copyToInternal(asm, fName, appName)) {
						Log.i(TAG, "Copy of " + asm.toString() + " completed");
					} else {
						Log.e(TAG, "Copy of " + asm.toString() + " failed.");
					}
				} catch(IOException e) {
					Log.e(TAG, "Unable to copy " + asm.toString());

				}
			}
			
			return null;
		}

		@Override
		protected void onProgressUpdate(String[] values) {
			
			// Set DumpyDialog's text to a custom one
			// given in input here
			DumpyDialog.setMessage(values[0]);
			
		}

		@Override
		protected void onPostExecute(String result) {
			
			// Done derping? K. Hide the alert dialog
			DumpyDialog.dismiss();
			
			// TODO: File Editor/Reader/Viewver/Whatever

		}
	}
}
