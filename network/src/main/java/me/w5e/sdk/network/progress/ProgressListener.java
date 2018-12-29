package me.w5e.sdk.network.progress;

/**
 * Created by w5e on 2018/7/20.
 */
public interface ProgressListener {
    void onProgress(long writtenLength, long totalLength, int progress);
}
