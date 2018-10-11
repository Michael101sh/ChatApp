/* Michael Shachar
   Ido Ben El
   Michal Bar Ilan
   Betzalel Moshkovitz */

package ap2pr.chatapp;

import org.apache.http.impl.client.DefaultHttpClient;

public class HttpClientHolder {
    private static DefaultHttpClient userHttpClient;

    public static void setHttpClient(DefaultHttpClient httpClient) {
        userHttpClient = httpClient;
    }
    public static DefaultHttpClient getHttpClient() {
        return userHttpClient;
    }
}
