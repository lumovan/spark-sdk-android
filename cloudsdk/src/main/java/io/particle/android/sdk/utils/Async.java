package io.particle.android.sdk.utils;

import android.app.Activity;
import android.os.AsyncTask;

import java.io.IOException;

import io.particle.android.sdk.cloud.SparkCloud;
import io.particle.android.sdk.cloud.SparkCloudException;
import io.particle.android.sdk.cloud.SparkDevice;


/**
 * Analgesic AsyncTask wrapper for making Particle cloud API calls
 */
public class Async {

    private static final TLog log = TLog.get(Async.class);


    public abstract static class ApiWork<ApiCaller, Result> {

        public abstract Result callApi(ApiCaller apiCaller) throws SparkCloudException, IOException;

        public abstract void onSuccess(Result result);

        public abstract void onFailure(SparkCloudException exception);

        /**
         * Called at the end of the async task execution, before
         * onSuccess(), onFailure(), or onCancel()
         */
        public void onTaskFinished() {
            // default: no-op
        }

        public void onCancelled() {
            // default: no-op
        }
    }


    /**
     * For when you don't care about the return value (or there isn't one), you just want to make
     * the REST call
     */
    public abstract static class ApiProcedure<ApiCaller> extends ApiWork<ApiCaller, Void> {

        @Override
        public void onSuccess(Void voyd) {
            // no-op, because that's the whole point of this class.
        }
    }


    public static <T> AsyncApiWorker<SparkCloud, T> executeAsync(SparkCloud sparkCloud,
                                                                 ApiWork<SparkCloud, T> work) {
        return (AsyncApiWorker<SparkCloud, T>) new AsyncApiWorker<>(sparkCloud, work)
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }


    public static <T> AsyncApiWorker<SparkDevice, T> executeAsync(SparkDevice sparkDevice,
                                                                  ApiWork<SparkDevice, T> work) {
        return (AsyncApiWorker<SparkDevice, T>) new AsyncApiWorker<>(sparkDevice, work)
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }


    public static class AsyncApiWorker<ApiCaller, Result> extends AsyncTask<Void, Void, Result> {

        private final ApiCaller caller;
        private final ApiWork<ApiCaller, Result> work;

        private Activity activity;

        private volatile SparkCloudException exception;
        // FIXME: this is Bad and Wrong, but this needs to SHIP, so I'm leaving it for now.
        public volatile IOException ioException;


        private AsyncApiWorker(ApiCaller caller, ApiWork<ApiCaller, Result> work) {
            this.caller = caller;
            this.work = work;
        }

        // This method name looks weird on its own, but looks fine in use.

        /**
         * Prevent all callbacks (onTaskFinished(), onCancelled(), onSuccess(), and onFailure())
         * from being called if the supplied Activity is finishing (i.e.: you don't necessarily
         * care about the )
         *
         * @param activity
         */
        public AsyncApiWorker<ApiCaller, Result> andIgnoreCallbacksIfActivityIsFinishing(Activity activity) {
            this.activity = activity;
            return this;
        }

        @Override
        protected Result doInBackground(Void... voids) {
            try {
                return work.callApi(caller);
            } catch (SparkCloudException e) {
                exception = e;
                return null;
            } catch (IOException e) {
                ioException = e;
                return null;
            }
        }

        @Override
        protected void onCancelled() {
            if (shouldCallCallbacks()) {
                work.onTaskFinished();
                work.onCancelled();
            }
        }

        @Override
        protected void onPostExecute(Result result) {
            if (!shouldCallCallbacks()) {
                return;
            }
            work.onTaskFinished();
            if (exception == null && ioException == null) {
                work.onSuccess(result);
            } else {
                log.e("Error calling API: " + exception.getBestMessage(), exception);
                work.onFailure(exception);
            }
        }

        private boolean shouldCallCallbacks() {
            if (activity == null) {
                return true;
            }
            boolean shouldCall = !activity.isFinishing();
            if (!shouldCall) {
                log.d("Refusing to call callbacks, was told to ignore them if the activity was finishing");
            }
            return shouldCall;
        }
    }

}
