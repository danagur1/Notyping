package com.example.finalproject2;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Map;
import java.util.Objects;

/**Sets the FAB buttons for the notes screens that used for adding new notes
 *
 */
public class AddFabs{

    Map<String, FloatingActionButton> FABs;
    // These are taken to make visible and invisible along with FABs
    TextView type_text, scan_text;
    // to check whether sub FAB buttons are visible or not
    Boolean FABs_visible;

    Activity activity;

    protected AddFabs(Activity activity){
        ResetObjects<FloatingActionButton> resetFAB = new ResetObjects<>();
        this.FABs = resetFAB.reset_views(activity, new String[]{"fab_add", "fab_type", "fab_scan"});
        this.type_text = activity.findViewById(R.id.type_text);
        this.scan_text = activity.findViewById(R.id.scan_text);
        this.activity = activity;
    }



    /** change views visibility to GONE
     *
     * @param views: the views to make invisible
     */
    private void make_invisible(View... views) {
        for (View view:views){
            view.setVisibility(View.GONE);
        }
    }

    /** change views visibility to VISIBLE
     *
     * @param views: the views to make visible
     */
    private void make_visible(View... views) {
        for (View view:views){
            view.setVisibility(View.VISIBLE);
        }
    }

    /**changes the state of visiblility of the type and scan buttons and texts
     *
     * @param view the view that clicked
     */
    private void on_click_add(View view){
        if (!FABs_visible) {
            Objects.requireNonNull(FABs.get("fab_type")).show();
            Objects.requireNonNull(FABs.get("fab_scan")).show();
            make_visible(type_text, scan_text);
            FABs_visible = true;
        } else {
            Objects.requireNonNull(FABs.get("fab_type")).hide();
            Objects.requireNonNull(FABs.get("fab_scan")).hide();
            make_invisible(type_text, scan_text);
            FABs_visible = false;
        }
    }

    /**Moves to the New Note screen for editing the note
     *
     * @param view: the view clicked on
     */
    private void on_click_type(View view){
        Intent intent=new Intent(view.getContext(), NewNote.class);
        view.getContext().startActivity(intent);
    }

    /**
     * 
     * @param view:
     */
    private void on_click_scan(View view){
        Intent intent=new Intent(view.getContext(), Scan.class);
        view.getContext().startActivity(intent);
    }
    
    /**Set the click listeners for the FABs
     *
     */
    protected void run_FABs() {
        make_invisible(FABs.get("fab_scan"), FABs.get("fab_type"), type_text, scan_text);
        FABs_visible = false;
        Objects.requireNonNull(FABs.get("fab_add")).setOnClickListener(this::on_click_add);
        Objects.requireNonNull(FABs.get("fab_type")).setOnClickListener(this::on_click_type);
        Objects.requireNonNull(FABs.get("fab_scan")).setOnClickListener(this::on_click_scan);
    }
}
