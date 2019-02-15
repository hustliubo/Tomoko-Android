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

package ai.bind.iot.client.common.wakeup;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import ai.bind.iot.client.common.Constants;

/**
 * Control进程保活实现
 * <pre>
 *     使用方法：
 *     #创建实例：{@link #ControlProcessAliveChecker(Context)}，
 *          推荐在{@link android.app.Activity#onCreate(Bundle)}方法中调用
 *     #结束使用：{@link #release()}
 *          推荐在{@link Activity#onDestroy()}方法中调用
 * </pre>
 * Created by w5e.
 */
public final class ControlProcessAliveChecker extends ProcessAliveChecker {

    public ControlProcessAliveChecker(@NonNull Context context) {
        super(context,
                ACTION_CLIENT_REQUEST,
                ACTION_CLIENT_RESPONSE,
                ACTION_CONTROL_REQUEST,
                ACTION_CONTROL_RESPONSE);
    }

    @Override
    public void wakeup() {
        Intent intent = new Intent();
        ComponentName componentName = new ComponentName(
                Constants.CONTROL_SERVICE_PACKAGE_NAME,
                Constants.CONTROL_SERVICE_CLASS_NAME);
        intent.setComponent(componentName);
        mContext.startService(intent);
    }

    @Override
    protected void onResponse(boolean isAlive) {
        if (DEBUG) {
            Log.d(TAG, "Control is alive:" + isAlive);
        }
    }
}
