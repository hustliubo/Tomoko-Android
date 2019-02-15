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

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.app.job.JobWorkItem;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by w5e.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class JobServiceManager extends JobScheduler {
    private JobScheduler mRealJobScheduler;
    private Context mContext;

    public JobServiceManager(@NonNull Context context) {
        mContext = context.getApplicationContext();
    }

    private JobScheduler getJobScheduler() {
        if (mRealJobScheduler == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mRealJobScheduler = mContext.getSystemService(JobScheduler.class);
            } else {
                mRealJobScheduler
                        = (JobScheduler) mContext.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            }
        }
        return mRealJobScheduler;
    }

    @Override
    public int schedule(@NonNull JobInfo job) {
        if (getJobScheduler() != null) {
            return mRealJobScheduler.schedule(job);
        }
        return RESULT_FAILURE;
    }

    public boolean scheduleJob(@NonNull JobInfo jobInfo) {
        return schedule(jobInfo) == RESULT_SUCCESS;
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Override
    public int enqueue(@NonNull JobInfo job, @NonNull JobWorkItem work) {
        if (getJobScheduler() != null) {
            return mRealJobScheduler.enqueue(job, work);
        }
        return RESULT_FAILURE;
    }

    @Override
    public void cancel(int jobId) {
        if (getJobScheduler() != null) {
            mRealJobScheduler.cancel(jobId);
        }
    }

    @Override
    public void cancelAll() {
        if (getJobScheduler() != null) {
            mRealJobScheduler.cancelAll();
        }
    }

    @NonNull
    @Override
    public List<JobInfo> getAllPendingJobs() {
        if (getJobScheduler() != null) {
            return mRealJobScheduler.getAllPendingJobs();
        }
        return new ArrayList<>();
    }

    @RequiresApi(Build.VERSION_CODES.N)
    @Nullable
    @Override
    public JobInfo getPendingJob(int jobId) {
        if (getJobScheduler() != null) {
            return mRealJobScheduler.getPendingJob(jobId);
        }
        return null;
    }
}
