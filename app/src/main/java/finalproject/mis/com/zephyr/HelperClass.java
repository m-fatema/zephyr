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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import static java.lang.Boolean.FALSE;

public class HelperClass extends MainActivity {

    public static void showToastMessage(String message, Context context){
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0 ,0);
        toast.show();
    }

    //File store data for over night Tracking
    public String getFilePath() {
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            // We can read and write the media
            return "";
        }

        File folder = new File(Environment.getExternalStorageDirectory()
                                + "/Zephry");
        if (!folder.exists()){
            folder.mkdirs();
        }

        final String filename = folder.getAbsolutePath() + "/" + "OverNightData.csv";
        return filename;
    }

    public void clearCSVFile( String filename, String data){
        try{
            FileWriter fw = new FileWriter(filename,false);
            System.out.println("Data to write:" + data);
            fw.append(data);
            fw.flush();
            fw.close();
        }
        catch (IOException e){
            System.out.println("Exception in Clearing Existing file" + e.getMessage());
        }
    }
    public void writeToCSVFile( String filename, String data){
        try{
            FileWriter fw = new FileWriter(filename,true);
            fw.append(data);
            fw.flush();
            fw.close();
        }
        catch (IOException e){
            System.out.println("Exception in Writing file" + e.getMessage());
        }
    }
    public void readCSVFile( String filename){
        BufferedReader br = null;
        try {
            String sCurrentLine;
            br = new BufferedReader(new FileReader(filename));
            while ((sCurrentLine = br.readLine()) != null) {
                System.out.println(sCurrentLine);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null)br.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
