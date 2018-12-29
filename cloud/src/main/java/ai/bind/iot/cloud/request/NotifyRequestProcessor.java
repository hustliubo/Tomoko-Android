/*
 * Copyright (c) 2018 CELLA
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

package ai.bind.iot.cloud.request;

import java.util.HashMap;

import androidx.annotation.NonNull;
import me.w5e.sdk.network.HttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 通知应用启动请求实现
 * Created by w5e.
 */
public final class NotifyRequestProcessor extends HttpClient.RequestProcessor<String> {
    private String mDeviceId;
    private String mVersionCode;

    public NotifyRequestProcessor(@NonNull HttpClient httpClient,
                                  @NonNull String deviceId, long versionCode) {
        super(httpClient);
        mDeviceId = deviceId;
        mVersionCode = String.valueOf(versionCode);
    }

    @NonNull
    @Override
    protected String getMethod() {
        return HttpClient.METHOD_POST;
    }

    @NonNull
    @Override
    protected String getUrl() {
        return "URL_NOTIFY_APP_START";//TODO:使用前修改为正确的服务器地址
    }

    @Override
    protected HashMap<String, String> getHeaders() {
        return null;
    }

    @Override
    protected HashMap<String, String> getParams() {
        HashMap<String, String> params = new HashMap<>();
        params.put("device_id", mDeviceId);
        params.put("version", mVersionCode);
        return params;
    }

    @Override
    protected RequestBody getRequestBody() {
        return null;
    }

    @NonNull
    @Override
    protected HttpClient.Result<String> parseResult(Response response) {
        return new HttpClient.Result<>(response.isSuccessful());
    }
}