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

package ai.bind.iot.xinge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.tencent.android.tpush.XGIOperateCallback;
import com.tencent.android.tpush.XGPushBaseReceiver;
import com.tencent.android.tpush.XGPushClickedResult;
import com.tencent.android.tpush.XGPushConfig;
import com.tencent.android.tpush.XGPushManager;
import com.tencent.android.tpush.XGPushRegisterResult;
import com.tencent.android.tpush.XGPushShowedResult;
import com.tencent.android.tpush.XGPushTextMessage;


/**
 * Created by w5e on 2018/10/8.
 */
public final class XinGeManager {
    private static final boolean DEBUG = true;
    private static final String TAG = XinGeManager.class.getSimpleName();

    private Context mContext;
    private Listener mListener;

    public XinGeManager(@NonNull Context context, @NonNull Listener listener) {
        mContext = context.getApplicationContext();
        mListener = listener;
        XGPushConfig.enableDebug(mContext, DEBUG);
        XGPushConfig.setReportNotificationStatusEnable(mContext, false);
    }

    public void start() {
        XGPushManager.registerPush(mContext, mCallback);
        if (DEBUG) {
            if (mDebugMsgReceiver == null) {
                mDebugMsgReceiver = new DebugMsgReceiver();
                mContext.registerReceiver(mDebugMsgReceiver, new IntentFilter(ACTION_PUSH_DEBUG));
            }
        }
    }

    public void stop() {
        try {
            XGPushManager.unregisterPush(mContext);

            if (DEBUG) {
                mContext.unregisterReceiver(mDebugMsgReceiver);
                mDebugMsgReceiver = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private XGIOperateCallback mCallback = new XGIOperateCallback() {
        @Override
        public void onSuccess(Object obj, int var2) {
            mListener.onStarted(obj.toString());
        }

        @Override
        public void onFail(Object var1, int var2, String var3) {
            mListener.onStartFailed();
        }
    };

    public static final class PushReceiver extends XGPushBaseReceiver {

        @Override
        public void onRegisterResult(
                Context context, int var2, XGPushRegisterResult xgPushRegisterResult) {
        }

        @Override
        public void onUnregisterResult(Context context, int var2) {
        }

        @Override
        public void onSetTagResult(Context context, int var2, String var3) {
        }

        @Override
        public void onDeleteTagResult(Context context, int var2, String var3) {
        }

        @Override
        public void onTextMessage(Context context, XGPushTextMessage xgPushTextMessage) {
            if (xgPushTextMessage == null) {
                return;
            }

            if (TextUtils.isEmpty(xgPushTextMessage.getContent())) {
                return;
            }

            if (DEBUG) {
                Log.d(TAG, xgPushTextMessage.toString());
            }

            Intent intent = new Intent(ACTION_PUSH_DEBUG);
            intent.putExtra(EXTRA_MSG, xgPushTextMessage.getContent());
            context.sendBroadcast(intent);
        }

        @Override
        public void onNotifactionClickedResult(
                Context context, XGPushClickedResult xgPushClickedResult) {
        }

        @Override
        public void onNotifactionShowedResult(
                Context context, XGPushShowedResult xgPushShowedResult) {
        }
    }

    public interface Listener {
        void onStarted(String pushToken);

        void onStartFailed();

        void onMessage(String msg);
    }

    private static final String ACTION_PUSH_DEBUG = "ai.bind.client.control.action.PUSH_DEBUG";
    private static final String EXTRA_MSG = "EXTRA_MSG";
    private DebugMsgReceiver mDebugMsgReceiver;

    public static void testOnMsg(@NonNull Context context, @NonNull String msg) {
        Intent intent = new Intent(ACTION_PUSH_DEBUG);
        intent.putExtra(EXTRA_MSG, msg);
        context.sendBroadcast(intent);
    }

    public class DebugMsgReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null || !action.equals(ACTION_PUSH_DEBUG)) {
                return;
            }
            mListener.onMessage(intent.getStringExtra(EXTRA_MSG));
        }
    }
}
