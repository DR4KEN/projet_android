package fr.lenours.sensortracker;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;


public class StepService extends Service {
    StepAlarm alarm = new StepAlarm();

    public StepAlarm getAlarm() {
        return alarm;
    }

    public void setAlarm(StepAlarm alarm) {
        this.alarm = alarm;
    }

    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        alarm.setAlarm(this);
        return START_STICKY;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        alarm.setAlarm(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}