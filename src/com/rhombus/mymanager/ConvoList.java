package com.rhombus.mymanager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import com.rhombus.mymanager.SimpleGestureFilter.SimpleGestureListener;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RelativeLayout.LayoutParams;

public class ConvoList extends Activity implements SimpleGestureListener {
	
	private SimpleGestureFilter detector;
	String myPath = "/DATA/MyManager/";
	String folderPath = "";
	String fullPath;
	
	final String endString = "|end|";
	final String colorString = "|color|";
	final String doneCopyingString = "|Done_cOpYiNg|";
	String dateString;
	
	int FONT_SIZE = 15;
	int BGcolor[] = {Color.WHITE, Color.RED, Color.BLACK};
	int FGcolor[] = {Color.BLACK, Color.WHITE, Color.WHITE};
	
	String todayFileName;
	Calendar currentDate;
	
    private ArrayList<MyFile> fileList = new ArrayList<MyFile>();
    
	@SuppressLint("NewApi")
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
    	setContentView(R.layout.my_manager_list);
    	
    	//Detect touched area
    	Display display = getWindowManager().getDefaultDisplay();
        float screenX;
    	if(android.os.Build.VERSION.SDK_INT >= 13) {
            Point size = new Point();
        	display.getSize(size);
        	screenX = size.x;
        } else {
			screenX = display.getWidth();
        }
        detector = new SimpleGestureFilter(this,this,screenX);

        //Get the local and use for selecting correct Date Format
        String local = Locale.getDefault().toString();
        if((Locale.UK.toString()).equals(local)) dateString = "dd/MM/yyyy - \nEEEE";
        else if((Locale.US.toString()).equals(local)) dateString = "MM/dd/yyyy - \nEEEE";
        else dateString = "MM/dd/yyyy - \nEEEE";
        
    	currentDate = Calendar.getInstance();
    	todayFileName = DateFormat.format("yyyyMMdd", currentDate).toString();
    	currentDate.set(Calendar.MONTH, getIntent().getIntExtra("currentMonth", currentDate.get(Calendar.MONTH)));
    	currentDate.set(Calendar.YEAR, getIntent().getIntExtra("currentYear", currentDate.get(Calendar.YEAR)));
    	getUserPreferences();
    	fullPath = Environment.getExternalStorageDirectory().getAbsolutePath() + myPath + folderPath;
        
