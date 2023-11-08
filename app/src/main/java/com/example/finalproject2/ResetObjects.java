package com.example.finalproject2;

import android.app.Activity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.util.HashMap;
import java.util.Map;

public class ResetObjects<T> {
    /**Reset a button to listening in activity by its id
     *
     * @param activity the activity to find the id and to listen to
     * @param id the button's id (ad defined in the xml file)
     * @return the reseated button
     */
    protected static Button reset_button(Activity activity, String id){
        int resID = activity.getResources().getIdentifier(id,
                "id", activity.getPackageName());
         Button button = (Button) activity.findViewById(resID);
         button.setOnClickListener((View.OnClickListener) activity);
         return button;
    }

    /**Reset buttons by array of their ids
     *
     * @param activity the activity to find the ids and to listen to
     * @param ids the button's id (as defined in the xml file)
     * @return map with ids as keys and their matching buttons as values
     */
    protected static Map<String, Button> reset_buttons(Activity activity, String[] ids){
        Map result = new HashMap<String, Button>();
        for (String id:ids){
            result.put(id, ResetObjects.reset_button(activity, id));
        }
        return result;
    }

    /**Reset a view to listening in activity by its id
     *
     * @param activity the activity to find the id in
     * @param id the view's id (as defined in the xml file)
     * @return the reseated edit text
     */
    protected T reset_view(Activity activity, String id){
        int resID = activity.getResources().getIdentifier(id,
                "id", activity.getPackageName());
        T view = (T) activity.findViewById(resID);
        return view;
    }

    /**Reset views by array of their ids
     *
     * @param activity the activity to find the ids and to listen to
     * @param ids the view's id (as defined in the xml file)
     * @return map with ids as keys and their matching views as values
     */
    protected Map<String, T> reset_views(Activity activity, String[] ids){
        Map result = new HashMap<String, T>();
        for (String id:ids){
            result.put(id, this.reset_view(activity, id));
        }
        return result;
    }
}
