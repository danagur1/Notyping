package com.example.finalproject2;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.*;
import java.net.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.*;

public class JSONClient {

    private static Encryption encryption;

    final protected static String hostName = "172.19.24.27";
    //my Home IP: 10.100.102.33
    //current school IP: 172.19.19.94
    final protected static int portNumber = 1024;
    final private static int MESSAGE_SIZE = 255;

    //Enum defines the possible requests from the server:
    enum Request {login, register, forgot_pass, notes, new_note, next_note, encryption}

    //--------------------------------------Overall communicating process---------------------------

    /**Android doesn't allow running internet connection on the main thread,
     * so a Runnable object is created and run server_comm on a new thread
     * @param activity:
     *                the activity which needs communication with server
     * @param handle_response_q:
     *                         a queue for all the responds received from the server that needs to
     *                         be handled by main thread
     * @param request:
     *               the request to be sent to the server
     * @param params:
     *              the parameters relevant for the request to be sent to the server
     */
    protected static void server_comm(Activity activity,BlockingQueue<JSONObject> handle_response_q,
                                      Request request, FunctionParam... params) {
        Runnable server_comm_and_resp = () -> {
            JSONObject result = null;
            try {
                result = JSONClient.thread_server_comm(activity, request, params);
            } catch (Exception e) {
                e.printStackTrace();
                Toasts.error_toast(activity);
            }
            JSONObject finalResult = result;
            try {
                handle_response_q.put(finalResult);
            } catch (Exception e) {
                Toasts.error_toast(activity);
            }
        };
        run_on_tread(server_comm_and_resp);
    }

    /**Uses the simple_thread_server_comm method and send messages sliced into small parts (in
     * order to encrypt them later with RSA)
     *
     * @param activity: the activity which there is communication on
     * @param request: the type of request from the server
     * @param params: the parameters required for the request from the server
     * @return the server's response
     * @throws Exception in case of error on server side- length sending
     */
    protected static JSONObject thread_server_comm(Activity activity, Request request,
                                                   FunctionParam... params) throws
            Exception {
        ask_internet_prem(activity);
        JSONObject request_json = JSONClient.create_object(request, params);
        String request_json_str = request_json.toString();
        int num_length = request_json_str.length();
        int num_of_requests = notify_amount_of_requests(num_length);
        //send the messages by their amount using simple_thread_server_comm (and doesn't
        // expect any response)
        for (int request_idx=0; request_idx<num_of_requests-1; request_idx++){
            simple_thread_server_comm(request_json_str.substring(request_idx*MESSAGE_SIZE,
                    (request_idx+1)*MESSAGE_SIZE), false);
        }
        //send the last message part to server and expect response
        return simple_thread_server_comm(request_json_str.substring((num_of_requests-1)*
                MESSAGE_SIZE, num_length), true);
    }

    /**Does all the communication with the server, sends requests and returns the answer in cases it is needed
     * @param request:
     *                    the string of the JSONObject of the request to be sent to server
     * @param response_needed:
     *                    True iff the request requires the servers' response
     * @return the JSONObject response that was received from server in case it is needed
     */
    private static JSONObject simple_thread_server_comm(String request, Boolean response_needed) throws
            IOException, JSONException {
        // create a socket connection to the server:
        Socket socket = new Socket(hostName, portNumber);
        JSONClient.send_encrypted(socket, request);
        if (response_needed){
            JSONObject received_json = JSONClient.receive_decrypted(socket);
            socket.close();
            return received_json;
        }
        else {
            socket.close();
            return new JSONObject();
        }

    }

    /** Run runnable in a new thread
     * @param runnable:
     *                the wanted function to run
     */
    private static void run_on_tread(Runnable runnable){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(runnable);
        executor.shutdown();
    }

    //---------------------------------Preparations------------------------------------------------

    protected static void server_setup(Activity activity){

        encryption = new Encryption(activity);
    }

