package fr.lenours.sensortracker;

//import android.graphics.Color;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.osmdroid.util.GeoPoint;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Classe outil
 * Created by Clemsbrowning on 26/04/2016.
 */
public class Extra {

    public static String TAG = "Extra";

    public static final String NETWORK_OFFLINE = "Network is unavailable";


    /**
     * set number of decimals of double value
     * @param value to set number of decimals
     * @param digitsAfterDot number of decimals
     * @return
     */
    public static Double decimalFormat(double value, int digitsAfterDot) {
        return Double.parseDouble(String.format("%." + Integer.toString(digitsAfterDot) + "f", value).replace(",", "."));
    }

    /**
     * get the clast known current location
     * @param systemService
     * @param activity
     * @return
     */
    public static GeoPoint currentLocation(Object systemService, Activity activity) {
        LocationManager locManager = (LocationManager) systemService;
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return null;
        }
        Location location = locManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
        if (location == null)
            return null;
        return new GeoPoint(location.getLatitude(), location.getLongitude());
    }

    /**
     * get current location information like country, city, etc. Internet needed
     * @param systemService
     * @param activity
     * @param point
     * @return
     */
    public static String getCityFromPoint(Object systemService, Activity activity, GeoPoint point) {
        if (!MainActivity.isNetworkAvailable())
            return NETWORK_OFFLINE;
        LocationManager locManager = (LocationManager) systemService;
        Geocoder info = new Geocoder(activity, Locale.getDefault());
        List<Address> address = null;
        try {
            address = info.getFromLocation(point.getLatitude(), point.getLongitude(), 1);
        } catch (IOException e) {
            e.printStackTrace();
            return NETWORK_OFFLINE;
        }

        return address.get(0).getAddressLine(0) + " " + address.get(0).getPostalCode() + " " + address.get(0).getLocality();
    }


    /**
     * A COMPLETER
     * @param list
     * @return
     */
    public static DataPoint[] listToDataPoint(List<String[]> list) {
        DataPoint[] values = new DataPoint[list.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = new DataPoint(Double.parseDouble(list.get(i)[0]), Double.parseDouble(list.get(i)[1]));
        }
        return values;
    }

    public static File createFileFromInputStream(InputStream inputStream, String path) {

        try {
            File f = new File(path);
            f.createNewFile();
            OutputStream outputStream = new FileOutputStream(f);
            byte buffer[] = new byte[1024];
            int length = 0;

            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.close();
            inputStream.close();

            return f;
        } catch (IOException e) {
            System.out.println("ERROR !!!!!" + e.toString());
        }

        return null;
    }

    /**
     * Open Application by the package name
     * @param context
     * @param packageName
     * @return
     */
    public static boolean openApp(Context context, String packageName) {
        PackageManager manager = context.getPackageManager();
        Intent i = manager.getLaunchIntentForPackage(packageName);
        if (i == null) {
            return false;
        }
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        context.startActivity(i);
        return true;
    }

    /**
     *
     * @param context
     * @param packageName
     * @return true if the package is installed, false otherwise
     */
    public static boolean isPackageInstalled(Context context, String packageName) {
        final PackageManager packageManager = context.getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(packageName);
        if (intent == null) {
            return false;
        }
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    /**
     *
     * @param step
     * @return distance in meter
     */
    public static int stepToMeter(int step) {
                return (int) (step * 0.762);
    }

    /**
     * Retrieve a date by millisecond time
     * @param time
     * @return a Date
     */
    public static String timeMillisToDate(long time) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy");
        Date resultdate = new Date(time);
        return sdf.format(resultdate);
    }

    /**
     * create date String by time in millisecond
     * @param time
     * @return date String in day-month-year
     */
    public static String getDateAsString(long time) {
        Log.i(TAG, new SimpleDateFormat("dd-MM-yyyy").format(new Date(time)));
        return new SimpleDateFormat("dd-MM-yyyy").format(new Date(time));
    }


    /**
     * caulculate avergae of a list
     * @param marks
     * @return average
     */
    public static double calculateAverage(List <Double> marks) {
        Double sum = 0.0;
        if(!marks.isEmpty()) {
            for (Double mark : marks) {
                sum += mark;
            }
            return sum.doubleValue() / marks.size();
        }
        return sum;
    }

    /*public static XYPlot lightPlot(XYPlot plot) {
        plot.getGraphWidget().setSize(new SizeMetrics(0, SizeLayoutType.FILL, 0, SizeLayoutType.FILL));
        plot.getGraphWidget().setBackgroundPaint(new Paint());
        plot.getGraphWidget().setGridBackgroundPaint(new Paint());
        plot.getGraphWidget().getBackgroundPaint().setColor(Color.WHITE);
        plot.getGraphWidget().getGridBackgroundPaint().setColor(Color.WHITE);
        plot.getGraphWidget().getDomainLabelPaint().setColor(Color.BLACK);
        plot.getGraphWidget().getRangeLabelPaint().setColor(Color.BLACK);
        plot.getGraphWidget().getDomainOriginLabelPaint().setColor(Color.BLACK);
        plot.getGraphWidget().getDomainOriginLinePaint().setColor(Color.BLACK);
        plot.getGraphWidget().getRangeOriginLinePaint().setColor(Color.BLACK);
        return plot;
    }

    public static XYPlot darkPlot(XYPlot plot) {
        plot.getGraphWidget().setSize(new SizeMetrics(0, SizeLayoutType.FILL, 0, SizeLayoutType.FILL));
        plot.getGraphWidget().setBackgroundPaint(new Paint());
        plot.getGraphWidget().setGridBackgroundPaint(new Paint());
        plot.getGraphWidget().getBackgroundPaint().setColor(Color.BLACK);
        plot.getGraphWidget().getGridBackgroundPaint().setColor(Color.BLACK);
        plot.getGraphWidget().getDomainLabelPaint().setColor(Color.WHITE);
        plot.getGraphWidget().getRangeLabelPaint().setColor(Color.WHITE);
        plot.getGraphWidget().getDomainOriginLabelPaint().setColor(Color.WHITE);
        plot.getGraphWidget().getDomainOriginLinePaint().setColor(Color.WHITE);
        plot.getGraphWidget().getRangeOriginLinePaint().setColor(Color.WHITE);
        return plot;
    }*/
}