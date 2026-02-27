package org.cpicpgx.util;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.cpicpgx.exception.NotFoundException;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HttpUtils {
  private static final String sf_clinpgxUrl = "https://api.clinpgx.org/v1/";
  public static final int API_WAIT_TIME = 2100;

  @Nullable
  public static String apiRequest(OkHttpClient client, String url) throws IOException, NotFoundException {
    return apiRequest(client, url, null);
  }

  /**
   * Make an API HTTP request with optional headers
   * @param client an OkHttp client
   * @param url the full URL to request
   * @param headers HTTP headers to include in request
   * @return the response data as a String
   */
  @Nullable
  public static String apiRequest(OkHttpClient client, String url, Map<String,String> headers) throws IOException, NotFoundException {
    Request.Builder builder = new Request.Builder().url(url).method("GET", null);
    if (headers != null) {
      for (String headerName : headers.keySet()) {
        builder.addHeader(headerName, headers.get(headerName));
      }
    }
    Request request = builder.build();

    try (Response response = client.newCall(request).execute()) {

      if (!response.isSuccessful()) {
        if (response.code() == 404) {
          throw new NotFoundException("No response for " + url);
        } else {
          throw new IOException("Unexpected response " + response);
        }
      }
      if (response.body() == null) {
        return null;
      }
      return response.body().string();
    }
  }

  /**
   * Builds an API request URL for the ClinPGx API
   * @param path the path of the API URL after the /v1/ (no beginning slash necessary)
   * @param queryParams optional pairs of name-value query parameter strings (values will be properly escaped)
   * @return a full HTTP URL
   */
  public static String buildClinpgxUrl(String path, String... queryParams) {
    if (queryParams != null && queryParams.length % 2 != 0) {
      throw new RuntimeException("List of query parameter key-value pairs must be divisible by 2");
    }
    if (path.startsWith("/")) {
      throw new RuntimeException("Path should not start with slash: " + path);
    }
    String url = sf_clinpgxUrl.concat(path);
    if (queryParams != null && queryParams.length > 0) {
      List<String> params = new ArrayList<>();
      for (int i = 0; i < queryParams.length / 2; i++) {
        params.add(queryParams[i*2] + "=" + queryParams[(i*2)+1]);
      }
      url = url.concat("?").concat(String.join("&", params));
    }
    return url;
  }
}
