package fr.lenours.sensortracker;

import android.content.Context;
import android.graphics.Color;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.dobi.walkingsynth.accelerometer.AccelerometerDetector;
import com.dobi.walkingsynth.accelerometer.OnStepCountChangeListener;
import com.github.lzyzsd.circleprogress.ArcProgress;

import java.io.File;
import java.util.HashMap;
import java.util.List;


public class StepTrackerFragment2 extends Fragment implements OnStepCountChangeListener {

    public static final int PROGESS_COLOR = Color.rgb(18, 86, 136);
    public static int DAY = 1000 * 60 * 60 * 24;
    private File currentFile;
    private View view;
    private AccelerometerDetector accelDetector;
    private RelativeLayout oneDayLayout;
    private RelativeLayout someDaysLayout;
    private Vibrator vibrator;
    private Spinner daysSpinner;
    private Spinner modeSpinner;
    private Spinner daysSpinnerOne;
    private Spinner daysSpinnerTwo;
    private Spinner daysModeSpinner;
    private String[] modeSpinnerChoice = {"Pas", "Calories", "Distance"};
    private String[] daysModeSpinnerChoice = {"Sur un jour", "Sur plusieurs jours"};
    private ArcProgress stepArcProgress;
    private ArcProgress caloriesArcProgress;
    private ArcProgress distanceArcProgress;
    private TextView dailyObjectiveText;
    private Button bValidMultipleDay;
    private boolean isToday = true;
    private OnFragmentInteractionListener mListener;
    private int lastDaysModeSpinnerPosition = 0;

    public StepTrackerFragment2() {
        // Required empty public constructor
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_step_tracker, container, false);

        currentFile = new File(FileData.folder + "tracker/" + Extra.getDateAsString(System.currentTimeMillis()) + "-tracker.txt");

        vibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
        oneDayLayout = (RelativeLayout) view.findViewById(R.id.firstMode);
        someDaysLayout = (RelativeLayout) view.findViewById(R.id.secondMode);
        daysSpinner = (Spinner) view.findViewById(R.id.daysSpinner);
        modeSpinner = (Spinner) view.findViewById(R.id.modeSpinner);
        daysSpinnerOne = (Spinner) view.findViewById(R.id.daysSpinnerOne);
        daysSpinnerTwo = (Spinner) view.findViewById(R.id.daysSpinnerTwo);
        daysModeSpinner = (Spinner) view.findViewById(R.id.daysSelector);
        dailyObjectiveText = (TextView) view.findViewById(R.id.dailyStepView);
        bValidMultipleDay = (Button) view.findViewById(R.id.bValidMultipleDay);
        stepArcProgress = (ArcProgress) view.findViewById(R.id.stepArcProgress);
        caloriesArcProgress = (ArcProgress) view.findViewById(R.id.caloriesArcProgress);
        distanceArcProgress = (ArcProgress) view.findViewById(R.id.distanceArcProgress);

        stepArcProgress.setUnfinishedStrokeColor(Color.rgb(220, 220, 220));
        stepArcProgress.setFinishedStrokeColor(PROGESS_COLOR);
        distanceArcProgress.setUnfinishedStrokeColor(Color.rgb(220, 220, 220));
        distanceArcProgress.setFinishedStrokeColor(PROGESS_COLOR);
        caloriesArcProgress.setUnfinishedStrokeColor(Color.rgb(220, 220, 220));
        caloriesArcProgress.setFinishedStrokeColor(PROGESS_COLOR);

