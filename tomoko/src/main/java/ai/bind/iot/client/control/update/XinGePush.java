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

import ai.bind.iot.client.common.util.AppManager;
import ai.bind.iot.client.common.util.AsyncAutoRetry;
import ai.bind.iot.cloud.RemoteService;
import ai.bind.iot.xinge.XGManager;
import androidx.annotation.NonNull;

import static ai.bind.iot.client.common.Constants.CLIENT_PACKAGE_NAME;

/**
 * 推送实现：使用腾讯移动推送
 * Created by w5e.
 */
public abstract class XinGePush extends AsyncAutoRetry.AsyncJob implements Pushable {
    private XGManager xgManager;
    private RemoteService remoteService;

    protected XinGePush(@NonNull Context context) {
        final long versionCode = AppManager.getVersionCode(context, CLIENT_PACKAGE_NAME);
        XGManager.Listener xinGeListener = new XGManager.Listener() {
            @Override
            public void onStarted(String pushToken) {
                XinGePush.this.onResult(true);
                XinGePush.this.onStart(pushToken);
                if (remoteService == null)
                    remoteService = new RemoteService();
                remoteService.bindPushDevice(pushToken, versionCode);
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
        xgManager = new XGManager(context, xinGeListener);
    }

    @Override
    public void start() {
        asyncAutoRetry.start();
    }

    @Override
    public void stop() {
        asyncAutoRetry.stop();
        xgManager.stop();
        if (remoteService != null)
            remoteService.release();
    }

    @Override
    protected void executeAsync() {
        xgManager.start();
    }

    private AsyncAutoRetry asyncAutoRetry = new AsyncAutoRetry(
            0, 3000, this) {
        @Override
        protected void onFinish(boolean isSuccessful) {
        }
    };
}