    	if(isExternalStorageReadable()) {
    		getMonthFiles(0);
    		LinearLayout lView = (LinearLayout) findViewById(R.id.allConversations);
    		TextView tv = new TextView(ConvoList.this); tv.setText("Sample"); tv.setBackgroundColor(Color.RED);
    		lView.addView(tv);
    		resetLView();
    	}
	}
	
	private void getUserPreferences() {
    	SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    	int originalFontSize = Integer.parseInt(sharedPrefs.getString("prefFontSize", "20"));
    	if(originalFontSize == 20) FONT_SIZE = 15;
    	else if(originalFontSize == 15) FONT_SIZE = 12;
    	else if(originalFontSize == 25) FONT_SIZE = 20;
    	else FONT_SIZE = originalFontSize * (3/4);
    	
    	folderPath = sharedPrefs.getString("prefFolder", "Default");
    	if("Default".equals(folderPath)) folderPath = "";
    }
	
	@Override
    public void onConfigurationChanged(Configuration newConfig) {
    	super.onConfigurationChanged(newConfig);
    	setContentView(R.layout.my_manager_list);
    	resetLView();
    }
	
	public void onDestroy() {
		super.onDestroy();
	}
	
	private void resetLView() {
		LinearLayout lView = (LinearLayout)findViewById(R.id.allConversations);
		lView.removeAllViews();
		
		TextView myDate = new TextView(ConvoList.this);
		myDate.setText(DateFormat.format("MMMM", currentDate).toString());
		myDate.setTextSize(FONT_SIZE + 10);
        myDate.setGravity(Gravity.LEFT);
        myDate.setBackgroundColor(BGcolor[2]);
		myDate.setTextColor(FGcolor[2]);
		lView.addView(myDate);
		
		for(MyFile mf: fileList) {
			mf.addViewTo(lView);
		}
	}
	
	/**TODO: USE THIS CODE SOMEWHERE ELSE: for getting all valid files in directory
    int lastPosition = 0;
    private void getAllFiles() {
    	File sdCardRoot = Environment.getExternalStorageDirectory();
    	File dir = new File(sdCardRoot, myPath);
    	File f[] = dir.listFiles();
    	int i = lastPosition;
    	for(; i < 30 + lastPosition && i < f.length; i++) {
    		if (f[i].isFile()) {
    	        String fileName = f[i].getName();
    	        Date fileDate = convertStringToDate(fileName);
    	        if(fileDate != null) {
    	        	fileList.add(new MyFile(fileName, fileDate));
    	        }
    	    }
    	}
    	lastPosition = i;
    }
    */
	
	
    private void getMonthFiles(int monthGap) {
    	fileList.clear();
    	currentDate.set(Calendar.DATE, 1);
    	currentDate.set(Calendar.MONTH, currentDate.get(Calendar.MONTH) + monthGap);
    	Calendar temp = currentDate;
    	int maxDays = temp.getActualMaximum(Calendar.DAY_OF_MONTH);
    	for(int i = 1; i <= maxDays; i++) {
    		temp.set(Calendar.DAY_OF_MONTH, i);
    		String fileName = DateFormat.format("yyyyMMdd", temp).toString();
    		fileList.add(new MyFile(fileName));
    	}
    }
    
    
    //FOR CHECKING SWIPE GESTURES
    @Override
    public boolean dispatchTouchEvent(MotionEvent me){
    	//Call onTouchEvent of SimpleGestureFilter class
    	this.detector.onTouchEvent(me);
    	return super.dispatchTouchEvent(me);
    }

	@Override
	public void onSwipe(int direction) {
		switch (direction) {
    	case SimpleGestureFilter.SWIPE_LEFT : getMonthFiles(1); resetLView(); break; //nextMonth
    	case SimpleGestureFilter.SWIPE_RIGHT : getMonthFiles(-1); resetLView(); break; //prevMonth
    	}
	}

	@Override
	public void onDoubleTap() {
		//DOUBLE TAPS RETURN FALSE IN SIMPLEGESTUREFILTER
    	//Toast.makeText(this, "Double Tap", Toast.LENGTH_SHORT).show();
	}
    
    
    public class MyFile {
    	private File file;
    	private String fileName;
    	private Calendar fileDate;
    	private String info = "";
    	private RelativeLayout relative = null;
    	
    	public MyFile(String myFileName) {
    		fileName = myFileName;
    		fileDate = convertStringToDate(fileName);
    		file = makeMessageStoreFile(fileName);
    	}
    	public MyFile(File myFile) {
    		file = myFile;
    		fileName = file.getName();
    		fileDate = convertStringToDate(fileName);
    	}
    	public MyFile(String myFileName, Calendar myFileDate) {
    		fileName = myFileName;
    		fileDate = myFileDate;
    		file = makeMessageStoreFile(fileName);
    	}
    	
    	public File getFile() { return file; }
    	public String getFileName() { return fileName; }
    	public Calendar getDate() { return fileDate; }
    	public String getInfo() { return info; }
    	
    	public void setInfo() {
    		BufferedReader br = null;
    		try {
    			br = new BufferedReader(new FileReader(file));
    		} catch (FileNotFoundException e) {
    			e.printStackTrace();
    		}
        	if(br != null && file.length() > 0) {
        		StringBuilder text = new StringBuilder();
    	    	String line = null;
    	    	String ls = System.getProperty("line.separator");
    			int endLength = 75;
    			
    	    	try {
    				int i=0;
    				while ((line = br.readLine()) != null) {
    					if(line.endsWith(endString)) {
    						info = info + ls + line.toString().substring(colorString.length() + 1, line.length() - endString.length());
    						i++;
    						if(i == 3 || info.length() > endLength)
    							break;
    					}
    				}
    				br.close();
    			} catch (IOException e) {
    				e.printStackTrace();
    			}
    			if(info.length() > endLength)
    				info = info.substring(0, endLength) + "...";
        	} else {
        		info = "-N/A-";
        	}
    	}

    	private void setRelativeClickListener() {
    		View.OnClickListener relativeClickListener = new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent returnIntent = new Intent();
					returnIntent.putExtra("nextFile", fileName);
					setResult(RESULT_OK,returnIntent);
					finish();
				}
    		};
    		relative.setOnClickListener(relativeClickListener);
    	}
    	
    	public void addViewTo(LinearLayout lView) {
    		if(relative == null) {
    			setInfo();
        		relative = new RelativeLayout(ConvoList.this);
        		TextView tV = new TextView(ConvoList.this);
        		TextView tV2 = new TextView(ConvoList.this);
        		tV.setText(DateFormat.format(dateString, fileDate).toString() + " : ");
        		tV2.setText(info);
        		
        		int i = 0;
        		if(todayFileName.equals(fileName)) i = 1;
        		else if(!file.exists()) i = 2;
        		
        		tV.setBackgroundColor(BGcolor[i]); tV.setTextColor(FGcolor[i]);
        		tV.setTextSize(FONT_SIZE + 3); tV.setGravity(Gravity.LEFT); 
        		tV2.setBackgroundColor(BGcolor[i]); tV2.setTextColor(FGcolor[i]);
        		tV2.setTextSize(FONT_SIZE); tV2.setGravity(Gravity.LEFT);
        		
        		tV.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)); 
        		tV.setId(info.length());
        		RelativeLayout.LayoutParams tVLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT); 
    	        tVLayoutParams.addRule(RelativeLayout.RIGHT_OF, tV.getId());
        		tV2.setLayoutParams(tVLayoutParams);
        		relative.addView(tV);
        		relative.addView(tV2);
        		relative.setBackgroundColor(BGcolor[i]);
        		
        		setRelativeClickListener();
    		}
    		lView.addView(relative);
    	}
	}
    
    private Calendar convertStringToDate(String dateString) {
    	SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        Date date = new Date();
        try {
            date = dateFormat.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        	return null;
        }
        Calendar convertedDate = Calendar.getInstance();
        convertedDate.setTime(date);
        return convertedDate;
    }
    
    

    //Checks if external storage is available for read and write
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        Toast.makeText(ConvoList.this, "Can't write to SD card. Session won't be stored.", Toast.LENGTH_LONG).show();
        return false;
    }

    //Checks if external storage is available to at least read
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
            Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        Toast.makeText(ConvoList.this, "Can't read SD card. Exit app and mount SD card.", Toast.LENGTH_LONG).show();
        return false;
    }
    
    //Creates and returns a file in path with name fileName
    public File makeMessageStoreFile(String fileName) {
    	if(isExternalStorageWritable()) {
    		File path = new File(fullPath);
    		path.mkdirs();
    		File f = new File(path, fileName);
    		return f;
    	} else {
    		Toast.makeText(ConvoList.this, "SD Card not connected. Session won't be stored for future.", Toast.LENGTH_LONG).show();
    		return null;
    	}
    }
    
    private void ToastIt(String s) { Toast.makeText(ConvoList.this, s, Toast.LENGTH_SHORT).show(); }
    private void ToastItLong(String s) { Toast.makeText(ConvoList.this, s, Toast.LENGTH_LONG).show(); }
}
