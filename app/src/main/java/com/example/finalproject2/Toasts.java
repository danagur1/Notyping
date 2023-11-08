package com.example.finalproject2;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

public class Toasts {
    protected static void toast(Activity activity, String text){
        try{
            Toast toast = Toast.makeText(activity, text, Toast.LENGTH_LONG);
            toast.show();
        } catch (Exception e){
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    Intent intent = new Intent(null, Error.class);
                    activity.startActivity(intent);
                }
            });
        }
    }

    protected static void error_toast(Activity activity){
        Toasts.toast(activity, "An error occurred");
    }
}
