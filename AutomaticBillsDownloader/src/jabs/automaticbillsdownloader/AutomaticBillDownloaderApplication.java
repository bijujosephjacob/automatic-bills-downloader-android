package jabs.automaticbillsdownloader;

import android.app.Application;
import android.content.Context;

public class AutomaticBillDownloaderApplication extends Application {
	private static Context context;

    public void onCreate(){
        super.onCreate();
        context = getApplicationContext();
    }

    public static Context getAppContext() {
        return context;
    }
}
