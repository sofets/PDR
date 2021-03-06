package com.example.android.pdr;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.ArrayList;

import static android.R.attr.button;
import static android.R.attr.x;
import static android.R.id.list;
import static android.hardware.Sensor.TYPE_ACCELEROMETER;

import static android.hardware.Sensor.TYPE_MAGNETIC_FIELD;
import static android.hardware.Sensor.TYPE_ROTATION_VECTOR;

import static android.hardware.Sensor.TYPE_STEP_COUNTER;
import static android.hardware.Sensor.TYPE_STEP_DETECTOR;
import static android.os.Build.VERSION_CODES.M;
import static com.example.android.pdr.R.id.meanFreq;


public class MainActivity extends AppCompatActivity implements SensorEventListener {

    SensorManager mSensorManager;
    Sensor mStepDetector;
    Sensor mStepCounter;
    Sensor mAccelerometer;
    Sensor mMagneticField;
    Sensor mRotationVector;
    Sensor mLinearAccelerometer;

    static final String COUNTED_STEPS = "counted steps";
    static final String DETECTED_STEPS = "detected steps";
    static final String INITIAL_COUNTED_STEPS = "initial detected steps";

    static double stepLengthConstant = 75;
    static double stepLengthHeight = 82;
    double stepLength;


    Boolean isSensorStepDetectorPresent = false;
    Boolean isSensorStepCounterPresent = false;
    Boolean isSensorAccelerometerPresent = false;
    Boolean isSensorMagneticFieldPresent = false;
    Boolean isSensorRotationVectorPresent = false;
    Boolean isSensorLinearAccelerationPresent = false;

    Boolean lastAccelerometerSet = false;
    Boolean lastMagnetometerSet = false;
    Boolean isOrientationSet = false;

    Boolean stepCountingActive = false;

    int numberOfStepsDetected = 0;
    int numberOfStepsCounted = 0;

    int orientationInDegrees = 0;

    int initialStepCounterValue = 0;

    double distance = 0;
    double distanceHeight = 0;
    double distanceFrequency = 0;
    double detectedStepsSensorValue = 0;
    double countedStepsSensorValue = 0;

    String orientation;

    float [] lastAccelerometer = new float[3];
    float [] lastMagnetometer = new float[3];

    float [] mRotationMatrix = new float[9];
    float [] mOrientationAngles = new float[3];

    double meanOrientationAngles = 0;
    double sumSinAngles = 0;
    double sumCosAngles = 0;
    long counter = 0;

    float [] mRotationMatrixFromVector = new float[16];

    //long[] stepTimeStamp;
    long timeCountingStarted = 0;
    long timeOfStep;
    double stepFrequency = 0;
    long totalTime = 0;

    double stepMeanFrequency = 0;
    double stepMeanTime = 0;
    double stepMeanAccDiff = 0;

    ArrayList<Long> stepTimeStamp = new ArrayList<Long>();

    double accelerationTotalMax = 0;
    double accelerationTotalMin = 0;
    double sumAccData = 0;

    double accelerationTotal = 0;

    static final float ALPHA = 0.25f;
    // test


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

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

        if (mSensorManager.getDefaultSensor(TYPE_ROTATION_VECTOR) != null) {
            mRotationVector = mSensorManager.getDefaultSensor(TYPE_ROTATION_VECTOR);

            isSensorRotationVectorPresent = true;
        }

