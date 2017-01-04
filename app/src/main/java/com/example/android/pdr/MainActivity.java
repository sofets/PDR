package com.example.android.pdr;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.ArrayList;

import static android.R.id.list;
import static android.hardware.Sensor.TYPE_ACCELEROMETER;
import static android.hardware.Sensor.TYPE_MAGNETIC_FIELD;
import static android.hardware.Sensor.TYPE_STEP_COUNTER;
import static android.hardware.Sensor.TYPE_STEP_DETECTOR;


public class MainActivity extends AppCompatActivity implements SensorEventListener {

    SensorManager mSensorManager;
    Sensor mStepDetector;
    Sensor mStepCounter;
    Sensor mAccelerometer;
    Sensor mMagneticField;

    static final String COUNTED_STEPS = "counted steps";
    static final String DETECTED_STEPS = "detected steps";
    static final String INITIAL_COUNTED_STEPS = "initial detected steps";


    Boolean isSensorStepDetectorPresent = false;
    Boolean isSensorStepCounterPresent = false;
    Boolean isSensorAccelerometerPresent = false;
    Boolean isSensorMagneticFieldPresent = false;

    Boolean lastAccelerometerSet = false;
    Boolean lastMagnetometerSet = false;
    Boolean isOrientationSet = false;

    int numberOfStepsDetected = 0;
    int numberOfStepsCounted = 0;

    int initialStepCounterValue = 0;

    String orientation;

    float [] lastAccelerometer = new float[3];
    float [] lastMagnetometer = new float[3];

    float [] mRotationMatrix = new float[9];
    float [] mOrientationAngles = new float[3];

    //long[] stepTimeStamp;


