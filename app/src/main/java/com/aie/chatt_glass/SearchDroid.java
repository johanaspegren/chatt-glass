package com.aie.chatt_glass;

import android.os.AsyncTask;
import android.util.Log;
import com.google.cloud.discoveryengine.v1.*;

import java.io.IOException;

public class SearchDroid {

    private String projectId = "gc-doc-chat";
    private String location = "global";
    private String collectionId = "default_collection";
    private String dataStoreId = "gc-doc-chat-ds_1696838692806";
    private String servingConfigId = "default_search";

    private String TAG = "askdoc";
    // Using AsyncTask for background processing
    public void search(final String searchQuery) {
        new AsyncTask<Void, Void, SearchResponse>() {
            @Override
            protected SearchResponse doInBackground(Void... voids) {
                try {
                    Log.d(TAG, "init");
                    return performSearch(projectId, location, collectionId, dataStoreId, servingConfigId, searchQuery);
                } catch (IOException e) {
                    Log.e("SearchError", "Failed to perform search", e);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(SearchResponse response) {
                if (response != null) {
                    for (SearchResponse.SearchResult element : response.getResultsList()) {
                        Log.d("SearchResult", "Response content: " + element);
                    }
                }
            }
        }.execute();
    }

    private SearchResponse performSearch(
            String projectId,
            String location,
            String collectionId,
            String dataStoreId,
            String servingConfigId,
            String searchQuery) throws IOException {

        Log.d(TAG, "perform search: " + searchQuery);
        String endpoint = (location.equals("global"))
                ? String.format("discoveryengine.googleapis.com:443", location)
                : String.format("%s-discoveryengine.googleapis.com:443", location);

        SearchServiceSettings settings =
                SearchServiceSettings.newBuilder().setEndpoint(endpoint).build();

        try (SearchServiceClient searchServiceClient = SearchServiceClient.create(settings)) {
            SearchRequest request =
                    SearchRequest.newBuilder()
                            .setServingConfig(
                                    ServingConfigName.formatProjectLocationCollectionDataStoreServingConfigName(
                                            projectId, location, collectionId, dataStoreId, servingConfigId))
                            .setQuery(searchQuery)
                            .setPageSize(10)
                            .build();

            return searchServiceClient.search(request).getPage().getResponse();
        }
    }
}
