package com.aie.chatt_glass;

import android.os.AsyncTask;
import android.util.Log;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import org.json.JSONObject;

public class ApiTest {

    private static final String TAG = "ApiTest";

    public static void requestApiResponse(String apiKey, String prompt, ApiResponseCallback callback) {
        new ApiRequestTask(apiKey, prompt, callback).execute();
    }

    private static class ApiRequestTask extends AsyncTask<Void, Void, String> {
        private String apiKey;
        private String prompt;
        private ApiResponseCallback callback;

        public ApiRequestTask(String apiKey, String prompt, ApiResponseCallback callback) {
            this.apiKey = apiKey;
            this.prompt = prompt;
            this.callback = callback;
        }

        @Override
        protected String doInBackground(Void... voids) {
            return makeRequest(apiKey, prompt);
        }

        @Override
        protected void onPostExecute(String response) {
            if (callback != null) {
                callback.onResponseReceived(response);
            }
        }
    }

    public static String makeRequest(String apiKey, String prompt) {
        String responseString = "";
        Log.d(TAG, "make request " + prompt);
        String summaryText = null;
        try {
            URL url = new URL("https://discoveryengine.googleapis.com/v1alpha/projects/546272475523/locations/global/collections/default_collection/dataStores/aie-ac-manuals-ds_1696848157153/conversations/-:converse");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);
            conn.setDoInput(true);

            String jsonPayload = "{" +
                    "\"query\": {\"input\":\"" + prompt + "\"}," +
                    "\"summarySpec\": {\"summaryResultCount\": 5, \"ignoreAdversarialQuery\": true, \"includeCitations\": true}" +
                    "}";

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonPayload.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            InputStream in = new BufferedInputStream(conn.getInputStream());
            responseString = convertStreamToString(in);

            summaryText = ApiTest.extractSummaryText(responseString);
            Log.d(TAG, "make request done summaryText" + summaryText);
            Log.d(TAG, "make request done" + responseString);

        } catch (Exception e) {
            Log.e(TAG, "Error making the request", e);
        }

        return summaryText;
    }

    public static String extractSummaryText(String jsonResponse) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            JSONObject reply = jsonObject.getJSONObject("reply");
            JSONObject summary = reply.getJSONObject("summary");
            return summary.getString("summaryText");
        } catch (Exception e) {
            Log.e(TAG, "Error parsing the JSON response", e);
            return null;
        }
    }

    private static String convertStreamToString(InputStream is) {
        Scanner s = new Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    public interface ApiResponseCallback {
        void onResponseReceived(String response);
    }
}
