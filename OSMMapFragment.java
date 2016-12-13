package fr.lenours.sensortracker;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.PathOverlay;

import java.util.ArrayList;
import java.util.List;

/**
 * Interface de la carte
 */
public class OSMMapFragment extends Fragment implements OnUserWalkingChangeListener, LocationListener {

    public static final int MARKER = 1;
    public static final int CURRENT_MARKER = 2;
    public static String TAG = "OSMMapFragment";
    public static double averageSpeed = 0;
    private MapView osmMap;
    private PathOverlay pathOverlay;
    private View view;
    private LocationManager locManager;
    private ArrayList<IGeoPoint> points;
    private GeoPoint current_loc;
    private GeoPoint last_loc;
    private OnFragmentInteractionListener mListener;
    private ImageButton bCenter;
    private ArrayList<OverlayItem> itemList;
    private Button bMarker;
    private Button bRouteRecording;
    private TextView currentLoc;
    private Marker startMarker;
    private Marker marker;
    private String markerInfo;
    private Object locService;
    private SensorManager sensorManager;
    private WalkingDetector walkingDetector;
    private boolean isWalking = false;
    private boolean routeRecording = false;
    private boolean markerWasSet = false;
    private int color = Color.GREEN;
    private List<Double> speeds;

    public OSMMapFragment() {
    }

    // TODO: Rename and change types and number of parameters
    public static OSMMapFragment newInstance(String param1, String param2) {
        OSMMapFragment fragment = new OSMMapFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_osmmap, container, false);

        locService = getActivity().getSystemService(Context.LOCATION_SERVICE);
        last_loc = Extra.currentLocation(locService, getActivity());

        //Views and objects init
        bCenter = (ImageButton) view.findViewById(R.id.bCenter);
        bMarker = (Button) view.findViewById(R.id.bMark);
        bRouteRecording = (Button) view.findViewById(R.id.routeRecording);
        currentLoc = (TextView) view.findViewById(R.id.tCurrentLoc);
        currentLoc.bringToFront();
        currentLoc.invalidate();
        locManager = (LocationManager) locService;
        points = new ArrayList<>();
        itemList = new ArrayList<>();
        markerInfo = new String();
        speeds = new ArrayList<>();

        initMap();
        if (FileData.ROUTE_FILE.exists()) restoreRoute();

        sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        walkingDetector = new WalkingDetector(sensorManager);
        walkingDetector.setOnUserWalkingListener(this);


