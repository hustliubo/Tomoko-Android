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

package ai.bind.iot.client.control.update;

import android.content.Context;

import androidx.annotation.NonNull;

import ai.bind.iot.client.common.Constants;
import ai.bind.iot.client.common.util.AppManager;
import ai.bind.iot.client.common.util.AsyncAutoRetry;
import ai.bind.iot.cloud.RemoteService;
import ai.bind.iot.xinge.XinGeManager;

/**
 * 推送实现：使用腾讯移动推送
 * Created by w5e.
 */
public abstract class XinGePush extends AsyncAutoRetry.AsyncJob implements Pushable {
    private XinGeManager mXinGeManager;
    private RemoteService mRemoteService;

    protected XinGePush(@NonNull final Context context) {
        final long versionCode = AppManager.getVersionCode(context, Constants.CLIENT_PACKAGE_NAME);
        XinGeManager.Listener xinGeListener = new XinGeManager.Listener() {
            @Override
            public void onStarted(String pushToken) {
                XinGePush.this.onResult(true);
                XinGePush.this.onStart(pushToken);
                if (mRemoteService == null) {
                    mRemoteService = new RemoteService();
                }
                mRemoteService.bindPushDevice(context, pushToken, versionCode);
            }

            @Override
            public void onStartFailed() {
                XinGePush.this.onResult(false);
            }

            @Override
            public void onMessage(String msg) {
                XinGePush.this.onMessage(msg);
            }
        };
        mXinGeManager = new XinGeManager(context, xinGeListener);
    }

    @Override
    public void start() {
        mAsyncAutoRetry.start();
    }

    @Override
    public void stop() {
        mAsyncAutoRetry.stop();
        mXinGeManager.stop();
        if (mRemoteService != null) {
            mRemoteService.release();
        }
    }

    @Override
    protected void executeAsync() {
        mXinGeManager.start();
    }

    private static final int RETRY_DELAY = 3000;
    private AsyncAutoRetry mAsyncAutoRetry = new AsyncAutoRetry(
            0, RETRY_DELAY, this) {
        @Override
        protected void onFinish(boolean isSuccessful) {
        }
    };
}
