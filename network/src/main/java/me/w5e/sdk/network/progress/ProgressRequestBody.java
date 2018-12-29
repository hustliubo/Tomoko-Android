package me.w5e.sdk.network.progress;

import android.util.Log;

import java.io.IOException;

import androidx.annotation.NonNull;
import me.w5e.sdk.network.HttpClient;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.Okio;

/**
 * Created by w5e on 2018/7/20.
 */
public class ProgressRequestBody extends RequestBody {
    private final RequestBody mRequestBody;
    private final ProgressListener l;


    public ProgressRequestBody(RequestBody requestBody, ProgressListener progressListener) {
        mRequestBody = requestBody;
        l = progressListener;
    }

    @Override
    public MediaType contentType() {
        return mRequestBody.contentType();
    }

    @Override
    public long contentLength() throws IOException {
        return mRequestBody.contentLength();
    }

    @Override
    public void writeTo(@NonNull BufferedSink sink) throws IOException {
        long contentLength = contentLength();
        if (HttpClient.DEBUG) Log.d(HttpClient.TAG, "Write Body: " + contentLength);
        if (l == null) {
            mRequestBody.writeTo(sink);
            return;
        }
        ProgressOutputStream os = new ProgressOutputStream(sink.outputStream(), contentLength, 0, l);
        BufferedSink progressSink = Okio.buffer(Okio.sink(os));
        mRequestBody.writeTo(progressSink);
        progressSink.flush();
    }
}
