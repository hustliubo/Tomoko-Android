/*
 * Copyright (c) 2019 CELLA
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.w5e.sdk.network.progress;

import android.util.Log;

import androidx.annotation.NonNull;

import me.w5e.sdk.network.HttpClient;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.Okio;

import java.io.IOException;

/**
 * Created by w5e on 2018/7/20.
 */

public class ProgressRequestBody extends RequestBody {
    private final RequestBody mRequestBody;
    private final ProgressListener mListener;

    public ProgressRequestBody(RequestBody requestBody, ProgressListener l) {
        mRequestBody = requestBody;
        mListener = l;
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
        if (HttpClient.DEBUG) {
            Log.d(HttpClient.TAG, "Write Body: " + contentLength);
        }
        if (mListener == null) {
            mRequestBody.writeTo(sink);
            return;
        }
        ProgressOutputStream os = new ProgressOutputStream(
                sink.outputStream(), contentLength, 0, mListener);
        BufferedSink progressSink = Okio.buffer(Okio.sink(os));
        mRequestBody.writeTo(progressSink);
        progressSink.flush();
    }
}
