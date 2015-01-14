package com.rhombus.mymanager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import android.os.Environment;

public class TinyDB {

	String myPath = "/DATA/MyManager/Folders/";
	
	public ArrayList<String> getList(String fileName) {
		ArrayList<String> list = new ArrayList<String>();
		if(isExternalStorageReadable()) {
			File file = makeMessageStoreFile(fileName);
			BufferedReader br = null;
			try {
				br = new BufferedReader(new FileReader(file));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			String item = "";
			if(br != null) {
				try {
					while ((item = br.readLine()) != null) {
						list.add(item);
					}
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return list;
	}
	
	public void putList(String fileName, ArrayList<String> list) {
		if(isExternalStorageWritable()) {
    		File file = makeMessageStoreFile(fileName);
    		PrintWriter pw = null;
			try {
				pw = new PrintWriter(file);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			if(pw != null) {
				for(int i=0; i < list.size(); i++) {
        			String item = list.get(i);
        			pw.println(item);
				}
				pw.flush();
				pw.close();
			}
		}
	}
	
	
	//Checks if external storage is available for read and write
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    //Checks if external storage is available to at least read
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
            Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
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
