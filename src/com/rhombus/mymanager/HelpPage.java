package com.rhombus.mymanager;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class HelpPage extends Activity {
	
	int layoutsList[] = {R.layout.my_manager_help_alert1, R.layout.my_manager_help_alert2, R.layout.my_manager_help_alert3};
	int currentLayoutIndex = 0;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if(getIntent().getExtras().getBoolean("firstHelp")) {
			showHelpFirst();
		} else {
			showHelp0();
		}
	}
	
	private void configureButtonListener() {
		Button nextBtn = (Button) findViewById(R.id.helpNextBtn);
		Button prevBtn = (Button) findViewById(R.id.helpPrevBtn);
		nextBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				currentLayoutIndex++;
				switch(currentLayoutIndex) {
				case 0: showHelp0(); break;
				case 1: showHelp1(); break;
				case 2: showHelp2(); break;
				case 3: showHelp3(); break;
				default: currentLayoutIndex--;
				}
			}
		});
		
		prevBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				currentLayoutIndex--;
				switch(currentLayoutIndex) {
				case 0: showHelp0(); break;
				case 1: showHelp1(); break;
				case 2: showHelp2(); break;
				case 3: showHelp3(); break;
				default: currentLayoutIndex++;
				}
			}
		});
	}
	
	private void showHelp0() {
		setContentView(layoutsList[0]);
		((Button) findViewById(R.id.helpPrevBtn)).setClickable(false);
		configureButtonListener();
	}
	private void showHelp1() {
		setContentView(layoutsList[1]);
		configureButtonListener();
	}
	private void showHelp2() {
		setContentView(layoutsList[2]);
		configureButtonListener();
	}
	private void showHelp3() {
		/*Intent i = new Intent(HelpPage.this, MyManager.class);
		startActivity(i);*/
		finish();
	}
	private void showHelpFirst() {
		setContentView(R.layout.my_manager_first_help);
		Button nextBtn = (Button) findViewById(R.id.helpNextBtn);
		nextBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(HelpPage.this, MyManager.class);
				startActivity(i);
				finish();
			}
		});
	}
}
