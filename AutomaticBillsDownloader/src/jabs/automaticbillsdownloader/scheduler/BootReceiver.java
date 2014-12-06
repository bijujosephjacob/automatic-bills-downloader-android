package jabs.automaticbillsdownloader.scheduler;

import jabs.automaticbillsdownloader.AutomaticBillDownloaderApplication;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class BootReceiver extends BroadcastReceiver {
	private static final String BOOT_COMPLETED_ACTION = "android.intent.action.BOOT_COMPLETED";
	private static final String TIME_PREFERENCE_KEY = "scheduledTimePreference";

	@Override
	public void onReceive(Context context, Intent intent) {
		Toast.makeText(context, "AutomaticBillsDownloader - phone restarted",
				Toast.LENGTH_SHORT).show();
		
		if (intent.getAction().equals(BOOT_COMPLETED_ACTION) == false) {
			Toast.makeText(
					context,
					"AutomaticBillsDownloader - received invalid intent action "
							+ intent.getAction(), Toast.LENGTH_SHORT).show();
			return;
		}
		
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		
		if (prefs.contains(TIME_PREFERENCE_KEY)) {
			String time = prefs.getString(TIME_PREFERENCE_KEY, "08:00");
			Toast.makeText(context,
					"AutomaticBillsDownloader - preferred time " + time,
					Toast.LENGTH_SHORT).show();
			AutomaticBillDownloaderApplication.scheduleBillDownloads(context,
					time);
		}
	}
}