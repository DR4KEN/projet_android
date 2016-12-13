package fr.lenours.sensortracker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

/**
 * Created by Clemsbrowning on 23/05/2016.
 */


public class StepAlarm extends BroadcastReceiver {

    public static String TAG = "StepAlarm";
    PowerManager pm;
    PowerManager.WakeLock wl;


    @Override
    public void onReceive(Context context, Intent intent) {

        pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "");
        wl.acquire();
        Log.i(TAG, "Toasted ! " + Extra.timeMillisToDate(System.currentTimeMillis()));
        if (MainActivity.onDestroyCalled) {
            MainActivity.onDestroyCalled = false;
            return;
        }

        updateSteps();

        wl.release();
    }

    private void updateSteps() {
        String[] lastLine = CsvReader.getLastLine(FileData.TOTAL_STEP);
        if ( lastLine != null  && lastLine[0].equals(Extra.getDateAsString(System.currentTimeMillis()))) {
            CsvReader.tail(FileData.TOTAL_STEP,1,true);
        }
        CsvReader.writeCSV(FileData.TOTAL_STEP,System.currentTimeMillis() + "," + StepData.day_step + "," + StepData.objective_step,true);
        StepData.dayChanged = true;
        StepData.day_step = 0;
    }


    public void setAlarm(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, StepAlarm.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
        am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000 * 60 * 60 * 24 /* 1 day */, pi);
    }

    public void cancelAlarm(Context context) {
        Intent intent = new Intent(context, StepAlarm.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
    }

}
