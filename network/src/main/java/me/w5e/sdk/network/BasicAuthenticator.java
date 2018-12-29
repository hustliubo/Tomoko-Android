package me.w5e.sdk.network;

import okhttp3.Credentials;
import okhttp3.Request;

/**
 * Created by w5e on 2018/7/26.
 */
public class BasicAuthenticator implements Authenticator {
    private String username, password;

    public BasicAuthenticator(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public void authorize(Request.Builder requestBuilder) {
        if (username == null || password == null) return;
        requestBuilder.addHeader(HttpClient.HEADER_AUTHORIZATION,
                Credentials.basic(username, password));
    }
}
