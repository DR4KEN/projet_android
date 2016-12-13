package fr.lenours.sensortracker;

import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.util.Log;

import com.dobi.walkingsynth.accelerometer.AccelerometerDetector;
import com.dobi.walkingsynth.accelerometer.OnStepCountChangeListener;

import org.apache.commons.lang3.ArrayUtils;

/**
 * Classe qui gère les données de l'application
 * Created by Clemsbrowning on 17/05/2016.
 */
public class StepData implements OnStepCountChangeListener, OnUserWalkingChangeListener {


    public static String TAG = "StepData";
    public static int day_step = 0;
    public static int objective_step = Integer.MAX_VALUE;
    public static boolean dayChanged = false;
    public static boolean isWalking = false;
    public static String[] days = new String[7];

    /**
     * Constructeur
     * @param sensorManager
     */
    public StepData(SensorManager sensorManager) {
        AccelerometerDetector accelerometerDetector = new AccelerometerDetector(sensorManager);
        accelerometerDetector.setStepCountChangeListener(this);
        accelerometerDetector.startDetector();
        WalkingDetector walkingDetector = new WalkingDetector(sensorManager);
        walkingDetector.setOnUserWalkingListener(this);
    }

    /**
     * Permet de recuperer la liste des x derniers jour a compter d'aujourd'hui
     * @param numberOfDays
     * @param nbDaysinOne
     * @param revert trie par ordre croissant ou decroissant la liste
     * @return
     */
    public static String[] getlastXDays(int numberOfDays, int nbDaysinOne, boolean revert) {
        String[] days = new String[numberOfDays];

        long time = System.currentTimeMillis();
        int cpt = 0;
        days[0] = "Aujourd'hui";
        days[1] = "Hier";
        if (nbDaysinOne == 1)
            for (int i = 2; i < numberOfDays; i++) {
                days[i] = Extra.timeMillisToDate(time - (1000 * 60 * 60 * 24 * i));
                Log.i(TAG, days[i]);
            }
        else if (nbDaysinOne>1)
            for (int i = 2 ; i < numberOfDays ; i+=nbDaysinOne) {
                days[cpt] = "Du " + Extra.timeMillisToDate(time + (1000 * 60 * 60 * 24 * i)) + " au " + Extra.timeMillisToDate(time + (1000 * 60 * 60 * 24 * (i+nbDaysinOne-1)));
                Log.i(TAG,days[cpt]);
            }
        if (revert) ArrayUtils.reverse(days);
        return days;
    }


    @Override
    public void onStepCountChange(SensorEvent event) {
        if (isWalking) {
            StepData.day_step++;
            Log.i(TAG, " Steps : " + StepData.day_step);
        }

    }

    @Override
    public void OnUserWalkingChange(boolean isWalking) {
        this.isWalking = isWalking;
    }
}
