package com.aspegrenide.chat_glass;

import  android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class HttpRequestTask extends AsyncTask<Void, Void, String> {

    private final String apiKey;
    private String imgPath;
    private final HttpRequestListener listener;

    public HttpRequestTask(String apiKey, String imgPath, HttpRequestListener listener) {
        this.apiKey = apiKey;
        this.imgPath = imgPath;
        this.listener = listener;
    }


    @Override
    protected String doInBackground(Void... voids) {
        try {
            // API endpoint URL
            //String apiUrl = "https://api.openai.com/v1/chat/completions";
            String apiUrl = "https://predict.app.landing.ai/inference/v1/predict?endpoint_id=23143f74-008b-4a8f-a038-da6044fd5320";

            // Create URL object
            URL url = new URL(apiUrl);

            // Open a connection to the URL
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Set the request method to POST
            connection.setRequestMethod("POST");

            // Set request headers
            connection.setRequestProperty("apiKey", apiKey);

            // Set content type to multipart/form-data
            String boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW";
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            // Enable input/output streams
            connection.setDoOutput(true);
            connection.setDoInput(true);

// Create a boundary string for the multipart request
            DataOutputStream os = new DataOutputStream(connection.getOutputStream());
            String lineEnd = "\r\n";
            String twoHyphens = "--";

            // Append the image file data to the request
            os.writeBytes(twoHyphens + boundary + lineEnd);
            os.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"" + imgPath + "\"" + lineEnd);
            os.writeBytes("Content-Type: image/jpeg" + lineEnd);
            os.writeBytes(lineEnd);

            // Read and write the image file bytes
            FileInputStream fileInputStream = new FileInputStream(imgPath);
            int bytesRead;
            byte[] buffer = new byte[1024];
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            fileInputStream.close();

            os.writeBytes(lineEnd);
            os.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

            // Get the response code
            int responseCode = connection.getResponseCode();

            // Read the response
            StringBuilder response = new StringBuilder();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
            } else {
                Log.e("HTTP Error", "Response Code: " + responseCode);
            }

            connection.disconnect();

            return response.toString();
        } catch (Exception e) {
            Log.e("HTTP Error", "Exception: " + e.getMessage());
            return null;
        }
    }

    @Override
    protected void onPostExecute(String result) {
        if (result != null) {
            listener.onSuccess(result);
        } else {
            listener.onError("Request failed.");
        }
    }

    public interface HttpRequestListener {
        void onSuccess(String response);
        void onError(String error);
    }
}
