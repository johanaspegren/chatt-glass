package com.aie.chatt_glass;

// Filename: APIRequestTask.java
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class APIRequestTask extends AsyncTask<Void, Void, String> {

    private String apiKey;
    private String prompt;
    private String TAG = "==>";

    public APIRequestTask(String apiKey, String prompt) {
        this.apiKey = apiKey;
        this.prompt = prompt;
    }

    @Override
    protected String doInBackground(Void... voids) {
        String responseString = "";

        try {
            URL url = new URL("https://discoveryengine.googleapis.com/v1alpha/projects/546272475523/locations/global/collections/default_collection/dataStores/aie-ac-manuals-ds_1696848157153/conversations/-:converse");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);
            conn.setDoInput(true);

            String jsonPayload = "{\"query\":{\"input\":\"" + prompt + "\"}}";
            jsonPayload = "{\"query\":{\"input\":\"is this a test?\"}}";
            Log.d(TAG, "jsonPayload" + jsonPayload);
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonPayload.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            InputStream in = new BufferedInputStream(conn.getInputStream());
            responseString = convertStreamToString(in);

        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d(TAG, "response from ac-docs" + responseString);
        Log.d(TAG, "response from ac-docs" + responseString.toString());
        Log.d(TAG, "response from ac-docs" + responseString);
        return responseString;
    }

    private String convertStreamToString(InputStream is) {
        Scanner s = new Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}
