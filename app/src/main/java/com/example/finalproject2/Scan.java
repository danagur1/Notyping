package com.example.finalproject2;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.ImageView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**This class does everything related to scanning text and other pictures and saving them
 *
 */
public class Scan extends AppCompatActivity {

    private Integer id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scan);
        ask_camera_prem();
        dispatchTakePictureIntent();
    }

    public static Bitmap loadBitmapFromFile(String filePath) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeFile(filePath, options);
    }

    /** Converts the bitmap into a bytes array and encodes it to a string with base64
     *
     * @param bitmap the bitmap to transfer to string
     * @return the string representing the bitmap
     */
    private String create_image_string(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageBytes = baos.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }

    /** Creates the request for the server for new note containing photo
     *
     * @param image
     */
    private void send_image(Bitmap image){
        FunctionParam type = new FunctionParam("note_type", "SCAN");
        String image_string = create_image_string(image);
        FunctionParam p_image = new FunctionParam("image", image_string);
        JSONClient.Request request = JSONClient.Request.new_note;
        BlockingQueue<JSONObject> handle_response_q = new LinkedBlockingQueue<>();
        JSONClient.server_comm(this, handle_response_q, request, type, p_image);
        try {
            JSONObject response_json = handle_response_q.poll(100000,
                    TimeUnit.MILLISECONDS);
            if (response_json == null){
                BlockingQueueTimeoutHandler.redirectToErrorActivity(this);
            }
            id = (Integer)response_json.get("return_code");
        } catch (Exception e) {
            Toasts.error_toast(this);
        }
    }

    private void return_to_all_notes(){
        Intent intent = new Intent(this, AllNotes.class);
        startActivity(intent);
    }

    /** Creates the launcher for opening camera and taking the picture.
     * Defines the what to do after picture taken
     *
     * @return the launcher
     */
    private ActivityResultLauncher<Intent> create_launcher(){
        ActivityResultLauncher<Intent> launcher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            Intent data = result.getData();
                            Bitmap image = (Bitmap) data.getExtras().get("data");
                            send_image(image);
                            //return_to_all_notes();
                            Intent intent_edit = new Intent(getApplicationContext(), NewNote.class);
                            intent_edit.putExtra("ID", id);
                            startActivity(intent_edit);
                        }
                    }
                });
        return launcher;
    }

    /**Uses the camera launcher, creates an intent and launch camera
     *
     */
    private void dispatchTakePictureIntent() {
        ActivityResultLauncher<Intent> launcher = create_launcher();
        // Create the camera_intent ACTION_IMAGE_CAPTURE it will open the camera for capture the
        // image
        Intent camera_intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Start the activity with camera_intent, and request pic id
        launcher.launch(camera_intent);
    }

    /** if there is no permission for the app for camera, asks for it
     *
     */
    private void ask_camera_prem() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    1);
        }
    }
}
