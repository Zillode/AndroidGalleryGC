package simple.gallery.gallerygarbagecollector;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Toast;

public class MainActivity extends Activity {

	private static Activity activity;
	private static ArrayList<FileWithTime> cleanFiles;
	private static SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yy HH:mm:ss", Locale.ENGLISH);
	private static String dcim = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Environment.DIRECTORY_DCIM + "/";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		activity = this;
		if (cleanFiles == null) {
		}
		Button clean = (Button) findViewById(R.id.button1);
		clean.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				DatePicker date = (DatePicker) findViewById(R.id.datePicker1);
				Calendar c = Calendar.getInstance();
				c.set(Calendar.YEAR, date.getYear());
	            c.set(Calendar.MONTH, date.getMonth());
	            c.set(Calendar.DAY_OF_MONTH, date.getDayOfMonth());
				showCleanFilesDialog(c.getTimeInMillis());
			}
		});
		showInformationDialog();
	}
	
	@Override
	public void onStart() {
		super.onStart();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		activity = this;
	}
	
	@Override
	public void onPause() {
		super.onPause();
		activity = null;
	}
	
	private void showInformationDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("The Gallery Garbage Collector");
		builder.setMessage("This application removes old items from your gallery (i.e. pictures and videos)."+
							"\n\nWarning: this application comes without ANY warranty and we cannot help you if it accidentally removes other files as well." +
							"\n\n\nTHE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT OF THIRD PARTY RIGHTS. IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, OR ANY SPECIAL INDIRECT OR CONSEQUENTIAL DAMAGES, OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.");
		builder.setPositiveButton("OK", null);
		builder.setCancelable(false);
		builder.create().show();
	}
	
	public static String humanReadableByteCount(long bytes, boolean si) {
	    int unit = si ? 1000 : 1024;
	    if (bytes < unit) return bytes + " B";
	    int exp = (int) (Math.log(bytes) / Math.log(unit));
	    String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
	    return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}
	
	public void showCleanFilesDialog(long olderThanInDays) {
		cleanFiles = getFilesToClean(dcim, olderThanInDays);
	    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
	    final String[] items = new String[cleanFiles.size()];
	    for (int i = 0; i < cleanFiles.size(); i++)
	    	items[i] = cleanFiles.get(i).fileName.replace(dcim, "") + "\n("+sdf.format(cleanFiles.get(i).modifiedTime)+")";
	    final boolean[] itemsChecked = new boolean[cleanFiles.size()];
	    for (int i = 0; i < cleanFiles.size(); i++)
	    	itemsChecked[i] = true;
	    	builder.setTitle("Select files to remove")
	           .setMultiChoiceItems(items, itemsChecked,
	                      new DialogInterface.OnMultiChoiceClickListener() {
	               @Override
	               public void onClick(DialogInterface dialog, int which, boolean isChecked) { }
	           })
	           .setPositiveButton("Remove selected files", new DialogInterface.OnClickListener() {
	               @Override
	               public void onClick(DialogInterface dialog, int id) {
	            	   AlertDialog.Builder builder = new AlertDialog.Builder(activity);
	            	   int selectedFiles = 0;
	            	   long totalFileLength = 0;
	            	   for (int i = 0; i < itemsChecked.length; i++) {
	            		   if (itemsChecked[i]) {
	            			   selectedFiles++;
	            			   totalFileLength += cleanFiles.get(i).fileLength;
	            		   }
	            	   }
	            	   final int selectedFilesFinal = selectedFiles;
	            	   final long totalFileLengthFinal = totalFileLength;
	                   builder.setMessage("Are you sure to delete "+selectedFiles+" selected file(s)?\nThis will free up "+humanReadableByteCount(totalFileLengthFinal, true)+".")
	                          .setPositiveButton("Delete files", new DialogInterface.OnClickListener() {
	                              public void onClick(DialogInterface dialog, int id) {
	                            	  AlertDialog.Builder builder = new AlertDialog.Builder(activity);
	                            	  builder.setMessage("Are you sure REALLY SURE to delete "+selectedFilesFinal+" selected file(s)?")
	           	                          .setPositiveButton("YES, delete my files!", new DialogInterface.OnClickListener() {
	           	                              public void onClick(DialogInterface dialog, int id) {
				                                  for (int i = 0; i < itemsChecked.length; i++) {
				                                	  if (itemsChecked[i]) {
				                                		  FileWithTime fw = cleanFiles.get(i);
				                                		  File f = new File(fw.fileName);
				                                		  Log.v("GalleryGC", "Removing " + fw.fileName + " (" + fw.modifiedTime + ")");
				                                		  f.delete();
				                                	  }
				                                  }
				                            	  dialog.dismiss();
				                            	  MediaScannerConnection.scanFile(activity, new String[] { dcim }, null, new MediaScannerConnection.OnScanCompletedListener() {
				                                      public void onScanCompleted(String path, Uri uri) { }});
				                            	  Toast.makeText(activity, "Gallery garbage collected!", Toast.LENGTH_LONG).show();
	           	                              }
	           	                          })
	           	                          .setNegativeButton("Cancel", null);
	                            	  dialog.dismiss();
	                            	  builder.create().show();
	                              }
	                          })
	                          .setNegativeButton("Cancel", null);
	                   builder.create().show();
	               }
	           })
	           .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	               @Override
	               public void onClick(DialogInterface dialog, int id) { }
	           });
	    builder.show();
	}
	
	private class FileWithTime {
		String fileName; Long modifiedTime; Long fileLength;
		public FileWithTime(String fileName, long modifiedTime, long fileLength) {
			this.fileName = fileName;
			this.modifiedTime = modifiedTime;
			this.fileLength = fileLength;
		}
	}
	
	private ArrayList<FileWithTime> getFilesToClean(String path, long purgeTime) {
		Log.v("GalleryGC", "Looking for files with modified time < " + purgeTime);
		ArrayList<FileWithTime> oldFiles = new ArrayList<FileWithTime>();
		File file = new File(path);
		File[] allFiles = file.listFiles();
        for (File f : allFiles) {
        	if (f.isDirectory())
        		oldFiles.addAll(getFilesToClean(f.getAbsolutePath(), purgeTime));
        	else if (f.lastModified() < purgeTime) {
        		// Log.v("GalleryGC", "File will be GC'ed (last modified time: "+f.lastModified()+")");
        		oldFiles.add(new FileWithTime(f.getAbsolutePath(), f.lastModified(), f.length()));
            }
        }
        Collections.sort(oldFiles, new Comparator<FileWithTime>(){
        	  public int compare(FileWithTime f1, FileWithTime f2) {
        	    return f1.modifiedTime.compareTo(f2.modifiedTime);
        	  }
        });
        return oldFiles;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
	    case R.id.action_information:
	    	showInformationDialog();
	    	return true;
	    default:
	    	return super.onOptionsItemSelected(item);
		}
	}

}
