package com.rhombus.mymanager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

import com.rhombus.mymanager.SimpleGestureFilter.SimpleGestureListener;

import android.text.format.DateFormat;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;

public class MyManager extends Activity implements SimpleGestureListener {
	
	private SimpleGestureFilter detector;
	private float screenX, screenY; //screen sizes
	private float touchX, touchY; //tap coordinates
	private int minTextViewHeight = 0;
	private int moveNote = -1;
	private MyClipboardManager clipper;
	String myPath = "/DATA/MyManager/";
	String folderPath = "";
	
	
	int FONT_SIZE = 20;
	int defBGcolor[] = {Color.BLACK, Color.WHITE, Color.RED, Color.GREEN, Color.BLUE, Color.DKGRAY, Color.LTGRAY, Color.YELLOW, Color.MAGENTA, Color.CYAN};
	ArrayList<Integer> BGcolor = new ArrayList<Integer>();
	ArrayList<Integer> FGcolor = new ArrayList<Integer>();
	
	final String endString = "|end|";
	final String colorString = "|color|";
	final String doneCopyingString = "|Done_cOpYiNg|";
	boolean doneCopying = false;
	boolean startsEmpty = true;
	String dateString;
	
	String currentFileName;
	Calendar currentDate;
	boolean fromConvoList = false;
	
	ArrayList<String> conversation = new ArrayList<String>();
	ArrayList<Integer> color = new ArrayList<Integer>();
	
    @SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    	setContentView(R.layout.my_manager_conversation);
    	
    	//Detect touched area
    	Display display = getWindowManager().getDefaultDisplay();
        if(android.os.Build.VERSION.SDK_INT >= 13) {
            Point size = new Point();
        	display.getSize(size);
        	screenX = size.x;
        	screenY = size.y;
        } else {
			screenX = display.getWidth();
        	screenY = display.getHeight();
        }
        minTextViewHeight = (int) (screenY / 15);
        detector = new SimpleGestureFilter(this,this,screenX);
        
        //Get the local and use for selecting correct Date Format
        String local = Locale.getDefault().toString();
        if((Locale.UK.toString()).equals(local)) dateString = "dd/MM/yyyy - EEE";
        else if((Locale.US.toString()).equals(local)) dateString = "MM/dd/yyyy - EEE";
        else dateString = "dd/MM/yyyy - EEE";
        
        //Creates a clipboard manager
        clipper = new MyClipboardManager();
    	
        FirstTimePreference prefFirstTime = new FirstTimePreference(getApplicationContext());
        if(prefFirstTime.runTheFirstTime("firstTimeKey")) {
        	Intent i = new Intent(getApplicationContext(), SettingsPage.class);
        	i.putExtra("firstTime", true);
        	startActivity(i);
        	finish();
        }
        
        EditText inputText = (EditText) findViewById(R.id.inputText);
        inputText.requestFocus();
        InputMethodManager inputManager = (InputMethodManager)MyManager.this.getSystemService(INPUT_METHOD_SERVICE);
        inputManager.restartInput(inputText);
        