        if (mSensorManager.getDefaultSensor(TYPE_STEP_COUNTER) != null) {
            mStepCounter = mSensorManager.getDefaultSensor(TYPE_STEP_COUNTER);

            isSensorStepCounterPresent = true;
        }

    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        switch (event.sensor.getType()) {
            case TYPE_STEP_DETECTOR:

                if (stepCountingActive) {

                    numberOfStepsDetected++;
                    detectedStepsSensorValue++;

                    distance = distance + stepLengthConstant;
                    distanceHeight = distanceHeight + stepLengthHeight;

                    stepTimeStamp.add(event.timestamp);

                    if (numberOfStepsDetected == 1){
                        timeOfStep = event.timestamp/1000000L - timeCountingStarted;
                        totalTime = 0;
                        distanceFrequency = distanceFrequency + stepLengthHeight;
                    }
                    else {
                        timeOfStep = (event.timestamp - stepTimeStamp.get((stepTimeStamp.size() - 1) - 1))
                                /1000000L;

                        if (timeOfStep > 1000 ){
                            counter = 0;
                            meanOrientationAngles = 0;
                            sumCosAngles = 0;
                            sumSinAngles = 0;
                        }

                        totalTime = totalTime + timeOfStep;
                        stepFrequency = 1000D / timeOfStep;

                        stepLength = 44 * stepFrequency + 4.4;

                        distanceFrequency = distanceFrequency + stepLength;

                        stepMeanFrequency = (detectedStepsSensorValue - 1)* 1000D / totalTime;
                        stepMeanTime = totalTime / (detectedStepsSensorValue -  1);

                        sumAccData = sumAccData + Math.sqrt(accelerationTotalMax - accelerationTotalMin);
                        stepMeanAccDiff = sumAccData / (detectedStepsSensorValue - 1);

                    }


                    if (lastAccelerometerSet && lastMagnetometerSet) {
                        mSensorManager.getRotationMatrix(mRotationMatrix, null, lastAccelerometer, lastMagnetometer);
                        mSensorManager.getOrientation(mRotationMatrix, mOrientationAngles);

                        float azimuthInRadians = mOrientationAngles[0];
                        float pitchInRadians = mOrientationAngles[1];
                        float rollInRadians = mOrientationAngles[2];

                        int azimuthInDegress = (int) (((azimuthInRadians * 180 / (float) Math.PI) + 360) % 360);

                        isOrientationSet = true;

                        //orientation = Float.toString(azimuthInRadians);
                        orientation = "" + azimuthInDegress;

                    }

                    TextView detectedSteps = (TextView) findViewById(R.id.stepsDetectedTextView);
                    detectedSteps.setText("" + detectedStepsSensorValue);

                    TextView distanceView = (TextView) findViewById(R.id.distanceTextView);
                    distanceView.setText(Double.toString(distance/100D));

                    TextView distanceHeightView = (TextView) findViewById(R.id.distanceHeight);
                    distanceHeightView.setText(Double.toString(distanceHeight/100D));

                    TextView distanceFrequencyView = (TextView) findViewById(R.id.distanceFreq);
                    distanceFrequencyView.setText(String.format("%.2f",distanceFrequency/100D));

                    TextView TotalTimeView = (TextView) findViewById(R.id.totalTime);
                    TotalTimeView.setText(Long.toString(totalTime));

                    TextView meanFreqView = (TextView) findViewById(R.id.meanFreq);
                    meanFreqView.setText(String.format("%.5f",stepMeanFrequency));

                    TextView meanAccqView = (TextView) findViewById(R.id.meanAccdiff);
                    meanAccqView.setText(String.format("%.5f",stepMeanAccDiff));

                    TableLayout table = (TableLayout) findViewById(R.id.tableLayoutView);
                    TableRow row = new TableRow(this);
                    TextView view = new TextView(this);
                    view.setText("" + numberOfStepsDetected);
                    row.addView(view, 0);

                    TextView view1 = new TextView(this);
                    view1.setText(String.format("%.2f", stepFrequency));
                    row.addView(view1, 1);

                    TextView view2 = new TextView(this);
                    view2.setText(String.format("%.3f", stepLength/100D));
                    row.addView(view2, 2);

                    TextView view3 = new TextView(this);
                    view3.setText(String.format("%.3f", accelerationTotalMax));
                    row.addView(view3, 3);

                    TextView view4 = new TextView(this);
                    view4.setText(Integer.toString(orientationInDegrees));
                    row.addView(view4, 4);

                    if (counter > 0){

                        TextView view5 = new TextView(this);
                        //view5.setText(Integer.toString(((int)Math.toDegrees(meanOrientationAngles/counter) +360) % 360));
                        view5.setText(Integer.toString(((int)Math.toDegrees(Math.atan2(sumSinAngles, sumCosAngles)) +360) % 360));

                        row.addView(view5, 5);
                    }


                    table.addView(row);

                    accelerationTotalMax = 0;
                    accelerationTotalMin = 0;
                    counter = 0;
                    meanOrientationAngles = 0;
                    sumCosAngles = 0;
                    sumSinAngles = 0;

                    //TextView numberOfStepView = (TextView) findViewById(numberOfStepView);
                    //numberOfStepView.setText("" + numberOfStepsDetected);

                    //TextView stepTimeStampView = (TextView) findViewById(R.id.stepTimeStampView);
                    //stepTimeStampView.setText("" + event.timestamp);
                    //stepTimeStampView.setText("" + stepTimeStamp.get(numberOfStepsDetected - 1));
                }


                break;

            case TYPE_STEP_COUNTER:


                if (stepCountingActive) {

                    if (initialStepCounterValue < 1) {
                        initialStepCounterValue = (int) event.values[0];
                    }

                    numberOfStepsCounted = (int) event.values[0] - initialStepCounterValue;

                    if (numberOfStepsCounted > numberOfStepsDetected) {

                        distance = distance + (numberOfStepsCounted - numberOfStepsDetected) * stepLengthConstant;
                        distanceHeight = distanceHeight + (numberOfStepsCounted - numberOfStepsDetected) * stepLengthHeight;

                        if (stepFrequency > 0){
                            distanceFrequency += (numberOfStepsCounted - numberOfStepsDetected) * (stepLength);
                        }else {
                            distanceFrequency += (numberOfStepsCounted - numberOfStepsDetected) * stepLengthHeight;
                        }

                        numberOfStepsDetected = numberOfStepsCounted;

                        //TextView detectedSteps = (TextView) findViewById(R.id.stepsDetectedTextView);
                        //detectedSteps.setText("" + numberOfStepsDetected);

                        TextView distanceView = (TextView) findViewById(R.id.distanceTextView);
                        distanceView.setText(Double.toString(distance/100D));

                        TextView distanceHeightView = (TextView) findViewById(R.id.distanceHeight);
                        distanceHeightView.setText(Double.toString(distanceHeight/100D));

                        TextView distanceFrequencyView = (TextView) findViewById(R.id.distanceFreq);
                        distanceFrequencyView.setText(String.format("%.2f",distanceFrequency/100D));

                    }

                    TextView countedSteps = (TextView) findViewById(R.id.countedStepsTextView);
                    countedSteps.setText(String.valueOf(numberOfStepsCounted));

                    TableLayout table = (TableLayout) findViewById(R.id.tableLayoutView);
                    TableRow row = new TableRow(this);
                    TextView view = new TextView(this);
                    view.setText("" + numberOfStepsCounted);
                    row.addView(view, 0);

                    table.addView(row);


                }
                else {
                    initialStepCounterValue = (int) event.values[0];

                }

                break;

            case TYPE_ACCELEROMETER:

                lastAccelerometerSet = true;

                System.arraycopy(event.values, 0, lastAccelerometer, 0, event.values.length);


                if (stepCountingActive && numberOfStepsDetected > 0){

                    long timeElapsedFromLastStep
                            = event.timestamp - stepTimeStamp.get(stepTimeStamp.size() - 1);


                    if (event.timestamp/1000000L - stepTimeStamp.get(stepTimeStamp.size() - 1)/1000000L
                            < 1500){

                         accelerationTotal =
                                Math.sqrt(Math.pow(event.values[0], 2) +
                                        Math.pow(event.values[1], 2) +
                                        Math.pow(event.values[2], 2));

                        if (accelerationTotalMin == 0){
                            accelerationTotalMin = accelerationTotal;
                        }
                        else if(accelerationTotal < accelerationTotalMin) {
                            accelerationTotalMin = accelerationTotal;

                        }
                        if (accelerationTotalMax == 0){
                            accelerationTotalMax = accelerationTotal;
                        }
                        else if (accelerationTotal > accelerationTotalMax){
                            accelerationTotalMax = accelerationTotal;
                        }
                    }
                    else{
                        accelerationTotalMax = 0;
                        accelerationTotalMin = 0;
                    }

                    //TextView accelerationX = (TextView) findViewById(R.id.accelerometerXTextView);
                    //accelerationX.setText(String.format("%.2f", accelerationTotal));

                    //TextView accelerationY = (TextView) findViewById(R.id.accelerometerYTextView);
                    //accelerationY.setText(String.format("%.2f", event.values[1]));

                }

                break;

//            case TYPE_LINEAR_ACCELERATION:
//
//                if (1 > 5) {
//
//
//                    //TextView accelerationX = (TextView) findViewById(R.id.accelerometerXTextView);
//                    //accelerationX.setText(String.format("%.2f", event.values[0]));
//
//                    //TextView accelerationY = (TextView) findViewById(R.id.accelerometerYTextView);
//                    //accelerationY.setText(String.format("%.2f", event.values[1]));
//
//                    //TextView accelerationZ = (TextView) findViewById(R.id.accelerometerZTextView);
//                    //accelerationZ.setText(String.format("%.2f", event.values[2]));
//
//                    if (stepCountingActive && numberOfStepsDetected > 1) {
//
//                        long timeElapsedFromLastStep
//                                = event.timestamp - stepTimeStamp.get(stepTimeStamp.size() - 1);
//
//
//                        if (event.timestamp / 1000000L - stepTimeStamp.get(stepTimeStamp.size() - 1) / 1000000L
//                                < 2000) {
//
//                            accelerationTotal =
//                                    Math.sqrt(Math.pow(event.values[0], 2) +
//                                            Math.pow(event.values[1], 2) +
//                                            Math.pow(event.values[2], 2));
//
//                            if (accelerationTotalMin == 0) {
//                                accelerationTotalMin = accelerationTotal;
//                            } else if (accelerationTotal < accelerationTotalMin) {
//                                accelerationTotalMin = accelerationTotal;
//
//                            }
//                            if (accelerationTotalMax == 0) {
//                                accelerationTotalMax = accelerationTotal;
//                            } else if (accelerationTotal > accelerationTotalMax) {
//                                accelerationTotalMax = accelerationTotal;
//                            }
//                        } else {
//                            accelerationTotalMax = 0;
//                            accelerationTotalMin = 0;
//                        }
//
//                        //TextView accelerationX = (TextView) findViewById(R.id.accelerometerXTextView);
//                        //accelerationX.setText(String.format("%.2f", accelerationTotal));
//
//                        //TextView accelerationY = (TextView) findViewById(R.id.accelerometerYTextView);
//                        //accelerationY.setText(String.format("%.2f", event.values[1]));
//
//                    }
//
//                }
//
//                break;

            case TYPE_MAGNETIC_FIELD:

                lastMagnetometerSet = true;

                System.arraycopy(event.values, 0, lastMagnetometer, 0, event.values.length);

                lastMagnetometer = lowPass(event.values.clone(), lastMagnetometer);

                if (lastAccelerometerSet && lastMagnetometerSet)
                {
                    mSensorManager.getRotationMatrix(mRotationMatrix, null, lastAccelerometer, lastMagnetometer);
                    mSensorManager.getOrientation(mRotationMatrix, mOrientationAngles);

                    float azimuthInRadians = mOrientationAngles[0];
                    float pitchInRadians = mOrientationAngles[1];
                    float rollInRadians = mOrientationAngles[2];

                    int azimuthInDegress = ((int)(azimuthInRadians * 180/(float)Math.PI) + 360) % 360;

                    TextView orientationView = (TextView) findViewById(R.id.orientationTextView);
                    orientationView.setText("" + azimuthInDegress);

                   // orientationView.setText(Float.toString(azimuthInDegress));

                }

                break;

            case TYPE_ROTATION_VECTOR:

                SensorManager.getRotationMatrixFromVector(mRotationMatrixFromVector, event.values);
                SensorManager.getOrientation(mRotationMatrixFromVector, mOrientationAngles);


                orientationInDegrees = ((int)(mOrientationAngles[0] * 180/(float)Math.PI) + 360) % 360;


                if (detectedStepsSensorValue > 0){
                    sumCosAngles += Math.cos(mOrientationAngles[0]);
                    sumSinAngles += Math.sin(mOrientationAngles[0]);
                    meanOrientationAngles += mOrientationAngles[0];
                    counter ++;
                }

                //int azimuthInDegress = (int)Math.toDegrees(mOrientationAngles[0]);

                TextView orientationView = (TextView) findViewById(R.id.rotationVectorTextView);
                orientationView.setText("" + orientationInDegrees);

                TextView rotVectAccuracy = (TextView) findViewById(R.id.rotationVectorAccView);
                rotVectAccuracy.setText("" + event.values[4]);
                //rotVectAccuracy.setText("" + counter);

                break;

        }
    }


    protected float[] lowPass( float[] input, float[] output ) {
        if ( output == null ) return input;

        for ( int i=0; i<input.length; i++ ) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    protected void onStop() {
        super.onStop();

        mSensorManager.unregisterListener(this);

    }

    @Override
    protected void onResume() {
        super.onResume();


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
        if (isSensorRotationVectorPresent) {
            mSensorManager.registerListener(this, mRotationVector,
                    SensorManager.SENSOR_DELAY_UI);
        }

                if (isSensorStepCounterPresent) {
            mSensorManager.registerListener(this, mStepCounter,
                    SensorManager.SENSOR_DELAY_FASTEST);
        }

//        if (isSensorLinearAccelerationPresent) {
//            mSensorManager.registerListener(this, mLinearAccelerometer,
//                    SensorManager.SENSOR_DELAY_UI);
//        }
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

    public void startStop (View view){

        Button myButton = (Button) findViewById(R.id.startStopButton);


        if (stepCountingActive){

            stepCountingActive = false;
            timeCountingStarted = 0;
            myButton.setText("START");
        }
        else {
            stepCountingActive = true;
            timeCountingStarted = SystemClock.elapsedRealtime();
            myButton.setText("STOP");

        }

    }

    public void reset (View view){
        stepCountingActive = false;
        timeCountingStarted = 0;
        initialStepCounterValue = initialStepCounterValue + numberOfStepsCounted;

        numberOfStepsCounted = 0;
        numberOfStepsDetected = 0;
        detectedStepsSensorValue = 0;
        countedStepsSensorValue = 0;

        stepMeanFrequency = 0;
        stepMeanTime = 0;

        sumAccData = 0;

        distance = 0;
        distanceHeight = 0;
        distanceFrequency = 0;
        totalTime =0;


        TextView countedSteps = (TextView) findViewById(R.id.countedStepsTextView);
        countedSteps.setText(String.valueOf(numberOfStepsCounted));

        TextView detectedSteps = (TextView) findViewById(R.id.stepsDetectedTextView);
        detectedSteps.setText("" + numberOfStepsDetected);

        TextView distanceView = (TextView) findViewById(R.id.distanceTextView);
        distanceView.setText(Double.toString(distance));

        Button myButton = (Button) findViewById(R.id.startStopButton);
        myButton.setText("START");

    }


}
