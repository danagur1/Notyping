package com.example.finalproject2;

import android.content.Intent;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class NewNote extends AppCompatActivity implements View.OnClickListener,
        View.OnFocusChangeListener {
    EditText et_title, et_note;
    Map<String, Button> text_edit_btns;
    BlockingQueue<JSONObject> handle_response_q;
    Integer id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_note);
        et_title = findViewById(R.id.et_title);
        et_note = findViewById(R.id.et_note);
        et_note.setOnFocusChangeListener(this);
        text_edit_btns = ResetObjects.reset_buttons(this, new String[]{"btn_save",
                "btn_bold_text", "btn_dec_text", "btn_inc_text", "btn_underline_text"});
        if (getIntent().hasExtra("ID")) {
            id = getIntent().getIntExtra("ID", 0);
            set_note();
        }

    }

    private void create_request(){
        handle_response_q = new LinkedBlockingQueue<>();
        JSONClient.Request request = JSONClient.Request.notes;
        FunctionParam group_param = new FunctionParam("group", "s_note");
        FunctionParam id_param = new FunctionParam("id", id);
        FunctionParam owner_param = new FunctionParam("owner", Login.get_username());
        JSONClient.server_comm(this, handle_response_q,request, group_param, id_param,
                owner_param);
    }

    private void set_note(){
        create_request();
        try {
            JSONObject response_json = handle_response_q.poll(BlockingQueueTimeoutHandler.timeout,
                    TimeUnit.MILLISECONDS);
            if (response_json == null){
                BlockingQueueTimeoutHandler.redirectToErrorActivity(this);
            }
            JSONObject notes_response = (JSONObject) response_json.get("return_code");
            et_title.setText(notes_response.get("title").toString());
            if (notes_response.getString("text").equals("")){
                Toasts.toast(this, "Couldn't find text");
            }
            et_note.setText(notes_response.getString("text"));
            if (notes_response.getBoolean("paintFlags")){
                et_note.setPaintFlags(et_note.getPaintFlags() | Paint.FAKE_BOLD_TEXT_FLAG);
            }
            et_note.setTextSize(notes_response.getInt("textSize"));
            if (notes_response.getBoolean("paintFlags")) {
                //et_note.setTypeface(Typeface.DEFAULT_BOLD);
                et_note.setTypeface(Typeface.DEFAULT_BOLD);
            }
        } catch (Exception e) {
            Toasts.error_toast(this);
        }
    }

    /**Set bold or remove bold considering the condition of the text and the mark area start and
     * end. If the marked area is already bold, removes it. If the marked area isn't bold, adds
     * bold to it.
     *
     * @param start the start of the mark area
     * @param end the end of the mark area
     */
    private void set_bold(int start, int end){
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(et_note.getText());
        StyleSpan[] styleSpans = spannableStringBuilder.getSpans(start, end, StyleSpan.class);
        boolean hasBold = false;
        for (StyleSpan span : styleSpans) {
            if (span.getStyle() == Typeface.BOLD) {
                // The selected area of text is already bold
                hasBold = true;
                spannableStringBuilder.removeSpan(span);
            }
        }
        if (!hasBold) {
            spannableStringBuilder.setSpan(new StyleSpan(Typeface.BOLD), start, end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        et_note.setText(spannableStringBuilder);
        et_note.setSelection(end);
    }

    private int[] get_mark_area(){
        int start = et_note.getSelectionStart();
        int end = et_note.getSelectionEnd();
        // In case of not marked area, set bold for all of it
        if (start == end) {
            start = 0;
            end = et_note.getText().length();
        }
        return new int[]{start, end};
    }

    /**Set bold or remove underline considering the condition of the text and the mark area start
     * and end. If the marked area is already underline, removes it. If the marked area isn't
     * underline, adds underline to it.
     *
     * @param start the start of the mark area
     * @param end the end of the mark area
     */
    private void set_underline(int start, int end){
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(
                et_note.getText());
        UnderlineSpan[] underlineSpans = spannableStringBuilder.getSpans(start, end,
                UnderlineSpan.class);
        boolean hasUnderline = false;

        for (UnderlineSpan span : underlineSpans) {
            hasUnderline = true;
            spannableStringBuilder.removeSpan(span);
        }

        if (!hasUnderline) {
            spannableStringBuilder.setSpan(new UnderlineSpan(), start, end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        et_note.setText(spannableStringBuilder);
        et_note.setSelection(end);
    }

    @Override
    public void onClick(View view) {
        int[] mark_area = get_mark_area();
        int start = mark_area[0];
        int end = mark_area[1];
        if (Objects.equals(text_edit_btns.get("btn_bold_text"), view)) {
            set_bold(start, end);
        }
        else if (Objects.equals(text_edit_btns.get("btn_underline_text"), view)) {
            set_underline(start, end);
        }
        else if (Objects.equals(text_edit_btns.get("btn_dec_text"), view)){
            et_note.setTextSize(TypedValue.COMPLEX_UNIT_PX, et_note.getTextSize()-10);
        }
        else if (Objects.equals(text_edit_btns.get("btn_inc_text"), view)){
            et_note.setTextSize(TypedValue.COMPLEX_UNIT_PX, et_note.getTextSize()+10);
        }
        else if (Objects.equals(text_edit_btns.get("btn_save"), view)){
            save(view);
        }
    }

    /**Handles the save button's click
     *
     * @param view the button's view, used for the intent for returning to all notes screen
     */
    private void save(View view){
        save_to_server();
        Intent intent=new Intent(view.getContext(), AllNotes.class);
        view.getContext().startActivity(intent);
    }

    /**Creates a BlockingQueue and checks the server's response. creates an error toast if needed
     *
     */
    private void save_to_server(){
        BlockingQueue<JSONObject> handle_response_q = new LinkedBlockingQueue<>();
        create_request(handle_response_q);
        try {
            JSONObject response_json = handle_response_q.poll(BlockingQueueTimeoutHandler.timeout,
                    TimeUnit.MILLISECONDS);
            if (response_json == null){
                BlockingQueueTimeoutHandler.redirectToErrorActivity(this);
            }
            if ((Integer)response_json.get("return_code") != 1){
                Toasts.error_toast(this);
            }
        } catch (Exception e) {
            Toasts.error_toast(this);
        }
    }

    /** Defines all the parameters and the request type for the new note creating for the server
     *
     * @param handle_response_q the queue for handling the server response, a return value for
     *                          errors notifications
     */
    private void create_request(BlockingQueue<JSONObject> handle_response_q){
        JSONClient.Request request = JSONClient.Request.new_note;
        FunctionParam type = new FunctionParam("note_type", "TYPE");
        FunctionParam p_title = new FunctionParam("title", et_title.getText().toString());
        FunctionParam p_text = new FunctionParam("text", et_note.getText().toString());
        FunctionParam p_paintFlags = new FunctionParam("paintFlags",
                et_note.getTypeface() != null &&
                        et_note.getTypeface().getStyle() == Typeface.BOLD);
        FunctionParam p_textSize = new FunctionParam("textSize",
                et_note.getTextSize()*1/3);
        FunctionParam p_typeface = new FunctionParam("typeface",
                et_note.getTypeface().toString());
        FunctionParam p_owner = new FunctionParam("owner", Login.get_username());
        FunctionParam p_lock = new FunctionParam("lock", false);
        FunctionParam p_star = new FunctionParam("star", false);
        if (id!=null){
            FunctionParam p_id = new FunctionParam("id", id);
            JSONClient.server_comm(this, handle_response_q, request, type, p_title, p_text,
                    p_paintFlags, p_textSize, p_typeface, p_id, p_owner, p_lock);
        }
        else{
            JSONClient.server_comm(this, handle_response_q, request, type, p_title, p_text,
                    p_paintFlags, p_textSize, p_typeface, p_owner, p_lock);
        }

    }


    @Override
    public void onFocusChange(View view, boolean b) {

    }
}
