package finalproject.mis.com.zephyr;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.widget.Button;

import com.kircherelectronics.fsensor.filter.averaging.LowPassFilter;
import com.kircherelectronics.fsensor.filter.averaging.MeanFilter;

//https://code.tutsplus.com/tutorials/android-barometer-logger-acquiring-sensor-data--mobile-10558
public class MyService extends Service implements SensorEventListener {

    Boolean trackOvernight;
    int overNightDataTrack;
    long lastUpdateOverNight = 0;
    HelperClass helper;
    String oveNightDataFilePath;
    private SensorManager mSensorManager;
    private MeanFilter meanFilter;
    private LowPassFilter lpfAccelerationSmoothing;
    NotificationManagerCompat notificationManager;
    Button overNightTracking;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate (){

        //overNightTracking = (Button) MainActivity.findViewById(R.id.stopBtn);
        trackOvernight = true;
        overNightDataTrack = 0;
        oveNightDataFilePath="";
        getSensorData();
        helper = new HelperClass();
        lpfAccelerationSmoothing = new LowPassFilter();
        lpfAccelerationSmoothing.setTimeConstant(0.2f);
        meanFilter = new MeanFilter();
        meanFilter.setTimeConstant(0.8f);
        oveNightDataFilePath = helper.getFilePath();
        helper.clearCSVFile(oveNightDataFilePath,"x,y,z\n");
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, "123")
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentTitle("Zephry")
                .setContentText("Collecting overnight data")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        notificationManager = NotificationManagerCompat.from(this);

// notificationId is a unique int for each notification that you must define
        notificationManager.notify(1234, mBuilder.build());
    }

    /*
    @Purpose: To get data from Motion Sensor -
     */
    public void getSensorData(){
        boolean isSucess = initialiseSensors();
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
            Sensor mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_UI);
            return true;
        }
        return false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //we have some options for service
        //start sticky means service will be explicity started and stopped
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        //stopping the player when service is destroyed
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long currTime = System.nanoTime();
        if( trackOvernight && currTime - lastUpdateOverNight > 1000000000 ){
            System.out.println("Inside");
            lastUpdateOverNight = currTime;
            collectOvernightData(event);
        }
    }

    public void collectOvernightData(SensorEvent event){
        float x,y,z;
        float[] acceleration;
        float[] input = { event.values[0], event.values[1], event.values[2]};
        acceleration = lpfAccelerationSmoothing.filter(input);
        acceleration = meanFilter.filter(acceleration);
        x = acceleration[0];
        y = acceleration[1];
        z = acceleration[2];

        if( !Float.isNaN(x) && !Float.isNaN(y) && !Float.isNaN(z) ) {

            if (!oveNightDataFilePath.equals("") && overNightDataTrack < 10800) {//10800

//                String sensorData = String.valueOf(x) + ","
//                        + String.valueOf(y) + ","
//                        + String.valueOf(z) + "\n";
                String sensorData = String.format ("%.2f", x) + ","
                        + String.format ("%.2f", y) + ","
                        + String.format ("%.2f", z) + "\n";
                System.out.println("####Count value - overNightDataTrack:" + overNightDataTrack);
                overNightDataTrack++;
                //HelperClass.showShortToastMessage("Processing!!!", this);
                helper.writeToCSVFile(oveNightDataFilePath, sensorData);


            } else if ( !oveNightDataFilePath.equals("") && overNightDataTrack == 10800) { //10800
                trackOvernight = false;
                helper.readCSVFile(oveNightDataFilePath);
                //stopping service
                mSensorManager.unregisterListener(MyService.this);
                stopService(new Intent(this, MyService.class));
                //findViewById(R.id.overNightTrackBtn).setEnabled(false);
                //stopSelf();
                HelperClass.showToastMessage("Processed.................", this);
                notificationManager.cancel(1234);

            } else if (oveNightDataFilePath.equals("")) {
                System.out.println("####No file path found:");
                HelperClass.showToastMessage("There was a problem in accessing the CSV file." +
                        " Please delete the folde Zephry and try again. Thank You", this);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}