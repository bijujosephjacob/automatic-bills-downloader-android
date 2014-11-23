package jabs.automaticbillsdownloader.model;

import android.util.Log;

import com.google.gson.Gson;

public class Bill {
	private static Gson gson = new Gson();
	
	private String billType;
	private String username, password;
	private String dropboxFolder;

	public Bill(String billType, String username, String password, String dropboxFolder) {
		this.billType = billType;
		this.username = username;
		this.password = password;
		this.dropboxFolder = dropboxFolder;
	}
	
	public String getBillType() {
		return billType;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public String getDropboxFolder() {
		return dropboxFolder;
	}

	public String toJson() {
        // Serialize this class into a JSON string using GSON
        return gson.toJson(this);
    }
 
    static public Bill createFromJson(String jsonString) {
    	try {
    		return gson.fromJson(jsonString, Bill.class);
    	}
    	catch(Exception ex) {
    		Log.e("Bill", ex.getMessage());
    	}
    	return null;
    }
    
    @Override
    public String toString() {
    	return billType + " - " + username;
    }
}
