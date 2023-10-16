package com.aspegrenide.chat_glass;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;
import android.widget.VideoView;


import com.aspegrenide.chat_glass.ChatMessage;
import com.aspegrenide.chat_glass.ChatRequestTask;
import com.aspegrenide.chat_glass.GlassGestureDetector;
import com.aspegrenide.chat_glass.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;


public class MainActivity extends AppCompatActivity implements GlassGestureDetector.OnGestureListener {
    private static final int REQUEST_CODE_PERMISSIONS = 101;
//    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};

    private static final int REQUEST_CODE_VOICE = 999;
    private static final Integer REQUEST_CODE_AUDIO_REC_PERM = 1;
    private static final int REQUEST_CODE_CAMERA_PERM = 2;
    private static final int REQUEST_IMAGE_CAPTURE = 3;
    private static final int REQUEST_GALLERY_FETCH = 4;
    private static final int REQUEST_QR = 5;

    private TextToSpeech textToSpeech;
    private Button scanBtn;
    private String TAG = "==>";
    private ChatRequestTask chatRequestTask;
    List<ChatMessage> messages = new ArrayList<>();


    private String LANDING_KEY = "land_sk_2xGcPfIeENggfWfCeUEVBL1XEF4rIJyDzhq2P0X3dpRM7R579b";
    String landingApiUrl = "https://predict.app.landing.ai/inference/v1/predict?endpoint_id=23143f74-008b-4a8f-a038-da6044fd5320";
    String landingMethod = "POST"; // Use POST method for uploading files
    String imagePath = "YOUR_IMAGE_PATH"; // Replace with the actual image file path

    private String APIKEY ="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";
    private String MODEL ="gpt-3.5-turbo-16k";

    private String DOC_API = "";

    private GlassGestureDetector glassGestureDetector;
    private String UNIQUE_ID = "12345";

    VideoView thinkingVideo;
    boolean stopListening;

    ImageView wifiStatusIcon;
    ImageView firebaseStatusIcon;
    ImageView openaiStatusIcon;

    TextView wifiStatusText;
    TextView firebaseStatusText;
    TextView openaiStatusText;

