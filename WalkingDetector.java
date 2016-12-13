package fr.lenours.sensortracker;

import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.os.Handler;
import android.util.Log;

import com.dobi.walkingsynth.accelerometer.AccelerometerDetector;
import com.dobi.walkingsynth.accelerometer.OnStepCountChangeListener;

import java.io.File;

/**
 * Classe qui permet de detecter la maniere dont se deplace l'utilisateur (la marche)
 * Created by Clemsbrowning on 24/05/2016.
 */
public class WalkingDetector implements OnStepCountChangeListener {


    public static int accuracy = 650;
    private Handler handler;
    private Runnable keepWalkingChecker;
    private OnUserWalkingChangeListener listener;
    private SensorManager sensorManager;
    private AccelerometerDetector stepDetector;
    private long launchTime;
    private boolean isWalking = false;
    private int walkingCounter = 1;

    /**
     * Constructeur
     * @param sensorManager
     */
    public WalkingDetector(SensorManager sensorManager) {
        this.sensorManager = sensorManager;
        handler = new Handler();

        //this runnable check if the user stop walking or not
        keepWalkingChecker = new Runnable() {
            @Override
            public void run() {
                if (System.currentTimeMillis() - launchTime > 3000) { //if you're not walking during 3 seconds
                    walkingCounter = 0;
                    isWalking = false;
                    listener.OnUserWalkingChange(false);
                    return;
                }
                handler.postDelayed(keepWalkingChecker, 0);
            }
        };
    }

    /**
     * Notifie les objets sur écoute
     * @param listener
     */
    public void setOnUserWalkingListener(OnUserWalkingChangeListener listener) {
        this.listener = listener;
        startListening();
    }

    /**
     * Démarre le detecteur
     */
    private void startListening() {
        listener.OnUserWalkingChange(false); //Not Walking by default
        launchTime = System.currentTimeMillis();
        stepDetector = new AccelerometerDetector(sensorManager);
        stepDetector.setStepCountChangeListener(this);
        stepDetector.startDetector();
    }

    /**
     * Detecte si l'utilisateur marche ou non, et appelle un Thread qui permet de suivre la marche
     * @param time la détection se fait par rapport au temps (en millisecondes)
     * @return
     */
    private boolean isWalking(long time) {
        walkingCounter++;
        handler.removeCallbacks(keepWalkingChecker);
        boolean isWalking = true;
        //check if time between 2 "steps" isn't long and user not already walking
        if ((time - launchTime > WalkingDetector.accuracy) && !this.isWalking) { //Not walking
            walkingCounter = 1;
            launchTime = time;
            return !isWalking;
        }
        launchTime = time;
        //if the  previous condition fail 5 times a row
        if (!this.isWalking && walkingCounter >= 5) { //Walking
            this.isWalking = true;
            listener.OnUserWalkingChange(true);
        }
        //if user is walking call a runnable
        if (this.isWalking) {
            handler.postDelayed(keepWalkingChecker, 0);
            return isWalking;
        }
        return !isWalking;
    }
    
    @Override
    public void onStepCountChange(SensorEvent event) {
        isWalking(System.currentTimeMillis());
    }
}