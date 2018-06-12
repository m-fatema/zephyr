package finalproject.mis.com.zephyr;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    //example variables
    private double[] freqCounts;

    private SensorManager mSensorManager;
    private Sensor mSensor;
    private List<Entry> xValueList;
    private List<Entry> yValueList;
    private List<Entry> zValueList;
    private List<Entry> magnitudeList;
    private static int chartListSize = 200;
    private int sampleRate, wSize, axisEntryIndex, magnitudeIndex;
    private double[] magnitudeArray;
    private Boolean isTrackingOn;
    Button btnStart, btnStop;
    Timer stopTimer;
    Handler stopHandler;
    static final float timeConstant = 0.18f;
    private float alpha = 0.1f;
    private float timestamp = System.nanoTime();
    private float timestampOld = System.nanoTime();
    private float[] gravity = new float[]{ 0, 0, 0 };
    private float[] linearAcceleration = new float[]{ 0, 0, 0 };
    private int count = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnStart = (Button)findViewById(R.id.startBtn);
        btnStop = (Button)findViewById(R.id.stopBtn);
        btnStop.setEnabled(false);
        isTrackingOn = false;
        inititalise();
    }

    public void startTracking( View view){
        axisEntryIndex = 0;
        magnitudeIndex = 0;
        xValueList = new ArrayList<>();
        yValueList = new ArrayList<>();
        zValueList = new ArrayList<>();
        magnitudeList = new ArrayList<>();
        magnitudeArray = new double[1000];

        btnStart.setEnabled(false);
        btnStop.setEnabled(true);


        new Handler().postDelayed(new Runnable(){
            @Override
            public void run() {
                isTrackingOn = true;
            }
        }, 1000);

//        stopTimer = new Timer();
//        stopTimer.schedule(new TimerTask() {
//            @Override
//            public void run() {
//                reInitialiseChart();
//            }
//        }, 15000);

        stopHandler = new Handler();
//        stopHandler.postDelayed(new Runnable(){
//
//            @Override
//            public void run() {
//                // This method will be executed once the timer is over
//                Log.d("In stopHandler Run" , "****************");
//                reInitialiseChart();
//            }
//        },60000);// set time as per your requirement
    }


    public void stopTracking( View view){
//        if( stopTimer != null){
//            stopTimer.cancel();
//        }
        stopHandler.removeCallbacks(null);
        reInitialiseChart();
    }

    public void reInitialiseChart(){
        isTrackingOn = false;
        btnStop.setEnabled(false);
        btnStart.setEnabled(true);
        LineChart graph = (LineChart) findViewById(R.id.dataGraph);
        LineData lineData = new LineData();
        graph.notifyDataSetChanged();
        graph.setData(lineData);
    }

    /*
        @Purpose: To initialise the app
                    Check for permissions, if not present ask for permission
     */
    public void inititalise(){
        sampleRate = 1000000; //in microseconds = 1 seconds
        wSize = 64;
        getSensorData();
    }

    /*
    @Purpose: To get data from Motion Sensor -
     */
    public void getSensorData(){
        boolean isSucess = initialiseSensors();
        Log.d("@@@Is Sucess: " , String.valueOf(isSucess));
        if( !isSucess ){
            HelperClass.showToastMessage("The device sensor could not be acessed" +
                    " or there is no accelerometer present on device " , this);
        }
    }

    /*
        @Purpose: Initialise Sensor
     */
    public boolean initialiseSensors(){
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager != null
                && mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null){
            mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mSensorManager.registerListener(this, mSensor, sampleRate);
            return true;
        }
        return false;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if( magnitudeIndex == wSize){
            FFTAsynctask fftAsynctask = new FFTAsynctask(wSize);
            fftAsynctask.execute(magnitudeArray);
        }
        if( isTrackingOn ){
            updateChartData( event );
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}



    public float[] LowPass(float[] input)
    {
        timestamp = System.nanoTime();
        float dt;
        // Find the sample period (between updates).
        // Convert from nanoseconds to seconds
        dt = 1 / (count / ((timestamp - timestampOld) / 1000000000.0f));

        count++;

        alpha = timeConstant / (timeConstant + dt);

        // Calculate alpha
        //alpha = dt/(timeConstant + dt);

        gravity[0] = alpha * gravity[0] + (1 - alpha) * input[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * input[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * input[2];

        linearAcceleration[0] = input[0] - gravity[0];
        linearAcceleration[1] = input[1] - gravity[1];
        linearAcceleration[2] = input[2] - gravity[2];

        return linearAcceleration;

        // Update the filter
        // y[i] = y[i] + alpha * (x[i] - y[i])
//        output[0] = output[0] + alpha * (input[0] - output[0]);
//        output[1] = output[1] + alpha * (input[1] - output[1]);
//        output[2] = output[2] + alpha * (input[2] - output[2]);
    }

    /*
        @Purpose: To plot data from accelerometer in grap-view
     */
    public void updateChartData( SensorEvent event ){

        float x,y,z,magnitude;

        float alpha = (float)0.5;
//        x = event.values[0] - (alpha * mSensorManager.GRAVITY_EARTH + (1 - alpha) * event.values[0]);
//        y = event.values[1] - (alpha * mSensorManager.GRAVITY_EARTH + (1 - alpha) * event.values[1]);
//        z = event.values[2] - (alpha * mSensorManager.GRAVITY_EARTH + (1 - alpha) * event.values[2]);
//        x = event.values[0];
//        y = event.values[1];
//        z = event.values[2];

//        x = x + alpha * ( event.values[0] - x );
//        y = y + alpha * ( event.values[1] - y );
//        z = z + alpha * ( event.values[2] - z );

        float[] input = { event.values[0], event.values[1], event.values[2]};
        float output[] = LowPass(input);
        Log.d("Output Array 1" , String.valueOf(output[0]));
        Log.d("Output Array 2" , String.valueOf(output[1]));
        Log.d("Output Array 3" , String.valueOf(output[2]));
        x = output[0];
        y = output[1];
        z = output[2];

        magnitude = (float) Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));

//        if(xValueList.size() >= chartListSize)
//            xValueList.remove(0);
//        if(yValueList.size() >= chartListSize)
//            yValueList.remove(0);
//        if(zValueList.size() >= chartListSize)
//            zValueList.remove(0);
//        if( magnitudeList.size() >= chartListSize)
//            magnitudeList.remove(0);
//
//        if( magnitudeIndex < magnitudeArray.length){
//            if(magnitudeArray.length <= wSize){
//                magnitudeArray[magnitudeIndex] =  magnitude;
//                magnitudeIndex++;
//            }
//        }
//        else{
//            magnitudeIndex = 0;
//            magnitudeArray = new double[wSize];
//        }

        magnitudeArray[magnitudeIndex] =  magnitude;
        magnitudeIndex++;
        Log.d("*******axisEntryIndex: " , String.valueOf(axisEntryIndex));
        axisEntryIndex += 1;
        xValueList.add( new Entry( axisEntryIndex, x));
        yValueList.add( new Entry( axisEntryIndex, y));
        zValueList.add( new Entry( axisEntryIndex, z));
        magnitudeList.add( new Entry( axisEntryIndex, magnitude));
        updateChartData();
    }

    /*
        @Purpose: To update the accelerometer chart with new data
     */
    public void updateChartData(){
        LineChart graph = (LineChart) findViewById(R.id.dataGraph);
        LineData lineData = new LineData();
        lineData.addDataSet(addLineData( xValueList, "X" , Color.RED));
        lineData.addDataSet(addLineData( yValueList, "Y" , Color.GREEN ));
        lineData.addDataSet(addLineData( zValueList, "Z" , Color.BLUE));
        //lineData.addDataSet(addLineData( magnitudeList, "Magnitude" , Color.WHITE));
        graph.setBackgroundColor(Color.DKGRAY);
        graph.setGridBackgroundColor(Color.BLACK);
        graph.setData(lineData);
        graph.notifyDataSetChanged();
        graph.invalidate();
    }
    /*
        @Purpose: To update the FFT chart with new data
     */
    public void updateFFTChart(){
        LineChart fftGraph = (LineChart) findViewById(R.id.fftGraph);
        List<Entry> magnitudeEntryList = new ArrayList<>();
        for (int i = 0; i < freqCounts.length; i++){
            magnitudeEntryList.add( new Entry(i, (float) freqCounts[i]));
        }
        LineData lineData = new LineData();
        lineData.addDataSet(addLineData( magnitudeEntryList, "X" , Color.BLACK));
        fftGraph.setData(lineData);
        fftGraph.notifyDataSetChanged();
        fftGraph.invalidate();
        fftGraph.setDescription(new Description());
    }

    /*
        @Purpose: To create a data set for each line to be shown on graph
        @Return: LineDataSet
    */
    public LineDataSet addLineData( List<Entry> valueList, String label, int lineColor  ){
        LineDataSet lineDataSet = new LineDataSet(valueList, label);
        lineDataSet.setColor(lineColor);
        lineDataSet.setValueTextColor(lineColor);
        lineDataSet.setValueTextSize(1);
        lineDataSet.setDrawCircles(false);
        return lineDataSet;
    }

    //When user moves out of the app, stop unregister the sensor
    @Override
    public void onPause() {
        super.onPause();  // Always call the superclass method first
        mSensorManager.unregisterListener(MainActivity.this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(MainActivity.this, mSensor, sampleRate);
    }


    /**
     * Implements the fft functionality as an async task
     * FFT(int n): constructor with fft length
     * fft(double[] x, double[] y)
     */

    private class FFTAsynctask extends AsyncTask<double[], Void, double[]> {

        private int wsize; //window size must be power of 2

        // constructor to set window size
        FFTAsynctask(int wsize) {
            this.wsize = wsize;
        }

        @Override
        protected double[] doInBackground(double[]... values) {


            double[] realPart = values[0].clone(); // actual acceleration values
            double[] imagPart = new double[wsize]; // init empty

            /**
             * Init the FFT class with given window size and run it with your input.
             * The fft() function overrides the realPart and imagPart arrays!
             */
            FFT fft = new FFT(wsize);
            fft.fft(realPart, imagPart);
            //init new double array for magnitude (e.g. frequency count)
            double[] magnitude = new double[wsize];


            //fill array with magnitude values of the distribution
            for (int i = 0; wsize > i ; i++) {
                magnitude[i] = Math.sqrt(Math.pow(realPart[i], 2) + Math.pow(imagPart[i], 2));
            }
            return magnitude;
        }

        @Override
        protected void onPostExecute(double[] values) {
            //hand over values to global variable after background task is finished
            freqCounts = values;
            updateFFTChart();
        }
    }

    /**
     * little helper function to fill example with random double values
     */
    public void randomFill(double[] array){
        Random rand = new Random();
        for(int i = 0; array.length > i; i++){
            array[i] = rand.nextDouble();
        }
    }
}