package jabs.automaticbillsdownloader.ui;

import jabs.automaticbillsdownloader.AutomaticBillDownloaderApplication;
import jabs.automaticbillsdownloader.DropboxManager;
import jabs.automaticbillsdownloader.R;
import jabs.automaticbillsdownloader.preferences.BillsPreference;
import jabs.automaticbillsdownloader.preferences.TimePreference;
import jabs.automaticbillsdownloader.scheduler.AlarmReceiver;

import java.util.Map;
import java.util.Map.Entry;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.widget.Toast;

import com.dropbox.client2.exception.DropboxException;

public class UserPreferenceFragment extends PreferenceFragment {
	private static final String DOWNLOAD_NOW_PREFERENCE_KEY = "downloadNowPreference";
	private static final String DROPBOX_ACCOUNT_PREFERENCE_KEY = "dropboxAccountPreference";
	private static final String TIME_PREFERENCE_KEY = "scheduledTimePreference";
	private Preference dropboxAccountPref;
	private OnSharedPreferenceChangeListener preferenceChangeListener;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		final Activity activity = getActivity();
		
		preferenceChangeListener = new OnSharedPreferenceChangeListener() {
			@Override
			public void onSharedPreferenceChanged(
					SharedPreferences sharedPreferences, String key) {
				rebuildBillPreferenceCategory();
			}
		};
		
		addPreferencesFromResource(R.xml.preferences);
		rebuildBillPreferenceCategory();
		
		dropboxAccountPref = getPreferenceManager().findPreference(
				DROPBOX_ACCOUNT_PREFERENCE_KEY);
		final TimePreference timePref = (TimePreference) getPreferenceManager()
				.findPreference(TIME_PREFERENCE_KEY);
		final Preference downloadNowPref = getPreferenceManager()
				.findPreference(DOWNLOAD_NOW_PREFERENCE_KEY);
		
		downloadNowPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Toast.makeText(activity, "download now clicked", Toast.LENGTH_SHORT).show();

				final Intent intent = new Intent(activity, AlarmReceiver.class);
				activity.sendBroadcast(intent);
				return true;
			}
		});
		
		dropboxAccountPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				DropboxManager.instance().getSession().startOAuth2Authentication(activity);
				return true;
			}
		});
		
		timePref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				String time = (String) newValue;
				return AutomaticBillDownloaderApplication
						.scheduleBillDownloads(activity, time);
			}
		});
		
		AutomaticBillDownloaderApplication.scheduleBillDownloads(activity,
				timePref.getSummary().toString());
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		if (DropboxManager.instance().getSession().authenticationSuccessful()) {
	        try {
	            // Required to complete auth, sets the access token on the session
	            DropboxManager.instance().getSession().finishAuthentication();

				String accessToken = DropboxManager.instance().getSession()
						.getOAuth2AccessToken();
				DropboxManager.setNewLinkedAccountAuthToken(accessToken);
	        } catch (IllegalStateException e) {
	            Log.i("DbAuthLog", "Error authenticating", e);
	        }
	    }
		
		dropboxAccountPref.setTitle(R.string.set_dropbox_account);
		enableDisablePreferences(false);
		
		if (DropboxManager.instance().getSession().isLinked()) {
			dropboxAccountPref.setSummary(R.string.loading_text);
			new AsyncTask<Void, Void, String>() {

				@Override
				protected String doInBackground(Void... params) {
					try {
						return DropboxManager.instance().accountInfo().displayName;
					} catch (DropboxException e) {
						return null;
					}
				}

				@Override
				protected void onPostExecute(String result) {
					if (result == null)
						dropboxAccountPref.setSummary("<Invalid Account>");
					else {
						dropboxAccountPref.setSummary(result);
						dropboxAccountPref.setTitle("Change Dropbox Account");
						enableDisablePreferences(true);
					}
				}
			}.execute();
		} else {
			dropboxAccountPref.setSummary("<No Dropbox Account>");
		}
	}
	
	private void rebuildBillPreferenceCategory() {
		
		PreferenceCategory billCategory = (PreferenceCategory) findPreference("billPreferenceCategory");
		billCategory.removeAll();
		
		SharedPreferences sharedPrefs = getPreferenceManager().getSharedPreferences();
		Map<String, ?> sharedPrefsMap = sharedPrefs.getAll();
		for(Entry<String, ?> entry : sharedPrefsMap.entrySet()) {
			if (entry.getKey().startsWith("billsPreference"))
				billCategory.addPreference(new BillsPreference(getActivity(),
						entry.getKey()));
		}
		
		Preference addNewBillPref = new BillsPreference(getActivity());
		billCategory.addPreference(addNewBillPref);
		
		sharedPrefs.unregisterOnSharedPreferenceChangeListener(
						preferenceChangeListener);

		sharedPrefs.registerOnSharedPreferenceChangeListener(
						preferenceChangeListener);
	}
	
	private void enableDisablePreferences(boolean enable) {
		PreferenceCategory billCategory = (PreferenceCategory) findPreference("billPreferenceCategory");
		billCategory.setEnabled(enable);
	}
}
