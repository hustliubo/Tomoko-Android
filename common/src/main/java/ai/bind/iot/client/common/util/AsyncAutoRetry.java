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
 * 异步自动重试（WAsyncTimer适合于做固定间隔执行）
 * <pre>
 *     使用说明：
 *     1. 创建实例
 *     2. 开始执行{@link #start()}，会调用{@link AsyncJob#executeAsync()}
 *     3. 结束重试{@link #stop()}，如果有延时执行的任务会被移除
 *     4. 重置{@link #reset()}， 只在当前重试结束时执行，重试后可以重新执行{@link #start()}
 * </pre>
 * Created by w5e.
 */
public abstract class AsyncAutoRetry {
    private static final boolean DEBUG = false;
    private static final String TAG = "AsyncAutoRetry";

    private final int mNumber;
    private final long mDelayMillis;
    private final AsyncJob mAsyncJob;

    /**
     * @param number      重试次数，如果值不大于0则无限次重试
     * @param delayMillis 重试延迟执行时间（毫秒）
     * @param asyncJob    重试实例
     */
    protected AsyncAutoRetry(int number, long delayMillis, @NonNull AsyncJob asyncJob) {
        mNumber = number;
        mDelayMillis = delayMillis;
        mAsyncJob = asyncJob;
        log("Initialized: RetryNo:" + number + ", Delay:" + delayMillis);
    }

    public final void start() {
        if (isRunning || isFinished) return;
        isRunning = true;
        log("Start");
        execute();
    }

    public final void stop() {
        if (isFinished || !isRunning) return;
        isFinished = true;
        log("Stop");
        if (delayHandler != null && retryRunnable != null)
            delayHandler.removeCallbacks(retryRunnable);
    }

    private boolean isFinished;
    private boolean isRunning;
    private int currentTimes;

    public final boolean reset() {
        if (!isFinished) return false;
        log("Reset");
        isFinished = false;
        isRunning = false;
        currentTimes = 0;
        return true;
    }

    protected abstract void onFinish(boolean isSuccessful);

    private AsyncJobResultListener asyncJobResultListener = new AsyncJobResultListener() {
        @Override
        public void onResult(boolean isSuccessful) {
            if (isFinished) return;
            try {
                if (isSuccessful || (mNumber > 0 && currentTimes > mNumber)) {
                    log("Finished:" + isSuccessful);
                    isFinished = true;
                    AsyncAutoRetry.this.onFinish(isSuccessful);
                } else
                    retry();
            } catch (Exception ignored) {
            }
        }
    };

    private void execute() {
        if (isFinished) return;
        currentTimes += 1;
        mAsyncJob.execute(asyncJobResultListener);
    }

    private Handler delayHandler;
    private Runnable retryRunnable;

    private void retry() {
        log("Retry:" + currentTimes);
        if (mDelayMillis > 0) {
            if (delayHandler == null) delayHandler = new Handler(Looper.getMainLooper());
            if (retryRunnable == null) retryRunnable = new Runnable() {
                @Override
                public void run() {
                    execute();
                }
            };
            delayHandler.postDelayed(retryRunnable, mDelayMillis);
        } else {//不需要延迟执行
            execute();
        }
    }

    public static abstract class AsyncJob {
        AsyncJobResultListener mAsyncJobResultListener;

        private void execute(@NonNull AsyncJobResultListener asyncJobResultListener) {
            mAsyncJobResultListener = asyncJobResultListener;
            executeAsync();
        }

        /**
         * 异步执行完成后必须调用{@link #onResult(boolean)}方法
         */
        protected abstract void executeAsync();

        /**
         * @param isSuccessful true 执行执行成功，否则为失败
         */
        protected void onResult(boolean isSuccessful) {
            mAsyncJobResultListener.onResult(isSuccessful);
        }
    }

    private interface AsyncJobResultListener {
        void onResult(boolean isSuccessful);
    }

    private void log(@NonNull String msg) {
        if (DEBUG) Log.d(TAG, msg);
    }
}
