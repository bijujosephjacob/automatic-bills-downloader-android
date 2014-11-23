package jabs.automaticbillsdownloader.scheduler;

import jabs.automaticbillsdownloader.DownloadManager;
import jabs.automaticbillsdownloader.DropboxManager;
import jabs.automaticbillsdownloader.R;
import jabs.automaticbillsdownloader.model.Bill;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.widget.Toast;

public class AlarmReceiver extends BroadcastReceiver {

	protected static final int PROGRESS_NOTIFICATION_ID = 1;

	@Override
	public void onReceive(Context context, Intent intent) {
		Toast.makeText(context, "Im running", Toast.LENGTH_SHORT).show();
		downloadBills(context);
	}
	
	private void downloadBills(Context context) {
		final NotificationManager notifyManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		final Builder mBuilder = new NotificationCompat.Builder(context);
		
		if(DropboxManager.instance().getSession().isLinked() == false) {
			Toast.makeText(context, "You have not yet connected to Dropbox",
					Toast.LENGTH_SHORT).show();
			mBuilder.setContentTitle("Automatic Bills Download")
					.setContentText("Download failed: Couldn't connect to Dropbox")
					.setSmallIcon(R.drawable.application_icon)
					.setProgress(100, 100, false);
			notifyManager.notify(PROGRESS_NOTIFICATION_ID, mBuilder.build());
			return;
		}
		
		List<Bill> bills = getBillListFromPreferences(context);
		if(bills == null || bills.isEmpty()) {
			Toast.makeText(context, "No bills found",
					Toast.LENGTH_SHORT).show();
			mBuilder.setContentTitle("Automatic Bills Download")
					.setContentText("Download failed: No bills configured")
					.setSmallIcon(R.drawable.application_icon)
					.setProgress(100, 100, false);
			notifyManager.notify(PROGRESS_NOTIFICATION_ID, mBuilder.build());
			return;
		}
		

		mBuilder.setContentTitle("Automatic Bills Download")
				.setContentText("Download in progress")
				.setSmallIcon(R.drawable.application_icon);
		
		new DownloadManager(DropboxManager.instance(), bills) {

			@Override
			protected void onProgressUpdate(Integer... progress) {
				mBuilder.setProgress(100, progress[0], false);
				notifyManager.notify(PROGRESS_NOTIFICATION_ID, mBuilder.build());
			}

			@Override
			protected void onPostExecute(Void result) {
				mBuilder.setContentText("Download complete")
						.setProgress(0, 0, false);
				notifyManager.notify(PROGRESS_NOTIFICATION_ID, mBuilder.build());
			}
		}.execute();
	}

	private List<Bill> getBillListFromPreferences(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		List<Bill> result = new ArrayList<Bill>();
		for(Entry<String, ?> entry : prefs.getAll().entrySet()) {
			if(entry.getKey().startsWith("billsPreference")) {
				result.add(Bill.createFromJson(entry.getValue().toString()));
			}
		}
		return result;
	}
}
