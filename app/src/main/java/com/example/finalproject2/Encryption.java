package com.example.finalproject2;

import static com.example.finalproject2.JSONClient.hostName;
import static com.example.finalproject2.JSONClient.portNumber;

import android.app.Activity;

import org.json.JSONObject;

import java.math.BigInteger;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Encryption {

    private final BigInteger n;
    private final BigInteger d;
    private final BigInteger e;
    private BigInteger e_server;
    private BigInteger n_server;

    public Encryption(Activity activity) {
        // Generate two large prime numbers p and q
        Random rnd = new Random();
        BigInteger p = BigInteger.probablePrime(1024, rnd);
        BigInteger q = BigInteger.probablePrime(1024, rnd);

        // Calculate n = p * q and phi = (p - 1) * (q - 1)
        n = p.multiply(q);
        BigInteger phi = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE));

        // Choose a random number e such that 1 < e < phi and gcd(e, phi) = 1
        e = BigInteger.valueOf(65537);

        // Calculate d such that d = e^-1 mod phi
        d = e.modInverse(phi);

        //Setup- Exchange values with server
        setup(activity);
    }

    public String encrypt(String message) {
        if (e_server == null || n_server == null) {
            return message;
        }
        //BigInteger m = new BigInteger(message.getBytes());
        BigInteger m = new BigInteger(message.getBytes(StandardCharsets.ISO_8859_1));
        BigInteger ciphertext = m.modPow(e_server, n_server);
        //return ciphertext.toString();
        return new String(ciphertext.toByteArray(), StandardCharsets.ISO_8859_1);
    }

    public String decrypt(String ciphertext) {
        BigInteger ct = new BigInteger(1, ciphertext.getBytes(StandardCharsets.ISO_8859_1)); //string -> int
        BigInteger message = ct.modPow(d, n);
        return new String(message.toByteArray(), StandardCharsets.ISO_8859_1); //int -> string
    }

    /**creates a socket for communication with server. Create message for server, send the message
     * and receive the server's response. Finally close the server socket.
     *
     * @param request  the request type to sent to server
     * @param e_param the e value as parameter object for sending to server
     * @param n_param the n value as parameter object for sending to server
     * @return the json received from the server
     * @throws Exception in case of error on creating socket, closing socket, or errors in
     * functions send_for_encryption or receive_for_encryption
     */
    private JSONObject setup_comm(JSONClient.Request request, FunctionParam e_param,
                                  FunctionParam n_param) throws Exception{
        JSONObject request_json = JSONClient.create_object(request, e_param, n_param);
        Socket socket = new Socket(hostName, portNumber);
        JSONClient.send_for_encryption(socket, request_json);
        JSONObject received_json = JSONClient.receive_for_encryption(socket);
        socket.close();
        return received_json;
    }

    /**Defines the runnable for the internet connection thread, for sending and receiving from
     * server relevant values for encryption.
     *
     * @param request the request type to sent to server
     * @param e_param the e value as parameter object for sending to server
     * @param n_param the n value as parameter object for sending to server
     * @param handle_response_q the queue to receive the server's response into
     * @return the runnable defined
     */
    private Runnable define_runnable(JSONClient.Request request, FunctionParam e_param,
                                 FunctionParam n_param,
                                 BlockingQueue<JSONObject> handle_response_q, Activity activity){
        return () -> {
            JSONObject result = null;
            try {
                result =setup_comm(request, e_param, n_param);
            } catch (Exception e1) {
                Toasts.error_toast(activity);
            }
            JSONObject finalResult = result;
            try {
                handle_response_q.put(finalResult);
            } catch (Exception e1) {
                Toasts.error_toast(activity);
            }
        };
    }

    /**Execute runnable on a new thread and shut it down
     *
     * @param runnable the runnable to execute
     */
    private void execute(Runnable runnable){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(runnable);
        executor.shutdown();
    }

    /**This function is responsible for all the setup process that needed for the encryption.
     * This include sending my e and n values to the server and receive his e and n values.
     */
    protected void setup(Activity activity){
        BlockingQueue<JSONObject> handle_response_q;
        handle_response_q = new LinkedBlockingQueue<>();
        JSONClient.Request request = JSONClient.Request.encryption;
        FunctionParam e_param = new FunctionParam("e", e.toString());
        FunctionParam n_param = new FunctionParam("n", n.toString());

        Runnable server_comm_and_resp = define_runnable(request, e_param, n_param,
                handle_response_q, activity);
        execute(server_comm_and_resp);

        try {
            JSONObject response_json = handle_response_q.poll(BlockingQueueTimeoutHandler.timeout,
                    TimeUnit.MILLISECONDS);
            if (response_json == null){
                BlockingQueueTimeoutHandler.redirectToErrorActivity(activity);
            }
            JSONObject response = response_json;
            e_server = BigInteger.valueOf(((Integer) response.get("e")).longValue());
            n_server = new BigInteger((String) response.get("n"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
