package com.aspegrenide.chat_glass;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class AskDocUtil {

    private static final String TAG = "AskDocUtil";
    private static final String ENDPOINT_URL = "https://discoveryengine.googleapis.com/v1alpha/projects/546272475523/locations/global/collections/default_collection/dataStores/aie-ac-manuals-ds_1696848157153/conversations/-:converse";

    public interface AskDocCallback {
        void onResult(String response);
        void onError(Exception e);
    }

    public static void AskDoc(String apiKey, String question, AskDocCallback callback) {
        new AskDocTask(apiKey, question, callback).execute();
    }

    private static class AskDocTask extends AsyncTask<Void, Void, String> {

        private String apiKey;
        private String question;
        private AskDocCallback callback;
        private Exception error = null;
        private String API_KEY = "AIzaSyAOMUd3VqZdIH4NSHLlgAN203fbP1K8xGg";

        AskDocTask(String apiKey, String question, AskDocCallback callback) {
            this.apiKey = apiKey;
            this.question = question;
            this.callback = callback;
        }

        @Override
        protected String doInBackground(Void... voids) {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(ENDPOINT_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);

                String jsonPayload = "{\"query\":{\"input\":\"" + question + "\"}}";

                try (OutputStream os = new BufferedOutputStream(conn.getOutputStream())) {
                    byte[] input = jsonPayload.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                InputStream in = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                return result.toString();

            } catch (Exception e) {
                error = e;
                return null;
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (error != null) {
                callback.onError(error);
            } else {
                callback.onResult(result);
            }
        }
    }
}
