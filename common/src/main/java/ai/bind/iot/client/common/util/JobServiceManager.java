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

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * Created by w5e.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class JobServiceManager extends JobScheduler{
    private JobScheduler realJobScheduler;
    private Context context;

    public JobServiceManager(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    private JobScheduler getJobScheduler() {
        if (realJobScheduler == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                realJobScheduler = context.getSystemService(JobScheduler.class);
            } else {
                realJobScheduler = (JobScheduler) context.getSystemService(Context.
                        JOB_SCHEDULER_SERVICE);
            }
        }
        return realJobScheduler;
    }

    @Override
    public int schedule(@NonNull JobInfo job) {
        if (getJobScheduler() != null) return realJobScheduler.schedule(job);
        return RESULT_FAILURE;
    }

    public boolean scheduleJob(@NonNull JobInfo jobInfo) {
        return schedule(jobInfo) == RESULT_SUCCESS;
    }

    @RequiresApi(26)
    @Override
    public int enqueue(@NonNull JobInfo job, @NonNull JobWorkItem work) {
        if (getJobScheduler() != null) return realJobScheduler.enqueue(job, work);
        return RESULT_FAILURE;
    }

    @Override
    public void cancel(int jobId) {
        if (getJobScheduler() != null) realJobScheduler.cancel(jobId);
    }

    @Override
    public void cancelAll() {
        if (getJobScheduler() != null) realJobScheduler.cancelAll();
    }

    @NonNull
    @Override
    public List<JobInfo> getAllPendingJobs() {
        if (getJobScheduler() != null) return realJobScheduler.getAllPendingJobs();
        return new ArrayList<>();
    }

    @RequiresApi(24)
    @Nullable
    @Override
    public JobInfo getPendingJob(int jobId) {
        if (getJobScheduler() != null) return realJobScheduler.getPendingJob(jobId);
        return null;
    }
}