        configArcs(StepData.objective_step, 0);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item, StepData.getlastXDays(14, 1, false));
        daysSpinner.setAdapter(adapter);
        adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item, StepData.getlastXDays(14, 1, true));
        daysSpinnerOne.setAdapter(adapter);
        daysSpinnerTwo.setAdapter(adapter);
        adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item, modeSpinnerChoice);
        modeSpinner.setAdapter(adapter);
        adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item, daysModeSpinnerChoice);
        daysModeSpinner.setAdapter(adapter);
        daysSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    restoreViews(Extra.getDateAsString(System.currentTimeMillis()), isToday = true);
                } else if (hasStepCount(System.currentTimeMillis() - (DAY * position))) {
                    restoreViews(Extra.getDateAsString(System.currentTimeMillis() - (DAY * position)), isToday = false);
                } else {
                    noFoundData(Extra.timeMillisToDate(System.currentTimeMillis() - (DAY * position)));
                    return;
                }
                dailyObjectiveText.setText("");
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        modeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        stepArcProgress.setVisibility(ArcProgress.VISIBLE);
                        distanceArcProgress.setVisibility(ArcProgress.GONE);
                        caloriesArcProgress.setVisibility(ArcProgress.GONE);
                        break;
                    case 1:
                        stepArcProgress.setVisibility(ArcProgress.GONE);
                        distanceArcProgress.setVisibility(ArcProgress.GONE);
                        caloriesArcProgress.setVisibility(ArcProgress.VISIBLE);
                        break;
                    case 2:
                        stepArcProgress.setVisibility(ArcProgress.GONE);
                        distanceArcProgress.setVisibility(ArcProgress.VISIBLE);
                        caloriesArcProgress.setVisibility(ArcProgress.GONE);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        daysModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        oneDayLayout.setVisibility(RelativeLayout.VISIBLE);
                        someDaysLayout.setVisibility(RelativeLayout.GONE);
                        lastDaysModeSpinnerPosition = 0;
                        break;
                    case 1:
                        oneDayLayout.setVisibility(RelativeLayout.GONE);
                        someDaysLayout.setVisibility(RelativeLayout.VISIBLE);
                        lastDaysModeSpinnerPosition = 1;
                        configArcs(0, 0);
                        break;
                }
                if (lastDaysModeSpinnerPosition == 0)
                    restoreViews(Extra.getDateAsString(System.currentTimeMillis() - (DAY * daysSpinner.getSelectedItemPosition())), isToday = true);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        bValidMultipleDay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (daysSpinnerOne.getSelectedItemPosition() >= daysSpinnerTwo.getSelectedItemPosition()) {
                    Toast.makeText(getActivity(), "Veuillez corriger les dates entrés", Toast.LENGTH_SHORT).show();
                    return;
                }
                int totalStep = 0;
                int data;
                int cpt = 1;
                String EndDate = Extra.getDateAsString(System.currentTimeMillis() - (DAY * (14 - daysSpinnerTwo.getSelectedItemPosition() - 1)));
                Log.i("EndDate", EndDate + " " + daysSpinnerTwo.getSelectedItemPosition());
                long curDate = 0;
                while (!Extra.getDateAsString(curDate).equals(EndDate)) {
                    curDate = System.currentTimeMillis() - (DAY * (14 - cpt - daysSpinnerOne.getSelectedItemPosition()));
                    cpt++;
                    /*if (new File(FileData.folder + "tracker/" + curDate + "-tracker.txt").exists()) {
                        data = CsvReader.readCSV(new File(FileData.folder + "tracker/" + curDate + "-tracker.txt"), ",");
                        totalStep += Integer.parseInt(data.get(0)[0]);
                    }*/
                    if (hasStepCount(curDate)) {
                        data = getStepByDate(Extra.getDateAsString(curDate));
                        totalStep += data;
                    }
                    Log.i("step", "" + totalStep);
                }
                configArcs(1, totalStep);
            }
        });

        accelDetector = new AccelerometerDetector((SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE));
        accelDetector.setStepCountChangeListener(this);
        accelDetector.startDetector();

        restoreViews(Extra.getDateAsString(System.currentTimeMillis()), true);
        return view;
    }

    private void noFoundData(String date) {
        dailyObjectiveText.setText(("Pas de données trouvé pour le " + date).substring(0, ("Pas de données trouvé pour le " + date).length() - 6));
        configArcs(0, 0);
    }

    public void configArcs(int max, int progress) {
        stepArcProgress.setProgress(progress);
        stepArcProgress.setMax(max);
        distanceArcProgress.setProgress(stepToMeter(progress));
        distanceArcProgress.setMax(1);
        caloriesArcProgress.setProgress(stepToCalories(progress));
        caloriesArcProgress.setMax(1);
    }

    private void restoreViews(String date, boolean isToday) {
        StepData.objective_step = getObjective();
        if (isToday) {
            StepData.day_step = getStepByDate(null);
            updateViews();
        } else {
            StepData.day_step = getStepByDate(date);
            configArcs(1, getStepByDate(date));
        }
    }

    private int getObjective() {

        List<String[]> step = CsvReader.readCSV(FileData.CURRENT_DAY, ",");
        return Integer.parseInt(step.get(0)[2]);
    }

    public void updateViews() {

        if (isToday) {
            configArcs(StepData.objective_step, StepData.day_step);
        }

        if (StepData.day_step >= StepData.objective_step) {
            stepArcProgress.setTextColor(Color.RED);
            distanceArcProgress.setTextColor(Color.RED);
            caloriesArcProgress.setTextColor(Color.RED);
            //new AlertDialog.Builder(getActivity()).setMessage("You reach " + StepData.objective_step + " steps for this fay, congratulations !").show();
            //   reset();
        }
    }

    private void saveData() {

        String[] lastLine = CsvReader.getLastLine(FileData.TOTAL_STEP);
        if ( lastLine != null  && lastLine[0].equals(Extra.getDateAsString(System.currentTimeMillis()))) {
            CsvReader.tail(FileData.TOTAL_STEP,1,true);
        }
        CsvReader.writeCSV(FileData.TOTAL_STEP,Extra.getDateAsString(System.currentTimeMillis()) + "," + StepData.day_step + "," + StepData.objective_step,true);
        StepData.dayChanged = true;
        StepData.day_step = 0;
    }

    public void updateObjective() {
        CsvReader.writeCSV(new File(FileData.folder + "tracker/" + Extra.getDateAsString(System.currentTimeMillis()) + "-tracker.txt"), StepData.day_step + "," + StepData.objective_step, false);
        stepArcProgress.invalidate();
        distanceArcProgress.invalidate();
        caloriesArcProgress.invalidate();
    }

    public boolean hasStepCount(long time) {
        List<String[]> steps = CsvReader.readCSV(FileData.TOTAL_STEP, ",");
        for (int i = 0; i < steps.size(); i++) {
            if (steps.get(i)[0].equals(Extra.getDateAsString(time)))
                return true;
        }
        return false;
    }

    public int getStepByDate(String date) {
        if (date == null) {
            List<String[]> step = CsvReader.readCSV(FileData.CURRENT_DAY, ",");
            return Integer.parseInt(step.get(0)[1]);
        }
        HashMap<String, String> steps = CsvReader.readCSV(FileData.TOTAL_STEP);
        return Integer.parseInt(steps.get(date));
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
        Log.i("StepTrackerFragment", "onDetached called");
        mListener = null;
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i("StepTrackerFragment", "onPause");
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i("StepTrackerhFragment", "onStart");
    }

    @Override
    public void onStepCountChange(SensorEvent event) {
        if (StepData.isWalking) {
            updateViews();
            saveData();
            Log.i("StepTrackerFragment", "OnStepCountChange called");
            daysSpinner.setSelection(0);
        }
    }

    public void removeAllRecord() {
        for (File file : new File(FileData.folder + "tracker/").listFiles()) {
            file.delete();
        }
    }


    public int stepToCalories(int steps) {
        return steps / 15;
    }

    public int stepToMeter(int step) {
        return (int) (step * 0.8);
    }


    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