    ImageView imgDrawing;

/*
Unplug USB cable from computer
Revoke USB Debug permissions
Disable USB Debug
Plug USB cable in computer
Enable USB Debug => now Glass should ask allow?
 */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_chat);

        String question = "Is this a test";
        Log.d(TAG, "askdoc test api");
        AskDocUtil.AskDoc(DOC_API, question, new AskDocUtil.AskDocCallback() {
            @Override
            public void onResult(String response) {
                Log.d("AskDoc", "Response: " + response);
            }

            @Override
            public void onError(Exception e) {
                Log.e("AskDoc", "Error: ", e);
            }
        });


        stopListening = false;

        imgDrawing = findViewById(R.id.imgDrawings);
        imgDrawing.setVisibility(View.GONE);
        wifiStatusIcon = findViewById(R.id.wifiStatusIcon);
        firebaseStatusIcon = findViewById(R.id.firebaseStatusIcon);
        openaiStatusIcon = findViewById(R.id.openaiStatusIcon);

        wifiStatusText = findViewById(R.id.txtWifiStatus);
        firebaseStatusText = findViewById(R.id.txtCloudStatus);
        openaiStatusText = findViewById(R.id.txtRobotStatus);

        // Check the status of each component and update the icons accordingly
        boolean isWifiConnected = isWifiConnected(); // Implement this method
        boolean isFirebaseConnected = isFirebaseConnected(); // Implement this method
        boolean isOpenAIOnline = isOpenAIOnline(); // Implement this method

        setWifiStatus(isWifiConnected());
        setFirebaseStatus(isFirebaseConnected);
        setOpenaiStatus(isOpenAIOnline());

        // set data
        // String newData = "{\"message\": \"This is a new message\"}";
        // new FirebaseSendTask().execute(newData);

        // get data from firebase
        // new FirebaseFetchTask().execute();

        glassGestureDetector = new GlassGestureDetector(this, this);

        // init the chat
        messages.add(new ChatMessage(
                "system",
                "You are a helpful assistant, answer questions with one or two sentences." +
                        "You will help me identify the correct drawing to show. There are a few that " +
                        "you know of: " +
                        "{'description':'Raspberry pin out', 'filename':'rpi_3'}," +
                        "{'description':'Arduino pin out', 'filename':'ard_1'}," +
                        "{'description':'other', 'filename':'other_1'}" +
                        "If I am asking for a drawing that matches the description of any of the items in the list " +
                        "then return the filename in the format filename:filename"));


        // video while thinking
        thinkingVideo = findViewById(R.id.videoView);
        thinkingVideo.setVideoPath("android.resource://" + getPackageName() + "/" + R.raw.matrix);
        thinkingVideo.setOnCompletionListener(mp -> {
            thinkingVideo.setVisibility(View.GONE);
        });

        // text to speech init
        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                Log.d(TAG, "TTS init, utteranceId");
                textToSpeech.speak("Hi, ready to get busy?", TextToSpeech.QUEUE_FLUSH, null, UNIQUE_ID);

                if (status == TextToSpeech.SUCCESS) {
                    textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override
                        public void onDone(String utteranceId) {
                            Log.d(TAG, "TTS finished");
                            if(stopListening){
                                stopListening = false;
                                return;
                            }

                            requestVoiceRecognition();
                            // hide thinking video
                            // thinkingVideo.stopPlayback();
                            thinkingVideo.setVisibility(View.GONE);

                        }

                        @Override
                        public void onError(String utteranceId) {
                            Log.d(TAG, "TTS onError");
                        }

                        @Override
                        public void onStart(String utteranceId) {
                            Log.d(TAG, "TTS onStart");
                        }
                    });
                } else {
                    Log.e(TAG, "Initilization Failed!");
                }
            }
        });


        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
            checkPermission();
        }

    }

    private void scanQr(){
        Log.d(TAG, "init scanQr");
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE, IntentIntegrator.CODE_128);
        integrator.setPrompt("Scan a barcode");
        integrator.setCameraId(0); // Use the device's default camera
        integrator.setOrientationLocked(false); // Allow both portrait and landscape orientation
        integrator.setBeepEnabled(true); // Enable a beep sound after successful scan
        integrator.setRequestCode(REQUEST_QR);
        integrator.initiateScan();
    }

    private boolean isOpenAIOnline() {
        sendChatRequest("Is this a test?");
        return false;
    }

    private boolean isFirebaseConnected() {
        // get data from firebase
        new FirebaseFetchTask().execute();
        return false;
    }

    private boolean isWifiConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

            // Check if the active network is Wi-Fi and it is connected
            if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI && networkInfo.isConnected()) {
                return true; // Wi-Fi is online
            }
        }

        return false; // Wi-Fi is not online or the check failed
    }


    private class FirebaseSendTask extends AsyncTask<String, Void, Boolean> {

        @Override
        protected Boolean doInBackground(String... params) {
            if (params.length == 0) {
                return false;
            }

            String newData = params[0];

            // Send data to Firebase using FirebaseHelper
            return com.aspegrenide.chat_glass.FirebaseHelper.sendDataToFirebase(newData);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                Log.d(TAG, "Data sent to Firebase successfully");
                // Handle success, e.g., show a confirmation message to the user
            } else {
                Log.e(TAG, "Failed to send data to Firebase");
                // Handle failure, e.g., show an error message to the user
            }
        }
    }

    private void setWifiStatus(boolean online){
        if(online){
            wifiStatusIcon.setImageResource(R.drawable.wifi_online);
            wifiStatusText.setText("Connected to Wifi");
        }else {
            wifiStatusIcon.setImageResource(R.drawable.wifi_offline);
            wifiStatusText.setText("Not connected to Wifi");
        }
    }

    private void setFirebaseStatus(boolean online){
        if(online){
            firebaseStatusIcon.setImageResource(R.drawable.cloud_online);
            firebaseStatusText.setText("Firebase is online");
        }else {
            firebaseStatusIcon.setImageResource(R.drawable.cloud_offline);
            firebaseStatusText.setText("Firebase is offline");
        }
    }

    private void setOpenaiStatus(boolean online){
        if(online){
            openaiStatusIcon.setImageResource(R.drawable.robot_online);
            openaiStatusText.setText("OpenAI is online");
        }else {
            openaiStatusIcon.setImageResource(R.drawable.robot_offline);
            openaiStatusText.setText("OpenAI is offline");
        }
    }

    private class FirebaseFetchTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... voids) {
            // Call the FirebaseHelper to fetch data from Firebase
            return FirebaseHelper.fetchDataFromFirebase();
        }

        @Override
        protected void onPostExecute(String firebaseData) {
            if (firebaseData != null) {
                Log.d(TAG, "Got data from Firebase: " + firebaseData);
                // Process the Firebase data as needed
                setFirebaseStatus(true);
            } else {
                Log.d(TAG, "Failed to get data from Firebase");
                // Handle the error or retry logic
                setFirebaseStatus(false);
            }
        }
    }
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        Log.d(TAG,"didispatchTouchEvent");
        return glassGestureDetector.onTouchEvent(ev) || super.dispatchTouchEvent(ev);
    }

    public boolean onGesture(GlassGestureDetector.Gesture gesture) {
        Log.d(TAG, "onGesture");
        switch (gesture) {
            case TAP_AND_HOLD:
                Log.d(TAG, "tap and hold");
//                runQr();
                return true;
            case TAP:
                Log.d(TAG, "tap");
                //toggleRecording();
                requestVoiceRecognition();
                return true;
            case SWIPE_FORWARD:
                Log.d(TAG, "swipe forward");
                // open video
                return true;
            case SWIPE_BACKWARD:
                Log.d(TAG, "swipe backward");
                return true;
            case SWIPE_DOWN:
                Log.d(TAG, "swipe down");
                this.finish();
                return true;
            default:
                return false;
        }
    }


    private void sendChatRequest(String userMessage) {
        ChatMessage userChatMessage = new ChatMessage("user", userMessage);
        messages.add(userChatMessage);

        // Execute the chat request task
        chatRequestTask = new ChatRequestTask(APIKEY, MODEL, messages, new ChatRequestTask.ChatRequestListener() {
            @Override
            public void onSuccess(String response) {
                handleChatResponse(response);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, error);
            }
        });
        chatRequestTask.execute();
    }


    private void handleChatResponse(String response) {
        // Handle the response from the API
        Log.d(TAG, "handleChatResponse:" + response);
        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONObject messageObject = jsonObject.getJSONArray("choices").getJSONObject(0).getJSONObject("message");

            String content = messageObject.getString("content");

            // set icon to active
            setOpenaiStatus(true);

            if(content.toString().contains("drawing")) {
                // imgDrawing.setVisibility(View.VISIBLE);
                if (content.toString().contains("rpi_3")) {
                 //   imgDrawing.setImageResource(R.drawable.rpi_3);
                }
                if (content.toString().contains("ard_1")) {
                 //   imgDrawing.setImageResource(R.drawable.ard_1);
                }
            }

            // System.out.println("Content: " + content);
            textToSpeech.speak(content, TextToSpeech.QUEUE_FLUSH, null, UNIQUE_ID);

            // add chattys
            ChatMessage assistantChatMessage = new ChatMessage("assistant", content);
            messages.add(assistantChatMessage);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult resultCode: " + resultCode);
        Log.d(TAG, "onActivityResult requestCode: " + requestCode);
        Log.d(TAG, "onActivityResult data: " + data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode != RESULT_OK) {
            Log.d(TAG, "Back from camera, no image: ");
        }

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Log.d(TAG, "REQUEST_IMAGE_CAPTURE results: " + data.toString());
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");

            // Save the image to a file and obtain its path
            //String imagePath = saveImageToFile(imageBitmap);
            //Log.d("Image Path", imagePath);
            //checkImage(LANDING_KEY, imagePath);
        }

        if (requestCode == REQUEST_CODE_VOICE &&resultCode == RESULT_OK) {
            final List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            Log.d(TAG, "results: " + results.toString());
            if (results != null && results.size() > 0 && !results.get(0).isEmpty()) {
                Log.d(TAG, "Understood:" + results.toString());
                //textToSpeech.speak(results.toString(), TextToSpeech.QUEUE_FLUSH, null, null);
                if(results.toString().contains("QR")) {
                    textToSpeech.speak("Scanning QR", TextToSpeech.QUEUE_FLUSH, null, UNIQUE_ID);
                    stopListening = true;
                    scanQr();
                    return;
                }else if(results.toString().contains("stop")){
                        textToSpeech.speak("Ok, see you later", TextToSpeech.QUEUE_FLUSH, null, UNIQUE_ID);
                        stopListening = true;
                } else {
                    // start thinking video
                    thinkingVideo.start();
                    thinkingVideo.setVisibility(View.VISIBLE);

                    sendChatRequest(results.toString());
                }
            }
        }

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri selectedImageUri = data.getData();
            Log.d(TAG, "fetch image " + selectedImageUri);
            // Now you have the selected image URI, and you can perform actions with it.
        }

        if (requestCode == REQUEST_QR){
            if(resultCode == RESULT_OK && data != null) {
    //            IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
                String contents = data.getStringExtra("SCAN_RESULT");
                Log.d(TAG, "qr string = " + contents);
                sendChatRequest("I have scanned a QR code with this content, please explain: " + contents);
            } else {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    private void requestVoiceRecognition() {
        final Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "sv_SE");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "sv_SE");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        startActivityForResult(intent, REQUEST_CODE_VOICE);
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_CODE_AUDIO_REC_PERM);
        }
        // Check for CAMERA permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_CAMERA_PERM);
        }
    }

}