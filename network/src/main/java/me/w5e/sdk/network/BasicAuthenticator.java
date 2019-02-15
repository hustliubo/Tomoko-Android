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

package me.w5e.sdk.network;

import okhttp3.Credentials;
import okhttp3.Request;

/**
 * Created by w5e on 2018/7/26.
 */

public class BasicAuthenticator implements Authenticator {

    private String mUsername;
    private String mPassword;

    public BasicAuthenticator(String username, String password) {
        mUsername = username;
        mPassword = password;
    }

    @Override
    public void authorize(Request.Builder requestBuilder) {
        if (mUsername == null || mPassword == null) {
            return;
        }
        requestBuilder.addHeader(HttpClient.HEADER_AUTHORIZATION,
                Credentials.basic(mUsername, mPassword));
    }
}
