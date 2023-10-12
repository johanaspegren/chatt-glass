package com.aie.chatt_glass;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;


// this works with the API key for today, seems it is movingg though


public class ApiTestOrg {
    public static void main(String[] args) {
        String apiKey = "API";
        String prompt = "What are the dimensions of the 6000 Power unit?";


        String response = makeRequest(apiKey, prompt);
        System.out.println(response);
    }


    public static String makeRequest(String apiKey, String prompt) {
        String responseString = "";


        try {
            URL url = new URL(
                    "https://discoveryengine.googleapis.com/v1alpha/projects/546272475523/locations/global/collections/default_collection/dataStores/aie-ac-manuals-ds_1696848157153/conversations/-:converse");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);
            conn.setDoInput(true);


            String jsonPayload = "{" +
                    "\"query\": {\"input\":\"" + prompt + "\"}," +
                    "\"summarySpec\": {\"summaryResultCount\": 5, \"ignoreAdversarialQuery\": true, \"includeCitations\": true}"
                    +
                    "}";


            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonPayload.getBytes("utf-8");
                os.write(input, 0, input.length);
            }


            InputStream in = new BufferedInputStream(conn.getInputStream());
            responseString = convertStreamToString(in);


        } catch (Exception e) {
            e.printStackTrace();
        }
        return responseString;
    }


    private static String convertStreamToString(InputStream is) {
        Scanner s = new Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";

    }
}