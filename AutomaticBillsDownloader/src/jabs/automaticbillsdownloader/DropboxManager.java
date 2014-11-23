package jabs.automaticbillsdownloader;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session.AccessType;

public class DropboxManager {
	private static final String SHARED_PREFERENCES_DROPBOX_ACCESS_TOKEN = "DropboxAccessToken";
	private static final String APP_KEY = "dropbox-app-key";
	private static final String APP_SECRET = "dropbox-secret-key";
	private static final AccessType ACCESS_TYPE = AccessType.DROPBOX;
	private static DropboxAPI<AndroidAuthSession> mDBApi;
	
	public static DropboxAPI<AndroidAuthSession> instance() {
		if(mDBApi == null)
		{
			AppKeyPair appKeys = new AppKeyPair(APP_KEY, APP_SECRET);
			
			@SuppressWarnings("deprecation")
			AndroidAuthSession session = new AndroidAuthSession(appKeys,
					ACCESS_TYPE);

			if (getLinkedAccountAuthToken() != null)
				session.setOAuth2AccessToken(getLinkedAccountAuthToken());

			mDBApi = new DropboxAPI<AndroidAuthSession>(session);
		}
		return mDBApi;
	}
	
	private static String getLinkedAccountAuthToken() {
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(AutomaticBillDownloaderApplication
						.getAppContext());
		return sharedPreferences.getString(SHARED_PREFERENCES_DROPBOX_ACCESS_TOKEN, null);
	}
	
	public static void setNewLinkedAccountAuthToken(String accessToken) {
		SharedPreferences shared = PreferenceManager
				.getDefaultSharedPreferences(AutomaticBillDownloaderApplication
						.getAppContext());
		Editor sharedPrefEditor = shared.edit();
		sharedPrefEditor.putString(SHARED_PREFERENCES_DROPBOX_ACCESS_TOKEN,
				accessToken);
		sharedPrefEditor.commit();
	}
}
