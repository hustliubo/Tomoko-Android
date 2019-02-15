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

/**
 * 时间延迟类<br>
 * <pre>
 * e.g.
 *      Delayer delayer = new Delayer(10*1000); //创建一个10秒的延迟类
 *      delayer.start(); //开始计时
 *      long remainingDur = delayer.getRemainingDuration();
 *      if (remainingDur == 0) {
 *         //已经过了延迟时长
 *      }
 *      delayer.start(); //再次调用可以重新开始计时
 *      delayer.setInvalid(); //可以设置立即使延迟失效
 * </pre>
 * Created by w5e.
 */
public class Delayer {
    private long mDelayDuration;
    private long mLastTimestamp;
    private boolean mInvalid;

    public Delayer(long delayDuration) {
        mDelayDuration = delayDuration;
    }

    private long getTimestamp() {
        return System.currentTimeMillis();
    }

    public void start() {
        if (isStarted()) {
            return;
        }
        mLastTimestamp = getTimestamp();
    }

    public boolean isStarted() {
        return mLastTimestamp > 0;
    }

    public void setInvalid() {
        this.mInvalid = true;
    }

    /**
     * 获取剩余延迟时长（毫秒），为0则表示已经过了延迟时长。否返回还要等待的毫秒数
     */
    public long getRemainingDuration() {
        if (mInvalid || !isStarted()) {
            return -1;
        }
        long currentTimestamp = getTimestamp();

        long passedDuration = currentTimestamp - mLastTimestamp;
        if (passedDuration < mDelayDuration) {
            return mDelayDuration - passedDuration;
        } else {
            mLastTimestamp = currentTimestamp - (passedDuration % mDelayDuration);
            return 0;
        }
    }

    public long getDelayDuration() {
        return mDelayDuration;
    }

    public void setDelayDuration(long delayDuration) {
        mDelayDuration = delayDuration;
    }

    public boolean isInvalid() {
        return mInvalid;
    }

}
