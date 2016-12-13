package fr.lenours.sensortracker;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Permet de lire/écrire dans des fichiers
 */
public class CsvReader {
    public static String TAG = "CsvReader";

    /**
     * @param file
     * @param separator
     * @return list of tring[] represeting lines
     */
    public static List<String[]> readCSV(File file, String separator) {

        if (!file.exists())
            return null;

        List<String[]> result = new ArrayList<>();

        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                String[] sLine = line.split(separator);
                result.add(sLine);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * @param file
     * @return List of String line by line
     */
    public static List<String> readCSVNoArray(File file) {

        if (!file.exists())
            return null;

        List<String> result = new ArrayList<>();

        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                result.add(line);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }


    /**
     * @param file
     * @param text
     * @param atTheEnd if true, add the string a the end of the file otherwise it will overwrite the file
     */
    public static void writeCSV(File file, String text, boolean atTheEnd) {

        Log.i("tag","bytes written in " + file.getPath() + " : \"" + text + "\""    );

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file, atTheEnd);
            fos.write((text + "\n").getBytes());
            //Log.i(TAG, "Succesfully write \"" + text + "\" to " + file.getPath());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.i(TAG, "File " + file.getPath() + " not found");
        } catch (IOException e) {
            e.printStackTrace();
            Log.i(TAG, "Can't write bytes to " + file.getPath());
        }
    }

    /**
     * @param file
     * @return hashmap with the first line element as the key and the second as the value
     */
    public static HashMap<String, String> readCSV(File file) {
        HashMap<String, String> map = new HashMap<>();

        if (!file.exists())
            return null;

        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                String[] sLine = line.split(",");
                map.put(sLine[0], sLine[1]);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return map;
    }


    /**
     * remove nbLines lines from the file
     *
     * @param file
     * @param nbLines to remove at the beginning or end of the file
     */
    public static void tail(File file, int nbLines, boolean atTheEnd) {
        List<String> text = CsvReader.readCSVNoArray(file);
        file.delete();
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (!atTheEnd) {
            for (int i = 0; i < text.size(); i++) {
                if (i < nbLines) continue;
                CsvReader.writeCSV(file, text.get(i), true);
            }
        } else {
            for (int i = 0 ; i < text.size() - nbLines ; i++) {
                CsvReader.writeCSV(file,text.get(i),true);
            }
        }
    }

    public static String[] getLastLine(File file) {
        if (!file.exists()) return null;
        List<String[]> str = CsvReader.readCSV(file,",");
        return str.get(str.size()-1);
    }

    public static int getLinesNumber(File file) {
        return CsvReader.readCSV(file).size();
    }
}
