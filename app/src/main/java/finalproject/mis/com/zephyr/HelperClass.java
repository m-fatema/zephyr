package finalproject.mis.com.zephyr;

import android.content.Context;
import android.view.Gravity;
import android.widget.Toast;

public class HelperClass extends MainActivity {

    public static void showToastMessage(String message, Context context){
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0 ,0);
        toast.show();
    }
}
