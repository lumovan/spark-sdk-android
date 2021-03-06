package io.particle.cloudsdk.example_app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import java.io.IOException;

import io.particle.android.sdk.cloud.SparkCloud;
import io.particle.android.sdk.cloud.SparkCloudException;
import io.particle.android.sdk.cloud.SparkDevice;
import io.particle.android.sdk.utils.Async;
import io.particle.android.sdk.utils.Toaster;

public class ValueActivity extends AppCompatActivity {

    private static final String ARG_VALUE = "ARG_VALUE";
    private static final String ARG_DEVICEID = "ARG_DEVICEID";

    private TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_value);
        tv = (TextView) findViewById(R.id.value);
        tv.setText(String.valueOf(getIntent().getIntExtra(ARG_VALUE, 0)));

        findViewById(R.id.refresh_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //...
                    // Do network work on background thread
                    Async.executeAsync(SparkCloud.get(ValueActivity.this), new Async.ApiWork<SparkCloud, Integer>() {
                        @Override
                        public Integer callApi(SparkCloud sparkCloud) throws SparkCloudException, IOException {
                            SparkDevice device = sparkCloud.getDevice(getIntent().getStringExtra(ARG_DEVICEID));
                            Integer variable;
                            try {
                                variable = device.getVariable("analogvalue");
                            }
                            catch (SparkDevice.VariableDoesNotExistException e)
                            {
                                Toaster.l(ValueActivity.this, e.getMessage());
                                variable = -1;
                            }
                            return variable;
                        }

                        @Override
                        public void onSuccess(Integer i) // this goes on the main thread
                        {
                            tv.setText(i.toString());
                        }

                        @Override
                        public void onFailure(SparkCloudException e) {
                            e.printStackTrace();
                        }
                    });
            }
        });
    }

    public static Intent buildIntent(Context ctx, Integer value, String deviceid)
    {
        Intent intent = new Intent(ctx, ValueActivity.class);
        intent.putExtra(ARG_VALUE, value);
        intent.putExtra(ARG_DEVICEID, deviceid);

        return intent;
    }




}
