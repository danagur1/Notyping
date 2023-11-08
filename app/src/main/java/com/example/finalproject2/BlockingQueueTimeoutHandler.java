package com.example.finalproject2;

import android.app.Activity;
import android.content.Intent;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;

public class BlockingQueueTimeoutHandler {
    protected static long timeout = 10000;

    protected static void redirectToErrorActivity(Activity activity) {
        Intent intent = new Intent(activity, Error.class);
        activity.startActivity(intent);
    }
}