        //When current location is updated
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return view;
        }
        locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        currentLoc.setText(Extra.getCityFromPoint(locService, getActivity(), last_loc));


        //Buttons listener init
        bMarker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!markerWasSet)
                    updateMarker(last_loc, OSMMapFragment.MARKER);
                markerWasSet = true;
            }
        });

        points.add(last_loc);

        bCenter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "okay in onClick");
                osmMap.getController().setCenter(Extra.currentLocation(locService, getActivity()));
                osmMap.getController().zoomTo(17);
            }
        });

        bRouteRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bRouteRecording.setText(routeRecording ? "enregistrer le tracé" : "Arrêter");
                routeRecording = !routeRecording;
            }
        });

        return view;
    }

    /**
     * Amméliore la précision du mouvement de la personne
     *
     * @return
     */
    private boolean hasMoved() {
        return (Math.abs(current_loc.getLongitude() - last_loc.getLongitude()) > 0.00001) && (Math.abs(current_loc.getLatitude() - last_loc.getLatitude()) > 0.00001);
    }

    /**
     * Initialise la carte
     */
    public void initMap() {
        osmMap = (MapView) view.findViewById(R.id.osmMap);
        osmMap.setClickable(true);
        osmMap.setTileSource(TileSourceFactory.MAPQUESTOSM);
        osmMap.getController().setZoom(17);
        pathOverlay = new PathOverlay(Color.BLUE, getActivity());
        pathOverlay.getPaint().setStrokeWidth(8);
        osmMap.getOverlays().add(pathOverlay);
        osmMap.setUseDataConnection(true);
        osmMap.setMultiTouchControls(true);
//        updateMarker(last_loc, OSMMapFragment.CURRENT_MARKER);
        Log.i(TAG, "Map successfully initialized!");
    }

    /**
     * Supprime la route
     */
    public void resetRoute() {
        points = new ArrayList<>();
        osmMap.getOverlays().clear();
        initMap();

        Log.i(TAG, "Resetting the route.. Success!");
    }

    /**
     * restore la route.
     */
    public void restoreRoute() {
        resetRoute();
        List<String[]> list = CsvReader.readCSV(FileData.ROUTE_FILE, ",");
        int color = Integer.parseInt(list.get(0)[2]);
        for (int i = 0; i < list.size(); i++) {
            if (color == Integer.parseInt(list.get(i)[2])) {
                updateRoute(new GeoPoint(Double.parseDouble(list.get(i)[0]), Double.parseDouble(list.get(i)[1])), true);
            } else {
                color = Integer.parseInt(list.get(i)[2]);
                pathOverlay = new PathOverlay(color, getActivity());
                pathOverlay.setColor(color);
                pathOverlay.getPaint().setStrokeWidth(8);
                pathOverlay.getPaint().setAlpha(150);
                osmMap.getOverlays().add(pathOverlay);
                points = new ArrayList<>();
                updateRoute(new GeoPoint(Double.parseDouble(list.get(i)[0]), Double.parseDouble(list.get(i)[1])), true);
            }
        }
    }

    /**
     * Met a jour la route avec une nouvelle coordonnées
     *
     * @param point
     * @param isRestoring Si ce n'est pas une restiration, sauegarde la route
     */
    public void updateRoute(GeoPoint point, boolean isRestoring) {
        points.add(point);
        pathOverlay.addPoint(point);

        Log.i(TAG, "Updating the route with point : (" + point.getLatitude() + "," + point.getLongitude() + ") .. Success!");
        if (!isRestoring) {
            CsvReader.writeCSV(FileData.ROUTE_FILE, Extra.decimalFormat(point.getLatitude(), 6) + "," + Extra.decimalFormat(point.getLongitude(), 6) + "," + color, true);
            osmMap.getOverlays().remove(pathOverlay);
            osmMap.getOverlays().add(pathOverlay);
        }
        osmMap.invalidate();
    }

    /**
     * @param point
     * @param type  Le type de marqueur
     */
    public void updateMarker(GeoPoint point, int type) {
        markerInfo = "Vous avez parcouru " + Extra.stepToMeter(StepData.day_step) + "m à pieds";
        Marker tempMarker = new Marker(osmMap);
        tempMarker.setPosition(point);
        tempMarker.setTitle(Extra.getCityFromPoint(locService, getActivity(), point));
        tempMarker.setSnippet(markerInfo);
        tempMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        switch (type) {
            case OSMMapFragment.CURRENT_MARKER:
                markerWasSet = false;
                osmMap.getOverlays().remove(startMarker);
                tempMarker.setIcon(ContextCompat.getDrawable(getActivity().getApplicationContext(), R.drawable.loc48));
                startMarker = tempMarker;
                osmMap.getOverlays().add(startMarker);
                osmMap.getController().setCenter(point);
                break;
            case OSMMapFragment.MARKER:
                markerWasSet = true;
                tempMarker.setIcon(ContextCompat.getDrawable(getActivity().getApplicationContext(), R.drawable.loc48marker));
                marker = tempMarker;
                osmMap.getOverlays().add(marker);
                osmMap.invalidate();
                break;
        }
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.i(TAG, "onDetached called");
        mListener = null;
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i(TAG, "onStart");
    }

    @Override
    public void onLocationChanged(Location location) {
        current_loc = new GeoPoint(location.getLatitude(), location.getLongitude());
        if (current_loc == null || !hasMoved() || location.getSpeed() <= 0.75) return;
        Log.d(TAG, "x = " + location.getLatitude() + " y = " + location.getLongitude());
        if (routeRecording) updateRoute(current_loc, false); //Update the map
        updateMarker(current_loc, OSMMapFragment.CURRENT_MARKER); //Set item marker
        last_loc = new GeoPoint(current_loc.getLatitude(), current_loc.getLongitude());
        osmMap.invalidate(); //Refresh the map
        CsvReader.writeCSV(FileData.newFile("speed.csv"), String.valueOf(location.getSpeed()), true);
        currentLoc.setText(Extra.getCityFromPoint(locService, getActivity(), current_loc));
        speeds.add((double) location.getSpeed());
        averageSpeed = Extra.calculateAverage(speeds);
    }

    @Override
    public void OnUserWalkingChange(boolean isWalking) {
        if (isWalking && !this.isWalking) {
            color = Color.GREEN;

        } else if (!isWalking && this.isWalking) {
            color = Color.BLUE;

        }
        Log.i(TAG, isWalking ? "Walking !" : "Not Walking !");
        PathOverlay pathOverlay = new PathOverlay(color, getActivity());
        pathOverlay.getPaint().setStrokeWidth(8);
        points = new ArrayList<>();
        points.add(last_loc);
        this.isWalking = isWalking;

    }


    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
