package com.sample.huawei.hihealth;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.huawei.hihealth.error.HiHealthError;
import com.huawei.hihealth.listener.ResultCallback;
import com.huawei.hihealthkit.HiHealthDataQuery;
import com.huawei.hihealthkit.HiHealthDataQueryOption;
import com.huawei.hihealthkit.auth.HiHealthAuth;
import com.huawei.hihealthkit.auth.HiHealthOpenPermissionType;
import com.huawei.hihealthkit.auth.IAuthorizationListener;
import com.huawei.hihealthkit.data.HiHealthPointData;
import com.huawei.hihealthkit.data.store.HiHealthDataStore;
import com.huawei.hihealthkit.data.type.HiHealthPointType;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "MainActivity";

    /*
       Массив пермишенов на чтение данных, которые мы хотим получить
    */
    private int[] readPermissions = new int[]{
            HiHealthOpenPermissionType.HEALTH_OPEN_PERMISSION_TYPE_READ_USER_PROFILE_FEATURE,
            HiHealthOpenPermissionType.HEALTH_OPEN_PERMISSION_TYPE_READ_DATA_POINT_STEP_SUM,
            HiHealthOpenPermissionType.HEALTH_OPEN_PERMISSION_TYPE_READ_USER_PROFILE_INFORMATION
    };

    /*
        Массив пермишенов на запись данных, которые мы хотим получить
    */
    private int[] writeWeightPermissions = new int[0];

    private TextView result;
    private RadioGroup radioGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        result = findViewById(R.id.result);
        radioGroup = findViewById(R.id.radioGroup);

        findViewById(R.id.requestAuthorizationBtn).setOnClickListener(requestAuthorization);
        findViewById(R.id.genderBtn).setOnClickListener(getGender);
        findViewById(R.id.weightBtn).setOnClickListener(getWeight);
        findViewById(R.id.stepBtn).setOnClickListener(getSteps);
    }

    private View.OnClickListener getSteps = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int timeout = 0;
            // получаем время
            long endTime = System.currentTimeMillis();
            long startTime = getStartOfDay();

            long firstDayOfWeek = getFirstDayOfWeek(Calendar.DAY_OF_WEEK);
            long firstDayOfMonth = getFirstDayOfWeek(Calendar.DAY_OF_MONTH);
            long firstDayOfYear = getFirstDayOfWeek(Calendar.DAY_OF_YEAR);

            switch (radioGroup.getCheckedRadioButtonId()) {
                case R.id.week:
                    startTime = firstDayOfWeek;
                    break;
                case R.id.month:
                    startTime = firstDayOfMonth;
                    break;
                case R.id.year:
                    startTime = firstDayOfYear;
                    break;
            }

            // Получаем ArrayList<HiHealthPointData>, который представляет значения для каждого дня
            HiHealthDataQuery hiHealthDataQuery = new HiHealthDataQuery(
                    HiHealthPointType.DATA_POINT_STEP_SUM,
                    startTime,
                    endTime,
                    new HiHealthDataQueryOption()
            );

            HiHealthDataStore.execQuery(getApplicationContext(), hiHealthDataQuery, timeout, new ResultCallback() {
                @Override
                public void onResult(int i, Object data) {
                    if (data != null) {
                        List<HiHealthPointData> dataList = (ArrayList<HiHealthPointData>) data;
                        long steps = 0;
                        for (HiHealthPointData pointData : dataList) {
                            steps += pointData.getValue();
                        }
                        result.setText(getString(R.string.steps, steps));
                    } else {
                        showErrorMessage(getString(R.string.data_type_step_count));
                    }
                }
            });
        }
    };

    private View.OnClickListener getGender =
            view -> HiHealthDataStore.getGender(getApplicationContext(),
                    (errorCode, gender) -> {
                        if (errorCode == HiHealthError.SUCCESS) {
                            switch ((Integer) gender) {
                                case 0:
                                    result.setText(R.string.gender_female);
                                    break;
                                case 1:
                                    result.setText(R.string.gender_male);
                                    break;
                                default:
                                    result.setText(R.string.gender_undefined);
                                    break;
                            }
                        } else {
                            showErrorMessage(getString(R.string.data_type_basic_personal_inf));
                        }
                    });


    private View.OnClickListener getWeight =
            view -> HiHealthDataStore.getWeight(getApplicationContext(),
                    (code, weight) -> {
                        if (code == HiHealthError.SUCCESS) {
                            if (weight instanceof Float) {
                                result.setText(getString(R.string.weight, (Float) weight));
                            }
                        } else {
                            showErrorMessage(getString(R.string.data_type_basic_measurement));
                        }
                    });

    private View.OnClickListener requestAuthorization = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            HiHealthAuth.requestAuthorization(getApplicationContext(), writeWeightPermissions, readPermissions, new IAuthorizationListener() {
                @Override
                public void onResult(int resultCode, Object resultDesc) {
                    switch (resultCode) {
                        case HiHealthError.SUCCESS:
                            result.setText(R.string.req_auth_success);
                            break;
                        case HiHealthError.FAILED:
                            result.setText(R.string.req_auth_failed);
                            break;
                        case HiHealthError.PARAM_INVALIED:
                            result.setText(R.string.req_auth_param_invalid);
                            break;
                        case HiHealthError.ERR_API_EXECEPTION:
                            result.setText(R.string.req_auth_err_api_ex);
                            break;
                        case HiHealthError.ERR_PERMISSION_EXCEPTION:
                            result.setText(R.string.req_auth_err_perm_ex);
                            break;
                        case HiHealthError.ERR_SCOPE_EXCEPTION:
                            result.setText(R.string.req_auth_err_scope_ex);
                            break;
                        default:
                            result.setText(R.string.req_auth_err_undefined);
                            break;
                    }
                    Log.d(TAG, "requestAuthorization onResult: " + resultCode);
                }
            });
        }
    };

    private void showErrorMessage(String dataType) {
        result.setText(
                getString(R.string.errorMessage, dataType)
        );
    }

    private Long getFirstDayOfWeek(Integer dayOf) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0); // ! clear would not reset the hour of day !
        cal.clear(Calendar.MINUTE);
        cal.clear(Calendar.SECOND);
        cal.clear(Calendar.MILLISECOND);

        switch (dayOf) {
            case Calendar.DAY_OF_WEEK:
                cal.set(dayOf, cal.getFirstDayOfWeek());
                return cal.getTimeInMillis();
            case Calendar.DAY_OF_MONTH:
            case Calendar.DAY_OF_YEAR:
                cal.set(dayOf, 1);
                return cal.getTimeInMillis();
            default:
                return 0L;
        }
    }

    private long getStartOfDay() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DATE);
        calendar.set(year, month, day, 0, 0, 0);
        return calendar.getTimeInMillis();
    }
}