package fr.lenours.sensortracker;

import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session;
import com.dropbox.client2.session.TokenPair;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import fr.lenours.dropboxapi.DropboxAuthActivity;
import fr.lenours.dropboxapi.GetDBFile;
import fr.lenours.dropboxapi.UploadFile;

public class MainActivity extends AppCompatActivity implements OSMMapFragment.OnFragmentInteractionListener, StepGraphFragment2.OnFragmentInteractionListener, SharedPreferences.OnSharedPreferenceChangeListener, StepTrackerFragment2.OnFragmentInteractionListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    public static String PACKAGE_NAME;
    private static ConnectivityManager cm;
    private Toolbar toolbar;
    private TabLayout tabLayout;
    private OSMMapFragment osmFrag;
    private boolean firstUse = true;
    private EditText setupText;
    private SharedPreferences sp;
    private ListView legend;
    private StepTrackerFragment2 stepTrackerFragment;
    private StepGraphFragment2 stepGraphFragment2;
    private SensorManager sensorManager;
    private FileData fd;

    public static String TAG = "MainActivity";
    public static boolean onDestroyCalled = false;
    private final static String DROPBOX_FILE_DIR = "/DropboxDemo/";
    private final static String DROPBOX_NAME = "dropbox_prefs";
    private final static String ACCESS_KEY = "zuj8q0szm7xdq6p";
    private final static String ACCESS_SECRET = "fajd2zi0azmjf1w";
    private final static Session.AccessType ACCESS_TYPE = Session.AccessType.DROPBOX;
    private DropboxAPI dropboxApi;
    private boolean isUserLoggedIn = false;
    public static GoogleApiClient googleClient;

    public static boolean isNetworkAvailable() {
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        PACKAGE_NAME = getPackageName();
        FileData.checkJSON();
        try {
            if (FileData.configFile.getBoolean("init"))
                firstUse = false;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        initDropbox();
        initGoogleApiClient();


        //startService(new Intent(MainActivity.this, StepService.class));

        stepGraphFragment2 = new StepGraphFragment2();
        osmFrag = new OSMMapFragment();
        stepTrackerFragment = new StepTrackerFragment2();
        Object internet = getSystemService(Context.CONNECTIVITY_SERVICE);
        FileData.createAppFolder();

        //Preferences listener
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.registerOnSharedPreferenceChangeListener(this);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        new StepData(sensorManager);

        if (!((LocationManager) getSystemService(Context.LOCATION_SERVICE)).isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);

            alert.setTitle("Location disable");
            alert.setMessage("Please enable gps and restart application");
            alert.setCancelable(false);
            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                    finish();
                }
            });
            alert.show();
        } else if (firstUse) setup();
        else {
            setupUI();
            try {
                StepData.objective_step = FileData.configFile.getInt("stepObjective");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

    }

    private void initGoogleApiClient() {
        googleClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

    }

    private void setupViewPager(CustomViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(osmFrag, "carte");
        adapter.addFragment(stepTrackerFragment /*stepTrackerFragment*/, "accueil");
        adapter.addFragment(stepGraphFragment2,"graphe");
        //   adapter.addFragment(stepGraphFrag, "Graph");
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(1);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main_drawer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                startActivity(new Intent(MainActivity.this, Settings.class));
                break;
            case R.id.mapLegend:
                final Paint paint = new Paint();
                paint.setColor(Color.GREEN);
                paint.setStrokeWidth(8);

                legend = new ListView(this);
                String[] items = {"img", "texte"};
                ArrayList<HashMap<String, String>> listItem = new ArrayList<>();
                HashMap<String, String> map;
                map = new HashMap<>();
                map.put("texte", "Votre position");
                map.put("img", String.valueOf(R.drawable.loc48));
                listItem.add(map);

                map = new HashMap<>();
                map.put("texte", "Marqueur");
                map.put("img", String.valueOf(R.drawable.loc48marker));
                listItem.add(map);
                map = new HashMap<>();
                map.put("texte", "Vous marchez");
                map.put("img", String.valueOf(R.drawable.green_trace));
                listItem.add(map);

                map = new HashMap<>();
                map.put("texte", "Vous ne marchez pas");
                map.put("img", String.valueOf(R.drawable.blue_trace));
                listItem.add(map);

                SimpleAdapter simpleAdapter = new SimpleAdapter(this, listItem, R.layout.maplegenditem, items, new int[]{R.id.imgItem, R.id.textItem});
                legend.setAdapter(simpleAdapter);

                new AlertDialog.Builder(this)
                        .setTitle("Map legend\nx")
                        .setView(legend)
                        .setCancelable(true)
                        .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        }).show();
                break;

            case R.id.dbConnect:
                connectToDropbox();
                break;
            case R.id.dbSend:
                UploadFile stepFile = new UploadFile(this,dropboxApi,FileData.TOTAL_STEP);
                UploadFile curretnStepsFile = new UploadFile(this,dropboxApi,FileData.CURRENT_DAY);
                UploadFile configFile = new UploadFile(this,dropboxApi,FileData.CONFIG_FILE);

                stepFile.execute();
                curretnStepsFile.execute();
                configFile.execute();
            case R.id.dbReceive:
                new GetDBFile(dropboxApi,FileData.TOTAL_STEP,"steps.csv").execute();
        }
        return true;
    }

    /**
     * Initialise l'api Dropbox
     */
    private void initDropbox() {
        AppKeyPair appKeyPair = new AppKeyPair(ACCESS_KEY, ACCESS_SECRET);
        AndroidAuthSession session;

        SharedPreferences prefs = getSharedPreferences(DROPBOX_NAME, 0);
        String key = prefs.getString(ACCESS_KEY, null);
        String secret = prefs.getString(ACCESS_SECRET, null);

        if (key != null && secret != null) {
            AccessTokenPair token = new AccessTokenPair(key, secret);
            session = new AndroidAuthSession(appKeyPair, ACCESS_TYPE, token);
        } else {
            session = new AndroidAuthSession(appKeyPair, ACCESS_TYPE);
        }

        dropboxApi = new DropboxAPI(session);
    }

    private void connectToDropbox() {
        if(isUserLoggedIn){
            dropboxApi.getSession().unlink();
            loggedIn(false);
        } else {
            ((AndroidAuthSession) dropboxApi.getSession())
                    .startAuthentication(this);
        }
    }

    public void loggedIn(boolean userLoggedIn) {
        isUserLoggedIn = userLoggedIn;
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case "removeSteps":
                if (sharedPreferences.getBoolean("removeSteps", true)) {
                    stepTrackerFragment.removeAllRecord();
                    restartApp();
                }
                System.out.println("Remove steps ! bool = " + sharedPreferences.getBoolean("removeSteps", true));
                break;
            case "removeRoute":
                if (sharedPreferences.getBoolean("removeRoute", true)) {
                    FileData.ROUTE_FILE.delete();
                    restartApp();
                    osmFrag.resetRoute();
                }
                break;
            case "pas" :
                StepData.objective_step = Integer.parseInt(sharedPreferences.getString("pas",StepData.objective_step+""));
                stepTrackerFragment.updateObjective();
                restartApp();
        }
    }

    private void restartApp() {
        Extra.openApp(this, "fr.lenours.sensortracker");
        finish();System.exit(0);
    }

    public void setup() {

        fd = new FileData();

        setupText = new EditText(MainActivity.this);
        setupText.setInputType(InputType.TYPE_CLASS_NUMBER);

        new AlertDialog.Builder(MainActivity.this)
                .setTitle("Bienvenue dans SensorTracker")
                .setMessage("Veuillez entrer votre objectif de pas journalier :")
                .setCancelable(false)
                .setView(setupText)
                .setPositiveButton("ok !", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (setupText.getText().toString().equals("")) {
                            dialog.cancel();
                            setup();
                            return;
                        }
                        StepData.objective_step = Integer.parseInt(setupText.getText().toString());
                        CsvReader.writeCSV(FileData.CURRENT_DAY,Extra.getDateAsString(System.currentTimeMillis()) +","+0+","+StepData.objective_step,false);
                        initConfig();
                        setupUI();
                        firstUse = false;
                    }
                }).show();
    }

    /**
     * Initialise les fichiers de configuration
     */
    private void initConfig() {
        FileData.addConfigElement("init", "true");
        FileData.addConfigElement("hasBeenRestored", false);
        FileData.addConfigElement("stepObjective", StepData.objective_step);
        FileData.addConfigElement("osmMapInit", false);
        FileData.addConfigElement("graphInit", false);
        FileData.addConfigElement("trackerInit", false);
        FileData.addConfigElement("isGraphSet",false);
    }

    /**
     * Initialise les vues
     */
    public void setupUI() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        CustomViewPager viewPager = (CustomViewPager) findViewById(R.id.viewpager);
        setupViewPager(viewPager);
        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);
    }

    @Override
    protected void onStart() {
        super.onStart();
        FileData.checkJSON();
        googleClient.connect();
    }

    @Override
    protected void onStop() {
        if (null != googleClient && googleClient.isConnected()) {
            googleClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        FileData.checkJSON();

        //DB CHECK
        AndroidAuthSession session = (AndroidAuthSession) dropboxApi.getSession();
        if (session.authenticationSuccessful()) {
            try {
                session.finishAuthentication();

                TokenPair tokens = session.getAccessTokenPair();
                SharedPreferences prefs = getSharedPreferences(DROPBOX_NAME, 0);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(ACCESS_KEY, tokens.key);
                editor.putString(ACCESS_SECRET, tokens.secret);
                editor.commit();

                loggedIn(true);
            } catch (IllegalStateException e) {

            }
        }
    }


    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean("setup", false);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        // Always call the superclass so it can restore the view hierarchy
        super.onRestoreInstanceState(savedInstanceState);
        firstUse = savedInstanceState.getBoolean("setup");
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        onDestroyCalled = true;
        Log.i(TAG,"onDestroy called");
    }

}