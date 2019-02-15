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

package me.w5e.sdk.network.request;

import android.util.Log;

import androidx.annotation.NonNull;

import me.w5e.sdk.network.HttpClient;
import me.w5e.sdk.network.progress.ProgressListener;
import me.w5e.sdk.network.progress.ProgressRequestBody;
import okhttp3.RequestBody;

import java.io.File;

/**
 * Created by w5e on 2018/7/31.
 */

public abstract class UploadFileProcessor<T> extends HttpClient.RequestProcessor<T> {
    protected File mFile;
    private ProgressListener mListener;

    public UploadFileProcessor(@NonNull HttpClient httpClient,
                               @NonNull File file, ProgressListener progressListener) {
        super(httpClient);
        mFile = file;
        mListener = progressListener;
    }

    @NonNull
    @Override
    protected final RequestBody getRequestBody() {
        RequestBody requestBody = new ProgressRequestBody(
                RequestBody.create(HttpClient.CONTENT_TYPE_DEFAULT, mFile), mListener);
        if (HttpClient.DEBUG) {
            Log.d(HttpClient.TAG, "Upload: " + mFile);
        }
        return requestBody;
    }
}
