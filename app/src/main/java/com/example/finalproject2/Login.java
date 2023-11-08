package com.example.finalproject2;

import android.app.Activity;
import android.content.Intent;
import android.widget.EditText;

import org.json.JSONObject;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/** Implementing all the log in function related with the function login_clicked used from MainActivity
 *
 */
public class Login {

    //The activity that the login happens in
    Activity activity;
    private static String username;

    protected Login(Activity activity){
        this.activity = activity;
    }

    /**creates a BlockingQueue for handling response. When response is received, call the relevant
     * function
     *
     * @param et_username the EditText for the username field
     * @param et_password the  EditText for the password field
     */
    protected void login_clicked(EditText et_username, EditText et_password) {
        BlockingQueue<JSONObject> handle_response_q = new LinkedBlockingQueue<>();
        String username = et_username.getText().toString();
        String password = et_password.getText().toString();
        if (username.equals("") || password.equals("")){
            Toasts.toast(activity, "Username of Password is empty");
            return;
        }
        this.create_request(handle_response_q, username, password);
        try {
            JSONObject response_json = handle_response_q.poll(BlockingQueueTimeoutHandler.timeout,
                    TimeUnit.MILLISECONDS);
            if (response_json == null){
                BlockingQueueTimeoutHandler.redirectToErrorActivity(activity);
            }
            if ((Integer)(response_json.get("return_code"))==1) {
                this.username = username;
                log_in();
            }
            else {
                incorrect_pass();
            }
        } catch (Exception e) {
            Toasts.error_toast(activity);
        }
    }

    /**Defines the login type of request and the parameters for the request and send it to the
     * server
     *
     * @param handle_response_q the queue for receiving responses and handling it
     * @param username the username entered by user
     * @param password the password entered by user
     */
    private void create_request(BlockingQueue<JSONObject> handle_response_q, String username,
                                String password){
        //Defines the login type of request:
        JSONClient.Request request = JSONClient.Request.login;
        //The parameters for the request:
        FunctionParam username_param = new FunctionParam("username", username);
        FunctionParam password_param = new FunctionParam("password",password);
        JSONClient.server_comm(activity, handle_response_q, request, username_param,
                password_param);
    }

    /**When logging in start the intent of all notes screen
     *
     */
    private void log_in(){
        Intent intent=new Intent(activity, AllNotes.class);
        activity.startActivity(intent);
    }

    /**When username and password not matching, show relevant toast
     *
     */
    private void incorrect_pass(){
        Toasts.toast(activity, "Wrong username or password");
    }

    protected static String get_username(){
        return username;
    }
}