    /** if there is no permission for the app for internet connection, asks for it
     * @param activity:
     *                the activity which needs the internet permission
     */
    private static void ask_internet_prem(Activity activity) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.INTERNET},
                    1);
        }
    }

    /** Send messages without encryption. This is for setting up the encryption
     * @param socket:
     *              the socket to sent the message on
     * @param request_json:
     *                    the json object to be sent to the server
     */
    protected static void send_for_encryption(Socket socket, JSONObject request_json) throws IOException {
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        out.println(request_json.toString());
    }

    /** Receive messages without encryption. This is for setting up the encryption
     * @param socket:
     *              the socket to receive the message on
     * @return JSONObject that received from server
     */
    protected static JSONObject receive_for_encryption(Socket socket) throws IOException, JSONException {
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String responseStr = in.readLine();
        return new JSONObject(responseStr);
    }



    //-------------------------------------SEND-----------------------------------------------------

    /**Find the amount of messages expected to be sent.
     * Send message to server to notify it and return it.
     *
     * @param length the length of the original message that needs to be sent
     * @return the amount of messages expected to be sent
     * @throws Exception in case of error on server side- length sending
     */
    private static int notify_amount_of_requests(int length) throws Exception {
        //finds the amount of requests expected to be sent
        int messages_amount = (int) Math.ceil((double)length / (double)MESSAGE_SIZE);
        //create a String to send to server and notify it the amount of requests expected to be sent
        String msg_length = Integer.toString(messages_amount);
        String complete_msg_length = new String(new char[3-msg_length.length()]).
                replace("\0", "0")+msg_length;
        //send the notification to server and check the return code
        JSONObject length_result = simple_thread_server_comm(complete_msg_length,
                false);
        return messages_amount;
    }

    /** create a JSON object for sending a request
     * @param request:
     *               the type of request in the created json object
     * @param params:
     *              the parameters relevant for the request in the created json object
     * @return JSONObject containing the request with its relevant parameters
     */
    protected static JSONObject create_object(Request request, FunctionParam... params) throws JSONException {
        JSONObject jsonObject = new JSONObject(); //the final json object
        jsonObject.put("request", request.toString());
        JSONObject params_json = new JSONObject(); //the params json object
        for (FunctionParam arg:params) {
            params_json.put(arg.name, arg.value);
        }
        jsonObject.put("params", params_json);
        return jsonObject;
    }

    /** send the JSON object to the server with its length at first
     * @param socket:
     *              the socket to sent the message on
     * @param message:
     *               the string to be sent to the server
     */
    protected static void send_encrypted(Socket socket, String message) throws IOException {
        //Use PrintWriter for sending messages to the server on socket
        PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(),
                "ISO_8859_1"),true);
        //encrypt the message
        String encrypted = encryption.encrypt(message);
        //create the length message for sending with 3 bytes
        String message_length = Integer.toString(encrypted.length());
        String message_length_completed = message_length+(new String(new char[3-message_length.length()]).replace
                ("\0", "0"));
        //Send the encrypted message with its length at first
        out.println(message_length_completed+encrypted);
    }

    //-----------------------------------RECEIVE----------------------------------------------------

    /**get the amount of expected messages. message is a part of the response which is at
     // most MESSAGE_SIZE bytes. The server first send 3 bytes representing this number.
     *
     * @param in the BufferedReader to read the amount messages from
     * @return the messages amount
     * @throws IOException in case of error in read function
     */
    private static int get_messages_amount(BufferedReader in) throws IOException {
        int MESSAGES_AMOUNT_SIZE = 3;
        char[] messages_amount = new char[MESSAGES_AMOUNT_SIZE];
        int read_result = in.read(messages_amount, 0, MESSAGES_AMOUNT_SIZE);
        if (read_result!=3){
            throw new IOException("error in read- didn't read 3 bytes for message amount");
        }
        return Integer.parseInt(new String(messages_amount));
    }

    /** Get a message from input_reader by its length in the first 3 bytes and decrypt its
     * encryption.
     *
     * @param input_reader the BufferedReader to read messages from
     * @return the message sent from the server
     * @throws IOException in case of error in read function
     */
    private static String get_message_by_length(BufferedReader input_reader) throws IOException {
        //first get the length of expected message
        char[] length = new char[3];
        int read_result = input_reader.read(length, 0, 3);
        if (read_result!=3){
            throw new IOException("error in read- didn't read 3 bytes for message amount");
        }
        int int_length = Integer.parseInt(new String(length));
        //then read the actual part of the response
        char[] responseStr = new char[int_length];
        read_result = input_reader.read(responseStr, 0, int_length);
        if (read_result!=int_length){
            throw new IOException("error in read- didn't read 3 bytes for message amount");
        }
        return encryption.decrypt(new String(responseStr));
    }

    /** read the response from the server
     * @param socket:
     *              the socket to receive the message on
     * @return JSONObject that received from server
     */
    protected static JSONObject receive_decrypted(Socket socket) throws IOException, JSONException {
        //create BufferedReader for reading server's response
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "ISO_8859_1"));
        StringBuilder all_response = new StringBuilder();
        int int_messages_amount = get_messages_amount(in);
        //receive every message and decrypt it separately
        for (int message_idx=0; message_idx<int_messages_amount; message_idx++){
            all_response.append(get_message_by_length(in));
        }
        return new JSONObject(all_response.toString());
    }

}
