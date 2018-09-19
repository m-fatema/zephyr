package finalproject.mis.com.zephyr;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static java.lang.Boolean.FALSE;

public class HelperClass extends MainActivity {

    public static void showToastMessage(String message, Context context){
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0 ,0);
        toast.show();
    }
    public void OverNightTracking(){
        checkForCSVFile();
    }

    public Boolean checkForCSVFile() {
        //Boolean mExternalStorageAvailable = false;
        String state = Environment.getExternalStorageState();

        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            // We can read and write the media
            return false;
        }
        File folder = new File(Environment.getExternalStorageDirectory()
                                + "/Zephry");
        boolean var = false;
        if (!folder.exists()){
            var = folder.mkdirs();
        }
        System.out.println("Is folder Present??? " + folder.exists());
        System.out.println("Folder Path + Name :" + folder.getAbsolutePath());
        final String filename = folder.getAbsolutePath() + "/" + "OverNightData.csv";

        try{
            FileWriter fw = new FileWriter(filename);
        }
        catch (IOException e){
            System.out.println("Exception in Writing file" + e.getMessage());
        }
        return true;
    }
}
