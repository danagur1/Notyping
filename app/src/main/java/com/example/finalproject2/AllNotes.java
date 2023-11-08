package com.example.finalproject2;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class AllNotes extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener {

    BlockingQueue<JSONObject> handle_response_q;
    Map<LinearLayout, Integer> notes_ids;
    int curr_menu_id;
    List<LinearLayout> marked_notes;
    PopupWindow popupWindow;
    EditText et_search;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        marked_notes = new LinkedList<>();
        notes_ids = new HashMap<LinearLayout, Integer>();
        curr_menu_id = R.menu.menu;
        setContentView(R.layout.all_notes_layout);
        set_action_bar();
        set_FABs();
        server_request();
        create_layout();
        setup_search_window();
    }

    private void search_request(String searchText){
        handle_response_q = new LinkedBlockingQueue<>();
        JSONClient.Request request = JSONClient.Request.notes;
        FunctionParam group_param = new FunctionParam("group", "search");
        FunctionParam search_text_param = new FunctionParam("search_text", searchText);
        FunctionParam owner_param = new FunctionParam("owner", Login.get_username());
        setContentView(R.layout.all_notes_layout);
        set_action_bar();
        set_FABs();
        JSONClient.server_comm(this, handle_response_q,request, group_param, owner_param,
                search_text_param);
        create_layout();
    }

    private void setup_search_window() {
        popupWindow = new PopupWindow(this);
        View popupView = getLayoutInflater().inflate(R.layout.search_window, null);
        popupWindow.setContentView(popupView);
        et_search = popupView.findViewById(R.id.et_search);
        Button submitButton = popupView.findViewById(R.id.btnSearch);

        // Add this code to open the keyboard when the EditText is focused
        et_search.requestFocus();
        popupWindow.setFocusable(true);

        // Add this code to listen for the keyboard opening
        et_search.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                et_search.getWindowVisibleDisplayFrame(r);
                int screenHeight = et_search.getRootView().getHeight();
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
                String searchText = et_search.getText().toString().trim();
                popupWindow.dismiss();
                search_request(searchText);
            }
        });
    }


    /**Sets the action bar by its xml description and ID
     *
     */
    private void set_action_bar(){
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ImageView searchView = findViewById(R.id.search_view);
        searchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show the popup window
                popupWindow.showAtLocation(v, Gravity.CENTER, 0, 0);
            }
        });
        toolbar.setOverflowIcon(ContextCompat.getDrawable(this, R.drawable.menu));
    }

    /**Sets the add, scan and type FABs
     *
     */
    private void set_FABs(){
        AddFabs addFabs = new AddFabs(this);
        addFabs.run_FABs();
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(curr_menu_id, menu);
        return true;
    }

    /**Sends the server a request for getting all of the notes
     *
     */
    private void server_request(){
        handle_response_q = new LinkedBlockingQueue<>();
        JSONClient.Request request = JSONClient.Request.notes;
        FunctionParam group_param = new FunctionParam("group", "all");
        FunctionParam owner_param = new FunctionParam("owner", Login.get_username());
        JSONClient.server_comm(this, handle_response_q,request, group_param, owner_param);
    }

    /** adds the design to the text view by the json object
     *
     * @param body the text view to apply the design on
     * @param design the json object defining the text design
     */
    private void add_design(TextView body, JSONObject design){
        try {
            Boolean bold = (Boolean) design.get("paintFlags");
            if (bold){
                body.setPaintFlags(body.getPaintFlags() | Paint.FAKE_BOLD_TEXT_FLAG);
            }
            String typeface = design.getString("typeface");
            body.setTypeface(Typeface.create(typeface, Typeface.NORMAL));
        } catch (Exception e) {
            Toasts.error_toast(this);
        }
    }

    private void set_stared_or_regular(LinearLayout shape, Boolean is_stared){
        if (is_stared){
            shape.setBackgroundResource(R.drawable.stared);
        }
        else{
            shape.setBackgroundResource(R.drawable.text_back);
        }
    }

    /**create note view based on the note response from the sever. creates TextViews for the title
     * and body and sets style for each one of theme and for the shape
     *
     * @param notes_response: the note response from the sever, a JSON object with title and body
     *                      fields
     * @return the note's view
     */
    private LinearLayout create_view(JSONObject notes_response) throws JSONException {
        TextView title_tv = new TextView(this);
        title_tv.setTextAppearance(R.style.my_text_mid_note_title);
        title_tv.setText(notes_response.get("title").toString());
        TextView body_tv = new TextView(this);
        body_tv.setTextAppearance(R.style.my_text_small_note_body);
        body_tv.setText(notes_response.getString("text"));
        add_design(body_tv, notes_response);

        LinearLayout shape = new LinearLayout(this, null, 0,
                R.style.text_shape);
        set_stared_or_regular(shape, notes_response.getBoolean("star"));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 20, 0, 0);
        shape.setLayoutParams(params);
        shape.addView(title_tv);
        shape.addView(body_tv);

        notes_ids.put(shape, notes_response.getInt("id"));
        return shape;
    }

    private void next_note(){
        JSONClient.Request request = JSONClient.Request.next_note;
        JSONClient.server_comm(this, handle_response_q,request);
    }

    /** Receiving the server's note responses. Adds every note to one of the 3 columns ny their
     * order.
     */
    private void create_layout(){
        try {
            int count_layout = 0;
            JSONObject response_json = handle_response_q.poll(BlockingQueueTimeoutHandler.timeout,
                    TimeUnit.MILLISECONDS);
            if (response_json == null){
                BlockingQueueTimeoutHandler.redirectToErrorActivity(this);
            }
            Object curr_response = response_json.get("return_code");
            while (!"END".equals(curr_response)){
                JSONObject curr_note = (JSONObject) curr_response;
                int resID = getResources().getIdentifier("col"+ count_layout % 3,
                        "id", getPackageName());
                LinearLayout layout = findViewById(resID);
                LinearLayout shape = create_view(curr_note);
                shape.setOnClickListener(this);
                shape.setOnLongClickListener(this);
                layout.addView(shape);
                next_note();
                response_json = handle_response_q.poll(BlockingQueueTimeoutHandler.timeout,
                        TimeUnit.MILLISECONDS);
                if (response_json == null){
                    BlockingQueueTimeoutHandler.redirectToErrorActivity(this);
                }
                curr_response = response_json.get("return_code");
                count_layout++;
            }
        } catch (Exception e) {
            Toasts.error_toast(this);
        }
    }

    private void get_stared_request(){
        JSONClient.Request request = JSONClient.Request.notes;
        FunctionParam group_param = new FunctionParam("group", "stared");
        FunctionParam owner_param = new FunctionParam("owner", Login.get_username());
        JSONClient.server_comm(this, handle_response_q,request, group_param, owner_param);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.log_out:
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                return true;
            case R.id.star:
                for (LinearLayout marked_note:marked_notes){
                    try {
                        Boolean is_stared = star_request(notes_ids.get(marked_note));
                        set_stared_marked(marked_note, is_stared);
                    } catch (Exception e) {
                        Toasts.error_toast(this);
                    }
                }
                return true;
            case R.id.stared_notes:
                setContentView(R.layout.all_notes_layout);
                set_action_bar();
                set_FABs();
                get_stared_request();
                create_layout();
                return true;
            case R.id.all_notes:
                setContentView(R.layout.all_notes_layout);
                set_action_bar();
                set_FABs();
                server_request();
                create_layout();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private Boolean star_request(int id) throws InterruptedException, JSONException {
        handle_response_q = new LinkedBlockingQueue<>();
        JSONClient.Request request = JSONClient.Request.new_note;
        FunctionParam group_param = new FunctionParam("star", true);
        FunctionParam owner_param = new FunctionParam("id", id);
        JSONClient.server_comm(this, handle_response_q,request, group_param, owner_param);
        JSONObject response_json = handle_response_q.poll(BlockingQueueTimeoutHandler.timeout,
                TimeUnit.MILLISECONDS);
        if (response_json == null){
            BlockingQueueTimeoutHandler.redirectToErrorActivity(this);
        }
        return response_json.getBoolean("return code");
    }

    @Override
    public void onClick(View view) {
        Intent intent = new Intent(this, NewNote.class);
        intent.putExtra("ID", notes_ids.get(view));
        startActivity(intent);
    }

    private Boolean check_star_request(int id) throws InterruptedException, JSONException {
        handle_response_q = new LinkedBlockingQueue<>();
        JSONClient.Request request = JSONClient.Request.notes;
        FunctionParam group_param = new FunctionParam("group", "s_note");
        FunctionParam id_param = new FunctionParam("id", id);
        FunctionParam owner_param = new FunctionParam("owner", Login.get_username());
        JSONClient.server_comm(this, handle_response_q,request, group_param, id_param,
                owner_param);
        JSONObject response_json = handle_response_q.poll(BlockingQueueTimeoutHandler.timeout,
                TimeUnit.MILLISECONDS);
        if (response_json == null){
            BlockingQueueTimeoutHandler.redirectToErrorActivity(this);
        }
        return response_json.getJSONObject("return_code").getBoolean("star");
    }

    private void set_stared_marked(LinearLayout layout, Boolean is_stared){
        if (is_stared){
            layout.setBackgroundResource(R.drawable.stared_marked);
        }
        else {
            layout.setBackgroundResource(R.drawable.text_back_marked);
        }
    }


    @Override
    public boolean onLongClick(View view) {
        if (marked_notes.contains((LinearLayout) view)){
            try {
                set_stared_or_regular((LinearLayout) view, check_star_request(notes_ids.get(view)));
            } catch (Exception e) {
                Toasts.error_toast(this);
            }
            marked_notes.remove((LinearLayout) view);
        }
        else{
            try {
                Boolean is_stared = check_star_request(notes_ids.get(view));
                set_stared_marked((LinearLayout) view, is_stared);
            } catch (Exception e) {
                Toasts.error_toast(this);
            }
            marked_notes.add((LinearLayout) view);
        }
        if (!marked_notes.isEmpty()) {
            curr_menu_id = R.menu.edit_menu;
        }
        else{
            curr_menu_id = R.menu.menu;
        }
        invalidateOptionsMenu();
        return true;
    }
}
