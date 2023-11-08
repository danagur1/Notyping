package com.example.finalproject2;

import android.content.Intent;
import android.os.Bundle;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Register extends AppCompatActivity implements View.OnClickListener {

    Button submit;
    Button returnMain;
    Map<String, EditText> register_fields;
    Map<String, Button> buttonMap;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register);
        ResetObjects<EditText> resetETs= new ResetObjects<>();
        register_fields = resetETs.reset_views(this, new String[]{"et_username",
                "et_password_reg", "et_mail", "et_phone"});
        buttonMap = ResetObjects.reset_buttons(this, new String[]{"btnRegister",
                "btnReturnMain"});
    }

    /**creates an intent and return to the log in screen
     *
     */
    private void return_to_login(){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    @Override
    public void onClick(View view) {
        if (view == buttonMap.get("btnReturnMain")) {
            return_to_login();
        }
        if (view == buttonMap.get("btnRegister")) {
            register();
        }
    }

    /**creates and defines the blocking queue for the server responses.
     *Print the anyway and return to login in case of registered successfully
     */
    private void register(){
        BlockingQueue<JSONObject> handle_response_q = new LinkedBlockingQueue<>();
        create_request(handle_response_q);
        String respone = null;
        try {
            JSONObject response_json = handle_response_q.poll(BlockingQueueTimeoutHandler.timeout,
                    TimeUnit.MILLISECONDS);
            if (response_json == null){
                BlockingQueueTimeoutHandler.redirectToErrorActivity(this);
            }
            respone = (String) response_json.get("return_code");
            //BlockingQueueTimeoutHandler.stopTimer();
        } catch (Exception e) {
            Toasts.error_toast(this);
        }
        Toasts.toast(this, respone);
        if (respone.equals("Registered Successfully")) {
            return_to_login();
        }
    }

    /**Defines the login type of request and the parameters for the request and send it to the
     * server
     *
     * @param handle_response_q the queue for receiving responses and handling it
     */
    private void create_request(BlockingQueue<JSONObject> handle_response_q){
        //Defines the login type of request:
        JSONClient.Request request = JSONClient.Request.register;
        //The parameters for the request:
        FunctionParam username_param = new FunctionParam("username", register_fields.
                get("et_username").getText());
        FunctionParam password_param = new FunctionParam("password", register_fields.
                get("et_password_reg").getText());
        FunctionParam phone_param = new FunctionParam("phone", register_fields.get
                ("et_phone").getText());
        FunctionParam mail_param = new FunctionParam("mail", register_fields.get("et_mail").
                getText());
        JSONClient.server_comm(this, handle_response_q, request, username_param,
                password_param, phone_param, mail_param);
    }

}
