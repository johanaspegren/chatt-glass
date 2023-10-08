package com.aspegrenide.chat_glass;

import  android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class ChatRequestTask extends AsyncTask<Void, Void, String> {

    private final String apiKey;
    private final String model;
    private final List<ChatMessage> messages;
    private final ChatRequestListener listener;

    public ChatRequestTask(String apiKey, String model, List<ChatMessage> messages, ChatRequestListener listener) {
        this.apiKey = apiKey;
        this.model = model;
        this.messages = messages;
        this.listener = listener;
    }

    @Override
    protected String doInBackground(Void... voids) {
        try {
            // API endpoint URL
            String apiUrl = "https://api.openai.com/v1/chat/completions";

            // Create the JSON request body
            String jsonBody = "{\"model\":\"" + model + "\",\"messages\":" + ChatMessage.serialize(messages) + "}";

            // Create URL object
            URL url = new URL(apiUrl);

            // Open a connection to the URL
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Set the request method to POST
            connection.setRequestMethod("POST");

            // Set request headers
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);

            // Enable input/output streams
            connection.setDoOutput(true);
            connection.setDoInput(true);

            // Write the JSON body to the output stream
            DataOutputStream os = new DataOutputStream(connection.getOutputStream());
            os.writeBytes(jsonBody);
            os.flush();
            os.close();

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

    public interface ChatRequestListener {
        void onSuccess(String response);
        void onError(String error);
    }
}
