package com.example.finalproject2;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupWindow;

import org.json.JSONObject;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private Map<String, Button> btns_by_id;
    private EditText et_password, et_username;
    protected static Activity activity;
    private static boolean first_time = true; //Do setup process only on first time running
    private PopupWindow popupWindow;
    private EditText forgot_username, forgot_mail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        reset_buttons();
        reset_input_fields();
        setup_search_window();
        if (first_time){
            JSONClient.server_setup(this);
            first_time = false;
        }
        activity = this;
    }

    private void setup_search_window() {
        popupWindow = new PopupWindow(this);
        View popupView = getLayoutInflater().inflate(R.layout.forgotpass_window, null);
        popupWindow.setContentView(popupView);
        forgot_mail = popupView.findViewById(R.id.et_mail_forgot);
        Button submitButton = popupView.findViewById(R.id.btnForgotPassword);

        // Add this code to open the keyboard when the EditText is focused
        forgot_mail.requestFocus();
        popupWindow.setFocusable(true);

        // Add this code to listen for the keyboard opening
        forgot_mail.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                forgot_mail.getWindowVisibleDisplayFrame(r);
                int screenHeight = forgot_mail.getRootView().getHeight();
                int keypadHeight = screenHeight - r.bottom;
                if (keypadHeight > screenHeight * 0.15) {
                    // Keyboard is open, do any additional handling if needed
                } else {
                    // Keyboard is closed, do any additional handling if needed
                }
            }
        });

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String mail = forgot_mail.getText().toString().trim();
                popupWindow.dismiss();
                forgot_password(mail);
            }
        });
    }

    private void forgot_password(String mail){
        BlockingQueue<JSONObject> handle_response_q = new LinkedBlockingQueue<>();
        JSONClient.Request request = JSONClient.Request.forgot_pass;
        FunctionParam group_param = new FunctionParam("group", "all");
        FunctionParam owner_param = new FunctionParam("owner", Login.get_username());
        FunctionParam mail_param = new FunctionParam("mail", mail);
        JSONClient.server_comm(this, handle_response_q,request, group_param, owner_param,
                mail_param);
    }

    /**set the instances of the EditText objects for the input of the password and the username
     *
     */
    public void reset_input_fields(){
        et_password = findViewById(R.id.et_password);
        et_username = findViewById(R.id.et_username);
    }

    /**reset the buttons relevant for the activity:
     *btnLogin, btnForgotPassword, btnNewAccount
     */
    public void reset_buttons(){
        btns_by_id = ResetObjects.reset_buttons(this, new String[]{"btnLogin",
                "btnNewAccount", "btnForgotPassword"});
    }

    /**The function that defines what happens when pressing one of the activity's buttons
     *
     * @param button the button that clicked
     */
    @Override
    public void onClick(View button) {
        if (button == btns_by_id.get("btnLogin")){
            try {
                Login login = new Login(this);
                login.login_clicked(et_username, et_password);
            } catch (Exception exception){
                Toasts.error_toast(this);
            }
        }
        else if (button == btns_by_id.get("btnForgotPassword")){
            popupWindow.showAtLocation(button, Gravity.CENTER, 0, 0);
        }
        else if (button == btns_by_id.get("btnNewAccount")){
            new_acc_clicked();
        }
    }

    /**When the new account button is clicked, starts the register activity
     */
    private void new_acc_clicked(){
        Intent intent=new Intent(this, Register.class);
        startActivity(intent);
    }

}