package com.android.tony.weather;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.tony.weather.gson.Forecast;
import com.android.tony.weather.gson.Weather;
import com.android.tony.weather.service.AutoUpdateService;
import com.android.tony.weather.util.HttpUtil;
import com.android.tony.weather.util.Utility;
import com.bumptech.glide.Glide;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by admin on 2017/08/04   .
 */

public class WeatherActivity extends AppCompatActivity {
    public static final String TAG = "Tony";

    private ScrollView mWeatherLayout;
    private LinearLayout mForecastLayout;
    public SwipeRefreshLayout swipeRefresh;
    public DrawerLayout drawerLayout;

    private String mWeatherId;

    private TextView mTitleCity;
    private TextView mTitleUpdateTime;
    private TextView mDegreeText;
    private TextView mWeatherInfoText;
    private TextView mAqiText;
    private TextView mPm25Text;
    private TextView mComfortText;
    private TextView mCarWashText;
    private TextView mSportText;

    private ImageView mBingPicImg;

    private Button mNavButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 21) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.activity_weather);

        drawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        swipeRefresh = (SwipeRefreshLayout)findViewById(R.id.swipe_refresh);
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);
        mWeatherLayout = (ScrollView)findViewById(R.id.weather_layout);
        mForecastLayout = (LinearLayout)findViewById(R.id.forecast_layout);

        mTitleCity = (TextView)findViewById(R.id.title_city);
        mTitleUpdateTime = (TextView)findViewById(R.id.title_update_time);
        mDegreeText = (TextView)findViewById(R.id.degree_text);
        mWeatherInfoText = (TextView)findViewById(R.id.weather_info_text);
        mAqiText = (TextView)findViewById(R.id.aqi_text);
        mPm25Text = (TextView)findViewById(R.id.pm25_text);
        mComfortText = (TextView)findViewById(R.id.comfort_text);
        mCarWashText = (TextView)findViewById(R.id.car_wash_text);
        mSportText = (TextView)findViewById(R.id.sport_text);

        mBingPicImg = (ImageView)findViewById(R.id.bing_pic_img);
        mNavButton = (Button)findViewById(R.id.nav_button);

        mNavButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String bingPic = sharedPreferences.getString("bing_pic", null);
        if (bingPic != null) {
            Log.d(TAG, "bingPic != null. bingPic =. "+bingPic.toString());
            Glide.with(this).load(bingPic).into(mBingPicImg);
        } else {
            loadBingPic();
        }
        String weatherString = sharedPreferences.getString("weather", null);
        if (weatherString != null) {
            Weather weather = Utility.handleWeatherResponse(weatherString);
            mWeatherId = weather.basic.weatherId;
            showWeatherInfo(weather);
        } else {
            mWeatherId = getIntent().getStringExtra("weather_id");
            mWeatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(mWeatherId);
        }

        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(mWeatherId);
            }
        });
    }

    public void requestWeather(final String weatherId) {
        String weatherUrl = "http://guolin.tech/api/weather?cityid=" + weatherId + "&key=4d49f5b949484b889638f8c9025952cf";
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG,"onFailure...........");
                        Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string().toString();
                Log.d(TAG, "requestWeather.onResponse  responseText = "+responseText);
                final Weather weather = Utility.handleWeatherResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (weather != null && "ok".equals(weather.status)){
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weather", responseText);
                            editor.apply();
                            showWeatherInfo(weather);
                        } else {
                            Log.d(TAG,"onResponse....");
                            Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        }
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }
        });
        Log.d(TAG, "requestWeather............. ");
        loadBingPic();
    }

    private void showWeatherInfo(Weather weather) {
        String cityName = weather.basic.cityName;
        String updateTime = weather.basic.update.updateTime.split(" ")[1];
        String degree = weather.now.temperature + "℃";
        String weatherInfo = weather.now.more.info;

        mTitleCity.setText(cityName);
        mTitleUpdateTime.setText(updateTime);
        mDegreeText.setText(degree);
        mWeatherInfoText.setText(weatherInfo);

        mForecastLayout.removeAllViews();
        for (Forecast forecast : weather.forecastList) {
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item, mForecastLayout, false);
            TextView dateText = (TextView)view.findViewById(R.id.date_text);
            TextView infoText = (TextView)view.findViewById(R.id.info_text);
            TextView maxText = (TextView)view.findViewById(R.id.max_text);
            TextView minText = (TextView)view.findViewById(R.id.min_text);
            dateText.setText(forecast.date);
            infoText.setText(forecast.more.info);
            maxText.setText(forecast.temperature.max);
            maxText.setText(forecast.temperature.min);
            mForecastLayout.addView(view);
        }
        if (weather.aqi != null) {
            mAqiText.setText(weather.aqi.city.aqi);
            mPm25Text.setText(weather.aqi.city.pm25);
        }
        String comfort = "舒适度：" + weather.suggestion.comfort.info;
        String carWash = "洗车指数：" + weather.suggestion.carWash.info;
        String sport = "运动建议：" + weather.suggestion.sport.info;
        mComfortText.setText(comfort);
        mCarWashText.setText(carWash);
        mSportText.setText(sport);
        mWeatherLayout.setVisibility(View.VISIBLE);
        Intent intent = new Intent(this, AutoUpdateService.class);
        startService(intent);
    }

    private void loadBingPic() {
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic = response.body().string().toString();
                Log.d(TAG, "loadBingPic.onResponse bingPic = "+bingPic);
                SharedPreferences.Editor editor  = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("bing_pic", bingPic);
                editor.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(bingPic).into(mBingPicImg);
                    }
                });
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
