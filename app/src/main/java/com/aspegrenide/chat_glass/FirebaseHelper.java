package com.aspegrenide.chat_glass;

import android.util.Log;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class FirebaseHelper {

    private static String TAG = "zzz";
    private static final String DATABASE_URL = "https://aie-glassistant-default-rtdb.europe-west1.firebasedatabase.app/.json";
    public static String fetchDataFromFirebase() {
        try {
            URL url = new URL(DATABASE_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                Log.d(TAG, "got data" + line);
                response.append(line);
            }
            reader.close();

            // Close the connection
            connection.disconnect();

            return response.toString();
        } catch (Exception e) {
            Log.e("FirebaseHelper", "Error fetching data from Firebase: " + e.getMessage());
            return null;
        }
    }

    public static boolean sendDataToFirebase(String newData) {
        try {
            URL url = new URL(DATABASE_URL); // Include the API key for authentication
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST"); // Use POST method to send data
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            // Write the data to the output stream
            DataOutputStream os = new DataOutputStream(connection.getOutputStream());
            os.writeBytes(newData);
            os.flush();
            os.close();

            int responseCode = connection.getResponseCode();

            // Check if the POST request was successful (HTTP status code 200)
            if (responseCode == HttpURLConnection.HTTP_OK) {
                Log.d("FirebaseHelper", "Data sent to Firebase successfully");
                return true;
            } else {
                Log.e("FirebaseHelper", "Failed to send data to Firebase. Response Code: " + responseCode);
                return false;
            }
        } catch (Exception e) {
            Log.e("FirebaseHelper", "Error sending data to Firebase: " + e.getMessage());
            return false;
        }
    }
}
