package com.rhombus.mymanager;

import java.io.File;
import java.util.ArrayList;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.Environment;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class SettingsPage extends PreferenceActivity implements OnSharedPreferenceChangeListener {

	int defBGcolor[] = {0, 1, 2, 4, 6, 8, 10, 10, 10, 10};
	ArrayList<String> folderNames;
	TinyDB tinyDB;
	
	String myPath = "DATA/MyManager/";
	
	@SuppressWarnings("deprecation")
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.my_manager_settings_view);
        addPreferencesFromResource(R.layout.my_manager_settings);

        tinyDB = new TinyDB();
        ListView v = getListView();
        v.addFooterView(getFolderNameFooter());
        
        initFolders();
        if(getIntent().getExtras().getBoolean("firstTime")) {
        	setDefaultValues();
        	firstTimeHelp();
        }
        PreferenceManager.setDefaultValues(SettingsPage.this, R.layout.my_manager_settings,false);
        initSummary(getPreferenceScreen());
        
        configureListeners();
	}

	private void configureListeners() {
		TextView helpBtn = (TextView) findViewById(R.id.helpTV);
		helpBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
	        	showHelpDialog();
			}
		});
		
		TextView creditBtn = (TextView) findViewById(R.id.creditsTV);
		creditBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showCreditsDialog();
			}
		});
	}
	
	@Override
    protected void onResume() {
        super.onResume();
        // Set up a listener whenever a key changes
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the listener whenever a key changes
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updatePrefSummary(findPreference(key));
    }

    private void initFolders() {
    	folderNames = tinyDB.getList("folders");
    	if(folderNames.isEmpty()) {
    		folderNames.add("Default");
    		tinyDB.putList("folders", folderNames);
    	}
    	CharSequence[] values = new String[folderNames.size()];
    	int i = 0;
    	for(String s: folderNames) {
    		values[i] = s; i++;
    	}
    	ListPreference lp = (ListPreference) findPreference("prefFolder");
    	lp.setEntries(values);
    	lp.setEntryValues(values);
    }
    
    private void initSummary(Preference p) {
    	if (p instanceof PreferenceGroup) {
            PreferenceGroup pGrp = (PreferenceGroup) p;
            for (int i = 0; i < pGrp.getPreferenceCount(); i++) {
                initSummary(pGrp.getPreference(i));
            }
        } else {
            updatePrefSummary(p);
        }
    }

    private void updatePrefSummary(Preference p) {
        if (p instanceof ListPreference) {
            ListPreference listPref = (ListPreference) p;
            p.setSummary(listPref.getEntry());
        }
        else if (p instanceof EditTextPreference) {
            EditTextPreference editTextPref = (EditTextPreference) p;
            if (p.getTitle().toString().contains("assword")) {
                p.setSummary("******");
            } else {
                p.setSummary(editTextPref.getText());
            }
        }
    }
    
    private void setDefaultValues() {
    	for(int i=0; i<10; i++) {
    		ListPreference colorPref = (ListPreference) findPreference("prefColors"+i);
    		colorPref.setValueIndex(defBGcolor[i]);
    	}
		ListPreference fontSizePref= (ListPreference) findPreference("prefFontSize");
		fontSizePref.setValueIndex(1);
		ListPreference folderPref = (ListPreference) findPreference("prefFolder");
		folderPref.setValueIndex(0);
    }
    
    private void firstTimeHelp() {
    	Intent i = new Intent(SettingsPage.this, HelpPage.class);
    	i.putExtra("firstHelp", true);
    	startActivity(i);
    	finish();
    }
    
    private void showHelpDialog() {
    	Intent i = new Intent(SettingsPage.this, HelpPage.class);
    	i.putExtra("firstHelp", false);
    	i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    	startActivity(i);
    	finish();
    }
    
    private void showCreditsDialog() {
    	Intent i = new Intent(SettingsPage.this, CreditsPage.class);
    	i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    	startActivity(i);
    }
    
    private RelativeLayout getFolderNameFooter() {
    	LayoutInflater inflater = LayoutInflater.from(SettingsPage.this);
    	RelativeLayout rView = (RelativeLayout) inflater.inflate(R.layout.my_manager_settings_listfooter, null, false);
    	ImageView btn = (ImageView) rView.findViewById(R.id.submitFolderName);
    	btn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				EditText inputText = (EditText) findViewById(R.id.inputFolderName);
				String folderText = inputText.getText().toString();
				folderNames.add(folderText);
				tinyDB.putList("folders", folderNames);
				inputText.setText("");
				
				CharSequence[] values = new String[folderNames.size()];
		    	int i = 0;
		    	for(String s: folderNames) {
		    		values[i] = s; i++;
		    	}
		    	ListPreference lp = (ListPreference) findPreference("prefFolder");
		    	lp.setEntries(values);
		    	lp.setEntryValues(values);
		    	
		    	lp.setValue(folderText);
			}
    	});
    	
    	return rView;
    }
    
  //Checks if external storage is available for read and write
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }
    
    //Creates and returns a file in path with name fileName
    public File makeMessageStoreFile(String fileName) {
    	if(isExternalStorageWritable()) {
    		File path = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + myPath);
    		path.mkdirs();
    		File f = new File(path, fileName);
    		return f;
    	} else {
    		return null;
    	}
    }
}