    ArrayList<Long> stepTimeStamp = new ArrayList<Long>();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null) {
            mStepCounter = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

            isSensorStepCounterPresent = true;
        }


        //TextView countedSteps = (TextView) findViewById(R.id.countedStepsTextView);
        //countedSteps.setText("" + isSensorStepCounterPresent);


        if (mSensorManager.getDefaultSensor(TYPE_STEP_DETECTOR) != null) {
            mStepDetector = mSensorManager.getDefaultSensor(TYPE_STEP_DETECTOR);

            isSensorStepDetectorPresent = true;
        }

        //TextView detectedSteps = (TextView) findViewById(R.id.stepsDetectedTextView);
        //detectedSteps.setText("" + isSensorStepDetectorPresent);


        if (mSensorManager.getDefaultSensor(TYPE_ACCELEROMETER) != null) {
            mAccelerometer = mSensorManager.getDefaultSensor(TYPE_ACCELEROMETER);

            isSensorAccelerometerPresent = true;
        }

        if (mSensorManager.getDefaultSensor(TYPE_MAGNETIC_FIELD) != null) {
            mMagneticField = mSensorManager.getDefaultSensor(TYPE_MAGNETIC_FIELD);

            isSensorMagneticFieldPresent = true;
        }

    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        switch (event.sensor.getType()) {
            case TYPE_STEP_DETECTOR:

                numberOfStepsDetected++;

                stepTimeStamp.add(event.timestamp);

                if (lastAccelerometerSet && lastMagnetometerSet)
                {
                    mSensorManager.getRotationMatrix(mRotationMatrix, null, lastAccelerometer, lastMagnetometer);
                    mSensorManager.getOrientation(mRotationMatrix, mOrientationAngles);

                    float azimuthInRadians = mOrientationAngles[0];
                    float pitchInRadians = mOrientationAngles[1];
                    float rollInRadians = mOrientationAngles[2];

                    int azimuthInDegress = (int)(((azimuthInRadians * 180/(float)Math.PI) + 360) % 360);

                    isOrientationSet = true;

                    //orientation = Float.toString(azimuthInRadians);
                    orientation = "" + azimuthInDegress;

                }

                TextView detectedSteps = (TextView) findViewById(R.id.stepsDetectedTextView);
                detectedSteps.setText("" + numberOfStepsDetected);

                TableLayout table = (TableLayout) findViewById(R.id.tableLayoutView);
                TableRow row = new TableRow(this);
                TextView view = new TextView(this);
                view.setText("" + numberOfStepsDetected);
                row.addView(view, 0);

                TextView view1 = new TextView(this);
                view1.setText(stepTimeStamp.get(numberOfStepsDetected - 1).toString());
                row.addView(view1, 1);

                if (isOrientationSet)
                {
                    TextView view2 = new TextView(this);
                    view2.setText(orientation);
                    row.addView(view2, 2);

                }

                table.addView(row);

                //TextView numberOfStepView = (TextView) findViewById(numberOfStepView);
                //numberOfStepView.setText("" + numberOfStepsDetected);

                //TextView stepTimeStampView = (TextView) findViewById(R.id.stepTimeStampView);
                //stepTimeStampView.setText("" + event.timestamp);
                //stepTimeStampView.setText("" + stepTimeStamp.get(numberOfStepsDetected - 1));

                break;

            case TYPE_STEP_COUNTER:

                if (initialStepCounterValue < 1) {
                    initialStepCounterValue = (int) event.values[0];
                }

                numberOfStepsCounted = (int) event.values[0] - initialStepCounterValue;

                TextView countedSteps = (TextView) findViewById(R.id.countedStepsTextView);
                countedSteps.setText(String.valueOf(numberOfStepsCounted));
                break;

            case TYPE_ACCELEROMETER:

                lastAccelerometerSet = true;

                System.arraycopy(event.values, 0, lastAccelerometer, 0, event.values.length);

                break;

            case TYPE_MAGNETIC_FIELD:

                lastMagnetometerSet = true;

                System.arraycopy(event.values, 0, lastMagnetometer, 0, event.values.length);

                if (lastAccelerometerSet && lastMagnetometerSet)
                {
                    mSensorManager.getRotationMatrix(mRotationMatrix, null, lastAccelerometer, lastMagnetometer);
                    mSensorManager.getOrientation(mRotationMatrix, mOrientationAngles);

                    float azimuthInRadians = mOrientationAngles[0];
                    float pitchInRadians = mOrientationAngles[1];
                    float rollInRadians = mOrientationAngles[2];

                    int azimuthInDegress = ((int)(azimuthInRadians * 180/(float)Math.PI) + 360) % 360;

                    TextView orientationView = (TextView) findViewById(R.id.orientationTextView);
                    orientationView.setText(Float.toString(azimuthInDegress));

                }

                break;

        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isSensorStepDetectorPresent) {
            mSensorManager.unregisterListener(this, mStepDetector);
        }

        if (isSensorStepCounterPresent) {
            mSensorManager.unregisterListener(this, mStepCounter);
        }

        if (isSensorAccelerometerPresent) {
            mSensorManager.unregisterListener(this, mAccelerometer);
        }

        if (isSensorMagneticFieldPresent) {
            mSensorManager.unregisterListener(this, mMagneticField);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (isSensorStepCounterPresent) {
            mSensorManager.registerListener(this, mStepCounter,
                    SensorManager.SENSOR_DELAY_FASTEST);
        }
        if (isSensorStepDetectorPresent) {
            mSensorManager.registerListener(this, mStepDetector,
                    SensorManager.SENSOR_DELAY_FASTEST);
        }
        if (isSensorAccelerometerPresent) {
            mSensorManager.registerListener(this, mAccelerometer,
                    SensorManager.SENSOR_DELAY_UI);
        }
        if (isSensorMagneticFieldPresent) {
            mSensorManager.registerListener(this, mMagneticField,
                    SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {

        super.onSaveInstanceState(savedInstanceState);

        // Save the user's current game state
        savedInstanceState.putInt(DETECTED_STEPS, numberOfStepsDetected);
        savedInstanceState.putInt(COUNTED_STEPS, numberOfStepsCounted);
        savedInstanceState.putInt(INITIAL_COUNTED_STEPS, initialStepCounterValue);
    }


    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        // Restore state members from saved instance
        numberOfStepsDetected = savedInstanceState.getInt(DETECTED_STEPS);
        numberOfStepsCounted = savedInstanceState.getInt(COUNTED_STEPS);
        initialStepCounterValue = savedInstanceState.getInt(INITIAL_COUNTED_STEPS);
    }
}
