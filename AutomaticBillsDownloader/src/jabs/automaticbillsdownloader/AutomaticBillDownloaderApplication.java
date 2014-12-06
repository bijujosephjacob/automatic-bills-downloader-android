package jabs.automaticbillsdownloader;

import jabs.automaticbillsdownloader.preferences.TimePreference;
import jabs.automaticbillsdownloader.scheduler.AlarmReceiver;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class AutomaticBillDownloaderApplication extends Application {
	private static Context context;

    public void onCreate(){
        super.onCreate();
        context = getApplicationContext();
    }

    public static Context getAppContext() {
        return context;
    }
    
    public static boolean scheduleBillDownloads(Context activity, String time) {
		if (time == null) {
			Toast.makeText(activity, "invalid time selected",
					Toast.LENGTH_LONG).show();
			return false;
		}
		
		final AlarmManager alarmManager = (AlarmManager) activity
				.getSystemService(Context.ALARM_SERVICE);
		final Intent intent = new Intent(activity, AlarmReceiver.class);
		final PendingIntent pendingIntent = PendingIntent.getBroadcast(
				activity, 0, intent, 0);
		
		Toast.makeText(activity, "schedule daily @ " + time,
				Toast.LENGTH_SHORT).show();
		
		int hour = TimePreference.getHour(time);
		int minute = TimePreference.getMinute(time);
		
		Calendar scheduledTime = Calendar.getInstance();
		scheduledTime.set(Calendar.HOUR_OF_DAY, hour);
		scheduledTime.set(Calendar.MINUTE, minute);
		scheduledTime.set(Calendar.SECOND, 0);
		scheduledTime.set(Calendar.MILLISECOND, 0);
		
		if(scheduledTime.before(Calendar.getInstance())) {
			scheduledTime.add(Calendar.DATE, 1);
		}

		alarmManager.cancel(pendingIntent);
		alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP,
				scheduledTime.getTimeInMillis(),
				AlarmManager.INTERVAL_DAY, pendingIntent);
		Toast.makeText(activity.getApplicationContext(),
				"Alarm Set", Toast.LENGTH_SHORT).show();
		
		return true;
	}
}
