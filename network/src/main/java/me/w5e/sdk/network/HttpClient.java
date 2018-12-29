package me.w5e.sdk.network;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.SocketException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringDef;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Route;


/**
 * HTTP客户端
 * Created by w5e on 2018/7/8.
 */
public abstract class HttpClient {
    public static final boolean DEBUG = false;
    public static final String TAG = HttpClient.class.getSimpleName();

    public static final String HEADER_DESTINATION = "Destination";
    public static final String HEADER_OVERWRITE = "Overwrite";
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_VALUE_OVERWRITE_TRUE = "T";
    public static final String HEADER_VALUE_OVERWRITE_FALSE = "F";

    public static final String METHOD_OPTIONS = "OPTIONS";
    public static final String METHOD_GET = "GET";
    public static final String METHOD_POST = "POST";
    public static final String METHOD_PUT = "PUT";
    public static final String METHOD_DELETE = "DELETE";
    public static final String METHOD_MOVE = "MOVE";
    public static final String METHOD_COPY = "COPY";
    public static final String METHOD_MKCOL = "MKCOL";
    public static final String METHOD_PROPFIND = "PROPFIND";

    public static final MediaType CONTENT_TYPE_DEFAULT = MediaType.parse("application/x-www-form-urlencoded");

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            METHOD_OPTIONS, METHOD_GET, METHOD_POST, METHOD_PUT, METHOD_PROPFIND,
            METHOD_DELETE, METHOD_MOVE, METHOD_COPY, METHOD_MKCOL
    })
    @interface METHOD {
    }

    private OkHttpClient mHttpClient;
    private Authenticator mAuthenticator;
    private Handler handler;

    public final OkHttpClient getOkHttpClient() {
        if (mHttpClient == null) {
            OkHttpClient.Builder builder = buildOkHttpClient().authenticator(new okhttp3.Authenticator() {
                @Nullable
                @Override
                public Request authenticate(@NonNull Route route, @NonNull Response response) {
                    if (DEBUG) Log.w(TAG, "Needs Authentication : " + response);
                    HttpClient.this.authenticate();
                    return null;
                }
            });
            mHttpClient = builder.build();
        }
        return mHttpClient;
    }

    @NonNull
    protected OkHttpClient.Builder buildOkHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(7, TimeUnit.SECONDS)
                .readTimeout(7, TimeUnit.SECONDS)
                .writeTimeout(7, TimeUnit.SECONDS);
    }

    /**
     * 当请求需要认证时(未设置{@link Authenticator} 或认证失败)会执行这个方法来通知用户执行认证操作
     */
    public abstract void authenticate();

    public Authenticator getAuthenticator() {
        return mAuthenticator;
    }

    public void setAuthenticator(Authenticator authenticator) {
        mAuthenticator = authenticator;
    }

    private Handler getHandler() {
        if (handler == null)
            handler = new Handler(Looper.getMainLooper());
        return handler;
    }
    //==============================================================================================

    public static abstract class RequestProcessor<T> {
        protected HttpClient mHttpClient;
        private Call mCall;
        private Response mResponse;


        public RequestProcessor(@NonNull HttpClient httpClient) {
            mHttpClient = httpClient;
        }

        private Request getRequest() {
            Request.Builder builder = buildRequest();
            if (mHttpClient.getAuthenticator() != null)
                mHttpClient.getAuthenticator().authorize(builder);
            Request request = builder.build();
            if (DEBUG && request.headers().size() > 0)
                Log.d(TAG, "Header: " + request.headers());
            return request;
        }

        public Result execute() throws IOException {
            return parseResult(mHttpClient.getOkHttpClient().newCall(getRequest()).execute());
        }

        public Result execute(@NonNull Request request) throws IOException {
            mCall = mHttpClient.getOkHttpClient().newCall(request);
            mResponse = mCall.execute();
            return parseResult(mResponse);
        }

        public void executeAsync(@NonNull final ResultListener<T> l) {
            executeAsync(getRequest(), l);
        }

        public void executeAsync(@NonNull Request request, @NonNull final ResultListener<T> l) {
            mCall = mHttpClient.getOkHttpClient().newCall(request);
            mCall.enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @Nullable IOException e) {
                    if (!(e instanceof SocketException) && !call.isCanceled()) {
                        if (DEBUG)
                            Log.e(TAG, "Failed: " + e + "; " + call.request());
                    }
                    processResult(new Result<T>(e), l);
                }

                @Override
                public void onResponse(@NonNull Call call, @Nullable Response response) {
                    try {
                        if (DEBUG && response != null)
                            Log.d(TAG, response.toString());
                        mResponse = response;
                        processResult(parseResult(response), l);
                    } catch (Exception e) {
                        e.printStackTrace();
                        processResult(new Result<T>(e), l);
                    }
                    mResponse = null;
                }
            });
        }

        private void processResult(final Result<T> result, @NonNull final ResultListener<T> l) {
            mHttpClient.getHandler().post(new Runnable() {
                @Override
                public void run() {
                    if (result.isSuccessful())
                        l.onSuccess(result.getValue());
                    else
                        l.onFailure(result.getErrorMsg());
                }
            });
        }

        public void cancel() {
            //Cancel Request
            if (mCall != null && !mCall.isCanceled()) {
                if (DEBUG) Log.d(TAG, "Cancel: " + mCall.request());
                mCall.cancel();
            }
            //Cancel Response Processing
            if (mResponse != null) {
                if (DEBUG) Log.d(TAG, "Cancel: " + mResponse);
                try {
                    mResponse.close();
                } catch (Exception ignored) {
                }
                mResponse = null;
            }
        }

        public boolean isCanceled() {
            return mCall != null && mCall.isCanceled();
        }

        /**
         * @return 请求方法
         */
        @NonNull
        @METHOD
        protected abstract String getMethod();

        /**
         * @return 请求路径
         */
        @NonNull
        protected abstract String getUrl();

        /**
         * @return 请求头
         */
        protected abstract HashMap<String, String> getHeaders();

        /**
         * @return 请求参数
         */
        protected abstract HashMap<String, String> getParams();

        /**
         * @return requestBody
         */
        protected abstract RequestBody getRequestBody();

        private Request.Builder buildRequest() {
            String method = getMethod();
            String url = getUrl();
            HashMap<String, String> params = getParams();
            RequestBody requestBody = null;
            if (params != null) {
                StringBuilder paramsBuilder = new StringBuilder();
                boolean append = false;
                for (String key : params.keySet()) {
                    if (append) paramsBuilder.append("&");
                    else append = true;
                    paramsBuilder.append(key).append("=").append(params.get(key));
                }
                if (method.equals(METHOD_GET)) {
                    if (url.contains("?")) {
                        url += "&" + paramsBuilder;
                    } else {
                        url += "?" + paramsBuilder;
                    }
                } else {
                    requestBody = RequestBody.create(CONTENT_TYPE_DEFAULT, paramsBuilder.toString());
                }
            }

            if (requestBody == null) requestBody = getRequestBody();

            if (method.equals(METHOD_DELETE) && requestBody == null) {//?删除方法的请体不能为空
                requestBody = RequestBody.create(CONTENT_TYPE_DEFAULT, "");
            }

            if (DEBUG) {
                Log.d(TAG, "Method: "
                        + method + ", Url: " + url + (params == null ? "" : ", Params: " + params));
            }
            Request.Builder builder = new Request.Builder()
                    .url(url)
                    .method(method, requestBody);
            HashMap<String, String> headers = getHeaders();
            if (headers != null) {
                for (String key : headers.keySet()) {
                    String value = headers.get(key);
                    if (value != null) builder.addHeader(key, value);
                }
            }
            return builder;
        }

        /**
         * 解析响应结果
         *
         * @param response 响应对象
         * @return 返回解析后的结果
         */
        @NonNull
        protected abstract Result<T> parseResult(Response response);
    }

    public static class Result<T> {
        private boolean mIsSuccessful;
        private int mErrorCode;
        private String mErrorMsg = "Unknown Error.";//默认错误信息
        private T mValue;

        public Result(boolean isSuccessful) {
            mIsSuccessful = isSuccessful;
        }

        public Result(T value) {
            mValue = value;
            mIsSuccessful = true;
        }

        public Result(Exception e) {
            if (e != null) mErrorMsg = e.getLocalizedMessage();
        }

        public Result(int errorCode, String errorMsg) {
            mErrorCode = errorCode;
            mErrorMsg = errorMsg;
        }

        public boolean isSuccessful() {
            return mIsSuccessful;
        }

        public String getErrorMsg() {
            return mErrorMsg;
        }

        public T getValue() {
            return mValue;
        }
    }

    public interface ResultListener<T> {
        void onSuccess(T t);

        void onFailure(String msg);
    }

    public static String encodeUrl(String path) {
        String encodedPath = Uri.encode(path, "/");
        if (!encodedPath.startsWith("/"))
            encodedPath = "/" + encodedPath;
        return encodedPath;
    }

    // --- 2xx Success ---
    public static final int SC_OK = 200;
    public static final int SC_CREATED = 201;
    public static final int SC_ACCEPTED = 202;
    public static final int SC_NON_AUTHORITATIVE_INFORMATION = 203;
    public static final int SC_NO_CONTENT = 204;
    public static final int SC_RESET_CONTENT = 205;
    public static final int SC_PARTIAL_CONTENT = 206;
    public static final int SC_MULTI_STATUS = 207;
}
