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

package ai.bind.iot.client.control.page;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import ai.bind.iot.client.common.util.AppManager;
import ai.bind.iot.client.common.util.ClipboardUtils;
import ai.bind.iot.client.common.util.OSUtils;
import ai.bind.iot.client.control.Config;
import ai.bind.iot.client.control.ControlService;
import ai.bind.iot.client.control.R;
import androidx.annotation.NonNull;

import static ai.bind.iot.client.control.ControlService.ACTION_CONTROL;

public class HomeActivity extends Activity {
    private static final String TAG = ControlService.TAG;
    private TextView updateInfoTv;
    private TextView pushServiceStateInfoTv;
    private TextView pushTokenTv;
    private Config config;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        config = new Config(this);
        setupView();
        ControlService.start(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerEventReceiver(this);
        bindService();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterEventReceiver(this);
        unbindService();
    }

    @SuppressLint("SetTextI18n")
    private void setupView() {
        setContentView(R.layout.page__home);
        TextView versionTv = findViewById(R.id.versionTv);
        versionTv.setText(
                "v" + AppManager.getVersionName(this, getPackageName())
                        + "  " + OSUtils.getSerialNo() + "\nIP: " + OSUtils.getIpAddress(true));
        updateInfoTv = findViewById(R.id.updateInfoTv);
        pushServiceStateInfoTv = findViewById(R.id.pushServiceStateInfoTv);
        pushTokenTv = findViewById(R.id.pushTokenTv);
        Switch aliveSwitch = findViewById(R.id.aliveSwitch);
        aliveSwitch.setChecked(config.isKeepAlive());
        aliveSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (localBinder != null)
                    localBinder.setKeepAliveEnabled(isChecked);
            }
        });
        Switch rebootSwitch = findViewById(R.id.rebootSwitch);
        rebootSwitch.setChecked(config.isRebootEnabled());
        rebootSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (localBinder != null)
                    localBinder.setRebootEnabled(isChecked);
            }
        });
    }

    private int msgCount;
    private StringBuilder updateStringBuilder;

    private void updateView(int type, String data) {
        if (type == ControlService.EVENT_TYPE_PUSH_STATE) {
            pushServiceStateInfoTv.setText(data);
        } else if (type == ControlService.EVENT_TYPE_PUSH_TOKEN) {
            pushTokenTv.setText(data);
        } else if (type == ControlService.EVENT_TYPE_PUSH_MSG) {
            msgCount += 1;
            updateStringBuilder = new StringBuilder(getString(R.string.received_push))
                    .append("(").append(msgCount).append("):");
            updateStringBuilder.append(data).append("\n");
            updateInfoTv.setText(updateStringBuilder.toString());
        } else {
            if (updateStringBuilder == null) updateStringBuilder = new StringBuilder("TEST");
            updateStringBuilder.append("\n").append(data).append("\n");
            updateInfoTv.setText(updateStringBuilder.toString());
        }
    }

    public void copyPushToken(View view) {
        ClipboardUtils.setText(this, pushTokenTv.getText());
    }

    private ControlEventReceiver controlEventReceiver = new ControlEventReceiver();

    public void registerEventReceiver(@NonNull Context context) {
        if (controlEventReceiver == null) controlEventReceiver = new ControlEventReceiver();
        context.registerReceiver(controlEventReceiver, new IntentFilter(ACTION_CONTROL));
    }

    public void unregisterEventReceiver(@NonNull Context context) {
        if (controlEventReceiver == null) return;
        context.unregisterReceiver(controlEventReceiver);
        controlEventReceiver = null;
    }

    public class ControlEventReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;
            String action = intent.getAction();
            if (action == null || !action.equals(ACTION_CONTROL)) return;
            int type = intent.getIntExtra(ControlService.EXTRA_TYPE, ControlService.EVENT_TYPE_PUSH_STATE);
            String data = intent.getStringExtra(ControlService.EXTRA_DATA);
            Log.d(TAG, "onReceive:" + type + " " + data);
            updateView(type, data);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    private ControlService.LocalBinder localBinder;

    private void bindService() {
        try {
            Intent intent = new Intent(getApplicationContext(), ControlService.class);
            getApplicationContext().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void unbindService() {
        try {
            getApplicationContext().unbindService(mConnection);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            localBinder = (ControlService.LocalBinder) service;
            pushTokenTv.setText(localBinder.getPushToken());
        }

        public void onServiceDisconnected(ComponentName className) {
        }
    };

}
