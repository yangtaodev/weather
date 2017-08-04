package com.android.tony.weather;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.tony.weather.db.City;
import com.android.tony.weather.db.County;
import com.android.tony.weather.db.Province;
import com.android.tony.weather.util.HttpUtil;
import com.android.tony.weather.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by admin on 2017/08/03   .
 */

public class ChooseAreaFragment extends Fragment {
    public static final String TAG = "Tony";
    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;

    private ProgressDialog mProgressDialog;
    private TextView mTitleText;
    private Button mBackButton;
    private ListView mListView;

    private ArrayAdapter<String> mAdapter;
    private List<String> mDataList = new ArrayList<>();
    private List<Province> mProvinceList;
    private List<City> mCityList;
    private List<County> mCountyList;
    private Province mSelectedProvince;
    private City mSelectedCity;
    private int mCurrentLevel;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.choose_area, container, false);
        mTitleText = (TextView)view.findViewById(R.id.title_text);
        mBackButton = (Button)view.findViewById(R.id.back_button);
        mListView = (ListView)view.findViewById(R.id.list_view);
        mAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, mDataList);
        mListView.setAdapter(mAdapter);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                if (mCurrentLevel == LEVEL_PROVINCE){
                    mSelectedProvince = mProvinceList.get(position);
                    queryCities();
                } else if (mCurrentLevel == LEVEL_CITY){
                    mSelectedCity = mCityList.get(position);
                    queryCounties();
                }
            }
        });
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCurrentLevel == LEVEL_COUNTY){
                    queryCities();
                } else if (mCurrentLevel == LEVEL_CITY){
                    queryProvinces();
                }
            }
        });
        queryProvinces();
    }

    private void queryProvinces(){
        mTitleText.setText("中国");
        mBackButton.setVisibility(View.GONE);
        mProvinceList = DataSupport.findAll(Province.class);
        if (mProvinceList.size() > 0){
            mDataList.clear();
            for (Province province : mProvinceList){
                mDataList.add(province.getProvinceName());
            }
            mAdapter.notifyDataSetChanged();
            mListView.setSelection(0);
            mCurrentLevel = LEVEL_PROVINCE;
        } else {
            String address = "http://guolin.tech/api/china";
            queryFromServer(address, "province");
        }
    }

    private void queryCities(){
        mTitleText.setText(mSelectedProvince.getProvinceName());
        mBackButton.setVisibility(View.VISIBLE);
        mCityList = DataSupport.where("provinceid = ?", String.valueOf(mSelectedProvince.getId())).find(City.class);
        if (mCityList.size() > 0){
            mDataList.clear();
            for (City city : mCityList){
                mDataList.add(city.getCityName());
            }
            mAdapter.notifyDataSetChanged();
            mListView.setSelection(0);
            mCurrentLevel = LEVEL_CITY;
        } else {
            int provinceCode = mSelectedProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china/" + provinceCode;
            queryFromServer(address, "city");
        }
    }

    private void queryCounties(){
        mTitleText.setText(mSelectedCity.getCityName());
        mBackButton.setVisibility(View.VISIBLE);
        mCountyList = DataSupport.where("cityid = ?", String.valueOf(mSelectedCity.getId())).find(County.class);
        if (mCountyList.size() > 0){
            mDataList.clear();
            for (County county : mCountyList){
                mDataList.add(county.getCountyName());
            }
            mAdapter.notifyDataSetChanged();
            mListView.setSelection(0);
            mCurrentLevel = LEVEL_COUNTY;
        } else {
            int provinceCode = mSelectedProvince.getProvinceCode();
            int cityCode = mSelectedCity.getCityCode();
            String address = "http://guolin.tech/api/china/" + provinceCode + "/" + cityCode;
            Log.d(TAG,"address = "+address);
            queryFromServer(address, "county");
        }
    }

    private void queryFromServer(String address, final String type){
        showProgressDialog();
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(), "加载失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();
                Log.d(TAG,"responseText = "+responseText);
                Log.d(TAG,"type = "+type);
                boolean result = false;
                if ("province".equals(type)){
                    result = Utility.handleProvinceResponse(responseText);
                } else if ("city".equals(type)){
                    result = Utility.handleCityResponse(responseText, mSelectedProvince.getId());
                } else if ("county".equals(type)){
                    result = Utility.handleCountyResponse(responseText, mSelectedCity.getId());
                }
                Log.d(TAG,"result = "+result);
                if (result){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if ("province".equals(type)){
                                queryProvinces();
                            } else if ("city".equals(type)){
                                queryCities();
                            } else if ("county".equals(type)){
                                queryCounties();
                            }
                        }
                    });
                }
            }
        });
    }

    private void showProgressDialog(){
        if (mProgressDialog == null){
            mProgressDialog = new ProgressDialog(getActivity());
            mProgressDialog.setMessage("正在加载...");
            mProgressDialog.setCanceledOnTouchOutside(false);
        }
        mProgressDialog.show();
    }

    private void closeProgressDialog(){
        if (mProgressDialog != null){
            mProgressDialog.dismiss();
        }
    }
}
