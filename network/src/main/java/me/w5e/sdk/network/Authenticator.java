package me.w5e.sdk.network;

import okhttp3.Request;

/**
 * Created by w5e on 2018/7/26.
 */
public interface Authenticator {
    void authorize(Request.Builder requestBuilder);
}
