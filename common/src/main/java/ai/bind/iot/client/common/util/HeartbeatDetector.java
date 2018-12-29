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

package ai.bind.iot.client.common.util;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

/**
 * 用于检测活跃状态
 * <pre>
 * 使用说明：
 *    1.创建实例
 *    2.开始检测{@link #start()}
 *    3.处理死亡事件{@link #die(HeartbeatDetector)}
 *    4.停止检测{@link #stop()}
 * </pre>
 * Created by w5e.
 */
public abstract class HeartbeatDetector {
    private static final boolean DEBUG = false;
    private static final String TAG = HeartbeatDetector.class.getSimpleName();
    private int beatCount = -1;
    private long maxBeatInterval;
    private Handler detectHandler;
    private boolean stopOnDie;

    protected HeartbeatDetector(long maxBeatInterval) {
        this(maxBeatInterval, true);
    }

    /**
     * @param maxBeatInterval 最大心跳时间间隔，如果在时间间隔内未心跳则会执行死亡方法
     * @param stopOnDie 是否在死亡时停止检测
     */
    protected HeartbeatDetector(long maxBeatInterval, boolean stopOnDie) {
        if (maxBeatInterval <= 0) maxBeatInterval = 1000;
        this.maxBeatInterval = maxBeatInterval;
        this.stopOnDie = stopOnDie;
        log("init:" + maxBeatInterval);
    }

    private Runnable detectCallback = new Runnable() {
        @Override
        public void run() {
            if (beatCount <= 0) {
                if (beatCount == 0)
                    die(HeartbeatDetector.this);
                log("die");
                if (stopOnDie) stop();
                else detect();
            } else
                detect();
        }
    };

    public void beat() {
        beatCount += 1;
        log("beat:" + beatCount);
    }

    protected abstract void die(HeartbeatDetector detector);

    public void start() {
        if (detectHandler != null) return;
        detectHandler = new Handler(Looper.getMainLooper());
        log("start");
        detect();
    }

    private void detect() {
        if (detectHandler == null) return;
        beatCount = 0;
        detectHandler.postDelayed(detectCallback, maxBeatInterval);
        log("detect");
    }

    public void stop() {
        if (detectHandler == null) return;
        detectHandler.removeCallbacks(detectCallback);
        detectHandler = null;
        beatCount = -1;
        log("stop");
    }

    private void log(@NonNull String msg) {
        if (DEBUG) Log.d(TAG, msg);
    }
}