    	if(isExternalStorageReadable()) {
    		getUserPreferences();
    		//Gets current day to display at start
    		currentDate = Calendar.getInstance();
        	String fileName = DateFormat.format("yyyyMMdd", currentDate).toString();
    		managerInit(fileName, currentDate);
    		managerStart();
    	}
    }
    
    private boolean getUserPreferences() {
    	SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    	FONT_SIZE = Integer.parseInt(sharedPrefs.getString("prefFontSize", "20"));
    	String oldFolderPath = folderPath;
    	folderPath = sharedPrefs.getString("prefFolder", "Default");
    	if("Default".equals(folderPath)) folderPath = "";
    	if(currentFileName != null && !folderPath.equals(oldFolderPath)) {
    		String temp = folderPath;
    		folderPath = oldFolderPath;
    		conversationToFile(currentFileName);
    		conversation.clear();
    		color.clear();
    		resetLView();
    		folderPath = temp;
    	}
    	
    	//GET THE COLORS
    	BGcolor.clear();
    	FGcolor.clear();
    	int addingColor;
    	for(int i=0; i<10; i++) {
    		addingColor = stringToColor(sharedPrefs.getString("prefColors"+i, "None"));
    		if(addingColor != 0) {
    			BGcolor.add(addingColor);
    			if(addingColor != Color.WHITE) FGcolor.add(Color.WHITE);
    			else FGcolor.add(Color.BLACK);
    		}
    	}
    	return true;
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	conversationToFile(currentFileName);
    	conversation.clear();
		color.clear();
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	conversationToFile(currentFileName);
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	if(isExternalStorageReadable()) {
			getUserPreferences();
    		if(!fromConvoList) {
        		//Gets current day to display at start
        		currentDate = Calendar.getInstance();
    			String fileName = DateFormat.format("yyyyMMdd", currentDate).toString();
    			managerInit(fileName, currentDate);
    		}
    		fromConvoList = false;
    		resetLView();
    	}
    }
    
    private void conversationToFile(String fileName) {
    	if(isExternalStorageWritable()) {
    		File file = makeMessageStoreFile(fileName);
    		if(!conversation.isEmpty()) {
    			PrintWriter pw = null;
				try {
					pw = new PrintWriter(file);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
    			if(pw != null) {
    				if(doneCopying) {
    					pw.println(doneCopyingString);
    				}
    				for(int i=0; i < conversation.size(); i++) {
	        			String message = null;
	        			message = conversation.get(i);
	        			pw.println(colorString + color.get(i).toString() + message + endString);
					}
    				pw.flush();
    				pw.close();
    			}
    		} else if(!startsEmpty && conversation.isEmpty()) {
    			file.delete();
    		}
    	}
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    	super.onConfigurationChanged(newConfig);
    	setContentView(R.layout.my_manager_conversation);
    	managerStart();
    }
    
    private void managerInit(String fileName, Calendar d) {
    	//Checks if file exists and gets all the existing conversation for the day.
    	conversation.clear();
		color.clear();
		doneCopying = false;
    	currentFileName = fileName;
    	currentDate = d;
    	final Calendar date = Calendar.getInstance();
        String todayFileName = DateFormat.format("yyyyMMdd", date).toString();
        
        File file = makeMessageStoreFile(fileName);
    	if(file.length() > 0) {
    		String fullConversation = fileToString(file);
    	    if(fullConversation.startsWith(doneCopyingString) && todayFileName.equals(fileName) || !todayFileName.equals(fileName)) {
    	    	//EITHER ALREADY COPIED YESTERDAYS OR FILE ISN'T TODAY YET
    	    	if(fullConversation.startsWith(doneCopyingString)) {
    	    		doneCopying = true;
    	    		fullConversation = fullConversation.substring(doneCopyingString.length());
    	    	}
    	    	Pattern pattern = Pattern.compile(Pattern.quote(endString));
    	    	String [] data = pattern.split(fullConversation);
    	    	int colorLength = colorString.length() + 1; //+1 is for the color index
    	    	for(String s: data) {
    	    		int colorIndex = (int) (s.charAt(colorLength - 1) - (int) '0');
    	    		color.add(colorIndex);
    	    		s = s.substring(colorLength);
    	    		conversation.add(s);
    	    	}
    	    } else {
            	//NOT DONE COPYING AND FILE IS TODAY
    	    	copyYesterdayIncomplete();
    	    	Pattern pattern = Pattern.compile(Pattern.quote(endString));
    	    	String [] data = pattern.split(fullConversation);
    	    	int colorLength = colorString.length() + 1; //+1 is for the color index
    	    	for(String s: data) {
    	    		int colorIndex = (int) (s.charAt(colorLength - 1) - (int) '0');
    	    		color.add(colorIndex);
    	    		s = s.substring(colorLength);
    	    		conversation.add(s);
    	    	}
    	    }
    	} else if(!file.exists()) {
    		//FILE DOESN'T EXIST
            if(todayFileName.equals(fileName)) {
            	//COPY ALL PREVIOUS DAY'S INCOMPLETE
            	copyYesterdayIncomplete();
            }
    	} else if(file.length() <= 0) {
    		file.delete();
    	}
    	startsEmpty = conversation.isEmpty();
    }
    
    private String fileToString(File file) {
    	BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
    	StringBuilder text = new StringBuilder();
	    //String ls = System.getProperty("line.separator", "\n"); //FOR LINE SEPARATOR (to fix readLine destroying /n chars)
		String line = null;
		try {
			while ((line = br.readLine()) != null) {
				text.append(line); //text.append(ls);
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String fullConversation = text.toString();
		return fullConversation;
    }
    
    private void copyYesterdayIncomplete() {
        String yesterdayFileName; File yesterdayFile;
    	Calendar yesterdayDate = currentDate;
        if(goodFilesExist()) {
    		do {
    			yesterdayDate.set(Calendar.DAY_OF_MONTH, yesterdayDate.get(Calendar.DAY_OF_MONTH) - 1);
    			yesterdayFileName = DateFormat.format("yyyyMMdd", yesterdayDate).toString();
    			yesterdayFile = makeMessageStoreFile(yesterdayFileName);
    		} while (!yesterdayFile.exists());
        	
    		String fullConversation = fileToString(yesterdayFile);
    	    if(fullConversation.startsWith(doneCopyingString)) fullConversation = fullConversation.substring(doneCopyingString.length());
    	    /*else {
    	    	Calendar tempC = currentDate;
    	    	currentDate = yesterdayDate;
    	    	String yesterdayDateTemp = DateFormat.format("yyyyMMdd", currentDate).toString();
    	    	switchToFile(yesterdayDateTemp, currentDate);
    	    	copyYesterdayIncomplete();
    	    	String todayDateTemp = DateFormat.format("yyyyMMdd", currentDate).toString();
    	    	switchToFile(todayDateTemp,tempC);
    	    	fullConversation = fileToString(yesterdayFile);
    	    	if(fullConversation.startsWith(doneCopyingString)) fullConversation = fullConversation.substring(doneCopyingString.length());
    	    }*/
    	    Pattern pattern = Pattern.compile(Pattern.quote(endString));
    	    String [] data = pattern.split(fullConversation);
    	    int colorLength = colorString.length() + 1; //+1 is for color index
    	    for(String s: data) {
	    		int colorIndex = (int) (s.charAt(colorLength - 1) - 48); //48 == ascii(0)
	    		if(colorIndex != 0) {
	    			color.add(colorIndex);
	    			s = s.substring(colorLength);
	    			conversation.add(s);
	    		}
	    	}
        } else {
        	Toast.makeText(MyManager.this, "No previous files exist. Can't Copy yesterday's notes.", Toast.LENGTH_LONG).show();
        }
    	doneCopying = true;
    }
    
    private boolean goodFilesExist() {
    	boolean goodFilesFlag = false;
    	
    	File sdCardRoot = Environment.getExternalStorageDirectory();
    	File dir = new File(sdCardRoot, myPath + folderPath);
    	if(dir != null && dir.listFiles() != null) {
    		for (File f : dir.listFiles()) {
    	    	if (f.isFile()) {
    	    		if(f.length() > 0) {
    	    			String fileName = f.getName();
    	        		Calendar fileDate = convertStringToDate(fileName);
    	        		if(fileDate != null) {
    	        			int difference = (int) ((fileDate.getTimeInMillis() - currentDate.getTimeInMillis()) / (24 * 60 * 60 * 1000));
    	        			if(difference < 0) {
    	    	        		goodFilesFlag = true;
    	        				break;
    	        			}
    	        		}
    	    		} else {
    	    			f.delete();
    	    		}
    	    	}
    		}
    	}
    	return goodFilesFlag;
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
    
    private void managerStart() {
		resetLView();
		//Sets onClickListener
		configureSubmitButton();
    }
    
    private void configureSubmitButton() {
		ImageView btn = (ImageView) findViewById(R.id.submit);
		ImageView menu = (ImageView) findViewById(R.id.menuBtn);
		ImageView help = (ImageView) findViewById(R.id.helpBtn);
		//CLICK ADDS A NEW NOTE
		btn.setOnClickListener(btnClickListener);
		//LONG CLICK REORGANIZES THE COLORS
		btn.setOnLongClickListener(btnLongClickListener);
		
		menu.setOnClickListener(menuClickListener);
		help.setOnClickListener(helpClickListener);
	}

    //CLICK LISTENER CREATED WHENEVER NEW TEXTVIEW IS CREATED ON BUTTON CLICK
	private void configureTextView(final TextView mText, final int index) {
		mText.setOnTouchListener(textTouchListener);
		//CLICK CHANGES ITS COLOR
		mText.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				int newColor = 0;
	    		if(moveNote == -1) {
	    			if( touchX > screenX / 3 ) {
	    				newColor = (color.get(index) + 1) % BGcolor.size();
	    			} else {
	    				newColor = (color.get(index) - 1 + BGcolor.size()) % BGcolor.size();
	    			}
	    			color.set(index, newColor);
	    			mText.setBackgroundColor(BGcolor.get(newColor));
	    			mText.setTextColor(FGcolor.get(newColor));
	    		} else {
	    			conversation.add(index, conversation.remove(moveNote));
	    			color.add(index, color.remove(moveNote));
	    			resetLView();
	    			moveNote = -1;
	    		}
			}
		});
		//LONGCLICK DELETES THE VIEW
		mText.setOnLongClickListener(new View.OnLongClickListener() {
    		@Override
			public boolean onLongClick(View v) {
				if(touchX < screenX / 2) {
					clipper.copyToClipboard(MyManager.this, conversation.get(index));
    				if(color.get(index) != 0) {
    					//Toast.makeText(MyManager.this, "Can't deleted unless event is completed (BLACK).", Toast.LENGTH_LONG).show();
    					//mText.setBackgroundColor(BGcolor[0]); color.set(index, 0);
    					
    					AlertDialog.Builder builder = new AlertDialog.Builder(MyManager.this);
    	            	builder.setMessage("Are you sure you want to delete this? (it is incomplete)");
    	            	builder.setCancelable(true);
    	            	builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
    	                	public void onClick(DialogInterface dialog, int id) {
    	    					conversation.remove(index); color.remove(index);
    	    					resetLView();
    	                    	dialog.cancel();
    	                	}
    	            	});
    	            	builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
    	            		public void onClick(DialogInterface dialog, int id) {
    	                    	dialog.cancel();
    	                	}
    	            	});

    	            	AlertDialog alert = builder.create();
    	            	alert.show();
    				} else {
    					conversation.remove(index); color.remove(index);
    					resetLView();
    				}
				} else if(moveNote == -1) {
					ToastIt("Click on the note to which you want to move this note.");
					mText.setBackgroundColor(Color.GRAY);
					moveNote = index;
				}
				return true;
			}
    	});
    }
	
	//LIST OF ALL *CONSTANT* CLICKLISTENERS, TOUCHLISTENERS, ETC. TO SAVE MEMORY
	View.OnClickListener btnClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			//Finds the LinearLayout and EditText
			EditText inputText = (EditText) findViewById(R.id.inputText);
			LinearLayout lView = (LinearLayout)findViewById(R.id.allMessages);
			
			//Adds properties to conversation and color
			String message = inputText.getText().toString();
			conversation.add(message);
			color.add(1);

			final int index = conversation.size() - 1;
			lView.addView(getTextView(index));
			
			InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(inputText.getWindowToken(), 0);
			
			inputText.setText(""); //Sets EditText to ""
			
			//Scrolls to bottom
			ScrollView sView = (ScrollView)findViewById(R.id.messages);
			sView.smoothScrollTo(0, lView.getTop());
		}
	};
	
	View.OnLongClickListener btnLongClickListener = new View.OnLongClickListener() {
		@Override
		public boolean onLongClick(View v) {
			reorganizeColors();
			return true;
		}
	};
	
	View.OnClickListener menuClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			Intent i = new Intent(v.getContext(), ConvoList.class);
			i.putExtra("currentMonth", currentDate.get(Calendar.MONTH));
			i.putExtra("currentYear", currentDate.get(Calendar.YEAR));
			startActivityForResult(i, 1);
		}
	};
	
	View.OnClickListener helpClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			Intent i = new Intent(v.getContext(), SettingsPage.class);
			i.putExtra("firstTime", false);
			startActivity(i);
		}
	};
	
	View.OnTouchListener textTouchListener = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent e) {
			int currentAction = e.getAction();
			if(currentAction == MotionEvent.ACTION_DOWN) {
	    		touchX = e.getX();
	    		touchY = e.getY();
			}
			return false;
		}
	};
	
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == 1) {
	        if(resultCode == RESULT_OK) {
	            String nextFile = intent.getStringExtra("nextFile");
	            Calendar nextDate = convertStringToDate(nextFile);
	            switchToFile(nextFile, nextDate);
	            fromConvoList = true;
	        }
		}
	}
	
	//FUNCTION TO BE CALLED ON SWIPING / GOING TO DIFFERENT FILE FROM FILELIST MENU
    private void switchToFile(String fileName, Calendar d) {
    	conversationToFile(currentFileName);
    	currentDate = d;
    	moveNote = -1;
    	managerInit(fileName, d);
		resetLView();
    }
	
	//REORGANIZE TEXTVIEWS BASED ON COLOR STORED IN COLOR ARRAYLIST
	private void reorganizeColors() {
		ArrayList<Integer> newColor = new ArrayList<Integer>();
		ArrayList<String> newConversation= new ArrayList<String>();
		for(int i=1; i < BGcolor.size(); i++) {
			for(int j=0; j<color.size(); j++) {
				if(color.get(j) == i) {
					newColor.add(color.get(j)); newConversation.add(conversation.get(j));
					color.remove(j); conversation.remove(j); j--;
				}
			}
		}
		for(int j=0; j<color.size(); j++) {
			if(color.get(j) == 0) {
				newColor.add(color.get(j)); newConversation.add(conversation.get(j));
				color.remove(j); conversation.remove(j); j--;
			}
		}
		color = newColor; conversation = newConversation;
		resetLView();
	}
	
	private void resetLView() {
		LinearLayout lView = (LinearLayout)findViewById(R.id.allMessages);
		lView.removeAllViews();
		
		TextView myDate = (TextView) findViewById(R.id.dateText);
		myDate.setText(DateFormat.format(dateString, currentDate).toString());
		int maxColor = 0;
		for(int i=0; i < conversation.size(); i++) {
			lView.addView(getTextView(i));
			if(maxColor < color.get(i)) maxColor = color.get(i);
		}
		if(BGcolor.size() <= maxColor)
			ToastItLong("Some of your tasks are stored as colors that haven't been set yet! Set them in Preferences");
		
		//Scrolls to bottom
		//ScrollView sView = (ScrollView)findViewById(R.id.messages);
		//sView.smoothScrollTo(0, lView.getTop());
	}

	private TextView getTextView(int i) {
		TextView mText = new TextView(MyManager.this);
		mText.setText(conversation.get(i));
		mText.setTextSize(FONT_SIZE);
        mText.setGravity(Gravity.LEFT);
        mText.setMinimumHeight(minTextViewHeight);
        
        int colorIndex = color.get(i);
        if(BGcolor.size() > colorIndex) {
        	mText.setBackgroundColor(BGcolor.get(colorIndex));
        	mText.setTextColor(FGcolor.get(colorIndex));
        } else {
        	int addingColor = defBGcolor[colorIndex];
        	if(addingColor == 0) addingColor = Color.BLACK;
        	mText.setBackgroundColor(addingColor);
        	if(defBGcolor[colorIndex] == Color.WHITE) mText.setTextColor(Color.BLACK);
        	else mText.setTextColor(Color.WHITE);
        }
        
        //mText.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, R.drawable.separator_line);
        configureTextView(mText, i);
		return mText;
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
    	case SimpleGestureFilter.SWIPE_LEFT : gotoNextFile(); break; //nextFile
    	case SimpleGestureFilter.SWIPE_RIGHT : gotoPrevFile(); break; //prevFile
    	}
    }
    
    @Override
    public void onDoubleTap() {
    	//DOUBLE TAPS RETURN FALSE IN SIMPLEGESTUREFILTER
    	//Toast.makeText(this, "Double Tap", Toast.LENGTH_SHORT).show();
    }
    
    private void gotoNextFile() {
    	String nextFileName; Calendar nextDate = currentDate;
    	nextDate.set(Calendar.DATE, nextDate.get(Calendar.DATE) + 1);
    	nextFileName = DateFormat.format("yyyyMMdd", nextDate).toString();
    	switchToFile(nextFileName, nextDate);
    }
    
    private void gotoPrevFile() {
    	String prevFileName; Calendar prevDate = currentDate;
    	prevDate.add(Calendar.DAY_OF_MONTH, -1);
		prevFileName = DateFormat.format("yyyyMMdd", prevDate).toString();
        switchToFile(prevFileName, prevDate);
    }
    

    //Checks if external storage is available for read and write
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        Toast.makeText(MyManager.this, "Can't write to SD card. Session won't be stored.", Toast.LENGTH_LONG).show();
        return false;
    }

    //Checks if external storage is available to at least read
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
            Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        Toast.makeText(MyManager.this, "Can't read SD card. Exit app and mount SD card.", Toast.LENGTH_LONG).show();
        return false;
    }
    
    //Creates and returns a file in path with name fileName
    public File makeMessageStoreFile(String fileName) {
    	if(isExternalStorageWritable()) {
    		File path = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + myPath + folderPath);
    		path.mkdirs();
    		File f = new File(path, fileName);
    		return f;
    	} else {
    		Toast.makeText(MyManager.this, "SD Card not connected. Session won't be stored for future.", Toast.LENGTH_LONG).show();
    		return null;
    	}
    }
    
    private int stringToColor(String s) {
    	if(s.equals("None")) return 0;
    	else if(s.equals("Black")) return Color.BLACK;
    	else if(s.equals("Blue")) return Color.BLUE;
    	else if(s.equals("Cyan")) return Color.CYAN;
    	else if(s.equals("Dark Grey")) return Color.DKGRAY;
    	else if(s.equals("Green")) return Color.GREEN;
    	else if(s.equals("Light Grey")) return Color.LTGRAY;
    	else if(s.equals("Magenta")) return Color.MAGENTA;
    	else if(s.equals("Red")) return Color.RED;
    	else if(s.equals("White")) return Color.WHITE;
    	else if(s.equals("Yellow")) return Color.YELLOW;
    	return 0;
    }
    
    private void ToastItShort(String s) { Toast.makeText(MyManager.this, s, 100).show(); }
    private void ToastIt(String s) { Toast.makeText(MyManager.this, s, Toast.LENGTH_SHORT).show(); }
    private void ToastItLong(String s) { Toast.makeText(MyManager.this, s, Toast.LENGTH_LONG).show(); }

}
