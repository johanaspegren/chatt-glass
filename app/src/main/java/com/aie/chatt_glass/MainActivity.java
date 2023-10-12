package com.aie.chatt_glass;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.VideoView;


import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

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

    private String ACGCPKEY_VALUE = "ya29.a0AfB_byD9nPSzCARFG0LyH1ZwbLdNtszBYJX8nSV6td0hWZAh9zmMn3fQARm8Ui8BPH251epZ4V4oGvfOxxdd416pa_ziuWH44SV8ExFe0du98OOs700J4q25b6ANp-VugHbPZB8UCxGgmvlXHWjPjCTRLapn4MqFtyqYFglYpLD-RpKcCWS5zz4DiBrtq9CMDVyN5ebyAAXZamZrTMfFlYSzuaqiP1A4r6AxO4hVL2HurCh_U2QD4xAZUvql6Zka6c2tnviqPb_Kr3WxfRVf321wiuClU4evTIUN_21ooZq50-2Cg3Th3RTN6_rDcgoaGFx_hsAr13AxmYHWERYDoMVgB1B6bbS2-K2xrglxJcvA_Bwcjja456DPIwFHDuPQCnbVUl9veM8nB6yO05hc3Bf0Ys-llCbmaCgYKAcMSARISFQGOcNnCXwmNq3_caTW5ZUQfLdqhlQ0423";

    private String LANDING_KEY = "land_sk_2xGcPfIeENggfWfCeUEVBL1XEF4rIJyDzhq2P0X3dpRM7R579b";
    String landingApiUrl = "https://predict.app.landing.ai/inference/v1/predict?endpoint_id=23143f74-008b-4a8f-a038-da6044fd5320";
    String landingMethod = "POST"; // Use POST method for uploading files
    String imagePath = "YOUR_IMAGE_PATH"; // Replace with the actual image file path

    String GC_API;
    private String APIKEY ="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";
    private String MODEL ="gpt-3.5-turbo-16k";

    private GlassGestureDetector glassGestureDetector;
    private String UNIQUE_ID = "12345";

    VideoView thinkingVideo;

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

        //executeAPIRequest("What is the dimension for the 6000 Power unit?");

        //String question = "What are the dimensions of the 6000 Power unit?";
        //askDoc(question);

        // set data
        String newData = "{\"message\": \"This is a new message\"}";
        //new FirebaseSendTask().execute(newData);

        // get data from firebase
        //new FirebaseFetchTask().execute();

        glassGestureDetector = new GlassGestureDetector(this, this);

        // init the chat
        messages.add(new ChatMessage("system", "You are a helpful assistant, answer questions with one or two sentences."));

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

                if (status == TextToSpeech.SUCCESS) {
                    textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override
                        public void onDone(String utteranceId) {
                            Log.d(TAG, "TTS finished");
                            requestVoiceRecognition();
                            // hide thinking video
                            thinkingVideo.stopPlayback();
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

    private void executeAPIRequest(String prompt) {

        APIRequestTask task = new APIRequestTask(ACGCPKEY_VALUE, prompt);
        task.execute();

        // Use an onPostExecute method inside the AsyncTask to process the result, or pass a callback to the APIRequestTask constructor
        // For simplicity, this code doesn't handle the response outside of the AsyncTask
    }


    private class FirebaseSendTask extends AsyncTask<String, Void, Boolean> {

        @Override
        protected Boolean doInBackground(String... params) {
            if (params.length == 0) {
                return false;
            }

            String newData = params[0];

            // Send data to Firebase using FirebaseHelper
            return FirebaseHelper.sendDataToFirebase(newData);
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
            } else {
                Log.d(TAG, "Failed to get data from Firebase");
                // Handle the error or retry logic
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


    private void askDoc(String question) {
        String apiKey = "ya29.a0AfB_byDHWyJlp7X6UbB1gVRT0QrrhGHsEd0CTB62GpItzYcAWR441MAtAfLNeb1nWhlIH_qcNzZ0_YuMwK8AsnK6JleG2aTUD9SOMO_f2JkEA2-LMxRCfQB6yrTrh5NAEtmPmUB080vPULm849e5RqhLxPOv5SCX5bJZMVFEzfQoii-mm149I32YMHZ0IJKMeGrI-qLCoIgWVwkpugReh4scHPW6WzUSqAOvjyYXlvwCwUPAHH3H5U5uJfROt1KG5tJ84lMigW2LaZawQ4zZwKG4x0704BpEZVxRp80NNw3-raloZkM32Nz2iQYp06O3ueXRiChOS8unsG43UMDRKQOhax8yfY_zaPYbw3xmpOwbwctZRwn2kIWzYsKSdFVmGCNNdCuYJm_vMATMhdRyyHyHVr27hGQaCgYKAc4SARISFQGOcNnCxSvMhTV0ZP7nOMePC6mzVQ0422";
        GC_API = apiKey;

        ApiTest.requestApiResponse(
            GC_API, question, response -> {
                Log.d("MainActivity",
                        "ApiTest Received summary text: " + response);
                handleChatResponse(response);
            });
    }

    private void sendChatRequest(String userMessage) {
        ChatMessage userChatMessage = new ChatMessage("user", userMessage);
        messages.add(userChatMessage);

        // Execute the chat request task
        chatRequestTask = new ChatRequestTask(APIKEY, MODEL, messages, new ChatRequestTask.ChatRequestListener() {
            @Override
            public void onSuccess(String response) {
                String cleanResp = "";

                JSONObject jsonObject = null;
                try {
                    jsonObject = new JSONObject(response);
                    JSONObject messageObject = jsonObject.getJSONArray("choices").getJSONObject(0).getJSONObject("message");
                    cleanResp = messageObject.getString("content");
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }

                handleChatResponse(cleanResp);
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
        Log.d(TAG, response);

        // System.out.println("Content: " + content);
        textToSpeech.speak(response, TextToSpeech.QUEUE_FLUSH, null, UNIQUE_ID);

        // add chattys
        ChatMessage assistantChatMessage = new ChatMessage("assistant", response);
        messages.add(assistantChatMessage);

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
                if(results.toString().contains("blueprint")){
                    // perhaps open QR
                }
                //sendChatRequest(results.toString());

                askDoc(results.toString());

                // start thinking video
                thinkingVideo.start();
                thinkingVideo.setVisibility(View.VISIBLE);

            }
        }

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri selectedImageUri = data.getData();
            Log.d(TAG, "fetch image " + selectedImageUri);
            // Now you have the selected image URI, and you can perform actions with it.
        }

        if (requestCode == REQUEST_QR && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Log.d(TAG, "fetch qr extras" + extras.toString());

            // Now you have the selected image URI, and you can perform actions with it.
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
