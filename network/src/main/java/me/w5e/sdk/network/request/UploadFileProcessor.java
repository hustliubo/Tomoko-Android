package me.w5e.sdk.network.request;

import android.util.Log;

import java.io.File;

import androidx.annotation.NonNull;
import me.w5e.sdk.network.HttpClient;
import me.w5e.sdk.network.progress.ProgressListener;
import me.w5e.sdk.network.progress.ProgressRequestBody;
import okhttp3.RequestBody;

/**
 * Created by w5e on 2018/7/31.
 */
public abstract class UploadFileProcessor<T> extends HttpClient.RequestProcessor<T> {
    protected File file;
    private ProgressListener l;

    public UploadFileProcessor(@NonNull HttpClient httpClient,
                               @NonNull File file, ProgressListener progressListener) {
        super(httpClient);
        this.file = file;
        this.l = progressListener;
    }

    @NonNull
    @Override
    protected final RequestBody getRequestBody() {
        RequestBody requestBody = new ProgressRequestBody(
                RequestBody.create(HttpClient.CONTENT_TYPE_DEFAULT, file), l);
        if (HttpClient.DEBUG) Log.d(HttpClient.TAG, "Upload: " + file);
        return requestBody;
    }
}
