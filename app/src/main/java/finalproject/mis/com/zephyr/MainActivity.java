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
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.kircherelectronics.fsensor.filter.averaging.LowPassFilter;
import com.kircherelectronics.fsensor.filter.averaging.MeanFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import static android.hardware.SensorManager.SENSOR_DELAY_NORMAL;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    //example variables
    private double[] freqCounts;

    private SensorManager mSensorManager;
    private Sensor mSensor;
    private List<Entry> xValueList;
    private List<Entry> yValueList;
    private List<Entry> zValueList;
    private List<Entry> magnitudeList;
    private int sampleRate, wSize, axisEntryIndex, magnitudeIndex;
    private double[] magnitudeArray;
    private Boolean isTrackingOn;
    Button btnStart, btnStop;
    Timer stopTimer;
    Handler stopHandler;

    private float[] linearAcceleration = new float[]{ 0, 0, 0 };

    //------------------------------------KalebKE/FSensor-----------------------------------------------
    private MeanFilter meanFilter;
    private LowPassFilter lpfAccelerationSmoothing;
    //------------------------------------KalebKE/FSensor-----------------------------------------------

//    private int count = 0;
//    static final float timeConstant = 0.18f;
//    private float alpha;
//    private float timestamp = System.nanoTime();
//    private float timestampOld = System.nanoTime();
//    private float[] gravity = new float[]{ 0, 0, 0 };


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
        stopHandler.postDelayed(new Runnable(){

            @Override
            public void run() {
                // This method will be executed once the timer is over
                Log.d("In stopHandler Run" , "****************");
                reInitialiseChart();
            }
        },60000);// set time as per your requirement
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
        sampleRate = 5000000; //in microseconds = 1 seconds
        wSize = 128;
        lpfAccelerationSmoothing = new LowPassFilter();
        lpfAccelerationSmoothing.setTimeConstant(0.2f);
        meanFilter = new MeanFilter();
        meanFilter.setTimeConstant(0.8f);
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
            mSensorManager.registerListener(this, mSensor, SENSOR_DELAY_NORMAL);//sampleRate);
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

    /*
        @Purpose: To initially remove gravity and remove noise to make a smooth signal
     */
    public float[] filterData(float[] input){

        float[] acceleration = new float[3];
        //System.arraycopy(event.values, 0, acceleration, 0, event.values.length);
        Log.d("Input:" , String.valueOf(input));
        //acceleration = input;
        acceleration = lpfAccelerationSmoothing.filter(input);
        acceleration = meanFilter.filter(acceleration);
//        Log.d("meanFilter:" , String.valueOf(acceleration));
//        Log.d("Filter 0:" , String.valueOf(acceleration[0]));
//        Log.d("Filter 1::" , String.valueOf(acceleration[1]));
//        Log.d("Filter 2:::" , String.valueOf(acceleration[2]));
        return acceleration;
    }


    /*
        @Purpose: To plot data from accelerometer in grap-view
     */
    public void updateChartData( SensorEvent event ){

        float x,y,z,magnitude;

        float[] input = { event.values[0], event.values[1], event.values[2]};
        float output[] = filterData(input);
        x = output[0];
        y = output[1];
        z = output[2];

        magnitude = (float) Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));

        magnitudeArray[magnitudeIndex] =  magnitude;
        magnitudeIndex++;
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
        lineData.addDataSet(addLineData( magnitudeList, "Magnitude" , Color.WHITE));
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
        Double maxFreqCount = 0.0;
        for (int i = 0; i < freqCounts.length; i++){
            magnitudeEntryList.add( new Entry(i, (float) freqCounts[i]));
            if( maxFreqCount < freqCounts[i]){
                maxFreqCount = freqCounts[i];
            }
        }
        LineData lineData = new LineData();
        lineData.addDataSet(addLineData( magnitudeEntryList, "X" , Color.BLACK));
        fftGraph.setData(lineData);
        fftGraph.notifyDataSetChanged();
        fftGraph.invalidate();
        fftGraph.setDescription(new Description());
        reInitialiseChart();
        Log.d("Max Freq is in Hz: " , String.valueOf(maxFreqCount));

        //Breath rate estimator
//        Integer bpm;
//        Integer maxFreqCount1 = Math.ceil((maxFreqCount));
//        bpm = (Integer) maxFreqCount1 * 60;
//        Log.d("BPM " , String.valueOf(bpm));
        TextView bpmTxt = (TextView) findViewById(R.id.breathRateTxt);
        bpmTxt.setText(String.valueOf(maxFreqCount));
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

//    public float[] filterData(float[] input)
//    {
//        timestamp = System.nanoTime();
//        float dt;
//        // Find the sample period (between updates).
//        // Convert from nanoseconds to seconds
//        dt = 1 / (count / ((timestamp - timestampOld) / 1000000000.0f));
//
//        count++;
//
//        //alpha = (float)0.5;
//
//        alpha = timeConstant / (timeConstant + dt);
//        Log.d("alpha" , String.valueOf(alpha));
//
//        // Calculate alpha
//        //alpha = 0.1f;
//
//        gravity[0] = alpha * gravity[0] + (1 - alpha) * input[0];
//        gravity[1] = alpha * gravity[1] + (1 - alpha) * input[1];
//        gravity[2] = alpha * gravity[2] + (1 - alpha) * input[2];
//        Log.d("Gravity 1" , String.valueOf(gravity[0]));
//        Log.d("Gravity 2" , String.valueOf(gravity[1]));
//        Log.d("Gravity 3" , String.valueOf(gravity[2]));
//
//        linearAcceleration[0] = input[0] - gravity[0];
//        linearAcceleration[1] = input[1] - gravity[1];
//        linearAcceleration[2] = input[2] - gravity[2];
//
////        linearAcceleration[0] = input[0];
////        linearAcceleration[1] = input[1];
////        linearAcceleration[2] = input[2];
//        return linearAcceleration;
//    }
}