
package com.handsomezhou.xdesktophelper.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.handsomezhou.xdesktophelper.Interface.OnTabChange;
import com.handsomezhou.xdesktophelper.R;
import com.handsomezhou.xdesktophelper.activity.MainActivity;
import com.handsomezhou.xdesktophelper.adapter.CustomPartnerViewPagerAdapter;
import com.handsomezhou.xdesktophelper.baidu.aip.constant.BaiduConstant;
import com.handsomezhou.xdesktophelper.baidu.aip.helper.BaiduAipHelper;
import com.handsomezhou.xdesktophelper.baidu.aip.model.Event;
import com.handsomezhou.xdesktophelper.baidu.aip.model.NlpLexer;
import com.handsomezhou.xdesktophelper.baidu.aip.util.PartOfSpeechUtil;
import com.handsomezhou.xdesktophelper.constant.Constant;
import com.handsomezhou.xdesktophelper.constant.EventAction;
import com.handsomezhou.xdesktophelper.dialog.BaseProgressDialog;
import com.handsomezhou.xdesktophelper.fragment.T9SearchFragment.OnT9SearchFragment;
import com.handsomezhou.xdesktophelper.helper.AppInfoHelper;
import com.handsomezhou.xdesktophelper.helper.AppInfoHelper.OnAppInfoLoad;
import com.handsomezhou.xdesktophelper.helper.AppSettingInfoHelper;
import com.handsomezhou.xdesktophelper.helper.AppSettingInfoHelper.OnAppSettingInfoLoad;
import com.handsomezhou.xdesktophelper.helper.AppStartRecordHelper;
import com.handsomezhou.xdesktophelper.helper.AppStartRecordHelper.OnAppStartRecordLoad;
import com.handsomezhou.xdesktophelper.helper.SettingsHelper;
import com.handsomezhou.xdesktophelper.model.AppDownloadInfo;
import com.handsomezhou.xdesktophelper.model.AppInfo;
import com.handsomezhou.xdesktophelper.model.IconButtonData;
import com.handsomezhou.xdesktophelper.model.IconButtonValue;
import com.handsomezhou.xdesktophelper.constant.LoadStatus;
import com.handsomezhou.xdesktophelper.model.PartnerView;
import com.handsomezhou.xdesktophelper.constant.SearchMode;
import com.handsomezhou.xdesktophelper.util.AppUtil;
import com.handsomezhou.xdesktophelper.util.CommonUtil;
import com.handsomezhou.xdesktophelper.util.ContextAnalysisUtil;
import com.handsomezhou.xdesktophelper.util.JsonUtil;
import com.handsomezhou.xdesktophelper.util.LogUtil;
import com.handsomezhou.xdesktophelper.util.ShareUtil;
import com.handsomezhou.xdesktophelper.util.TimeUtil;
import com.handsomezhou.xdesktophelper.util.ToastUtil;
import com.handsomezhou.xdesktophelper.util.ViewUtil;
import com.handsomezhou.xdesktophelper.util.XfyunErrorCodeUtil;
import com.handsomezhou.xdesktophelper.view.CustomViewPager;
import com.handsomezhou.xdesktophelper.view.TopTabView;
import com.handsomezhou.xdesktophelper.xfyun.setting.IatSettings;
import com.handsomezhou.xdesktophelper.xfyun.util.JsonParser;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;
import com.zhl.userguideview.UserGuideView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class MainFragment extends BaseFragment implements OnAppInfoLoad, OnAppStartRecordLoad, OnAppSettingInfoLoad, OnTabChange, OnT9SearchFragment, QwertySearchFragment.OnQwertySearchFragment {
    private static final String TAG = MainFragment.class.getSimpleName();
    private UserGuideView mUserGuideView;
    private View mTipTextView;
    private int mUserGuideViewIndex=0;
    private List<PartnerView> mPartnerViews;
    private TopTabView mTopTabView;
    private TextView mDataCountTv;
    private ImageView mShareIv;
    private CustomViewPager mCustomViewPager;
    private CustomPartnerViewPagerAdapter mCustomPartnerViewPagerAdapter;
    private BaseProgressDialog mBaseProgressDialog;
    private PopupWindow mSearchModeSwitchTipsPw;

    /*start: xfyun voice input*/
    // 语音听写对象
    private SpeechRecognizer mIat;
    // 语音听写UI
    private RecognizerDialog mIatDialog;
    // 用HashMap存储听写结果
    private HashMap<String, String> mIatResults = new LinkedHashMap<String, String>();

    private Toast mToast;
    private SharedPreferences mSharedPreferences;
    // 引擎类型
    private String mEngineType = SpeechConstant.TYPE_CLOUD;
    /*end: xfyun voice input*/

    private boolean mVoiceSearch = false;
    private String mVoiceText = null;
    private int mDataCount=0;

    @Override
    public void onResume() {
        super.onResume();

        mCustomViewPager.setCurrentItem(getPartnerViewItem(SettingsHelper.getInstance().getSearchMode()));
        refreshView();
        boolean appInfoChanged=AppInfoHelper.getInstance().isAppInfoChanged();
        if(true==appInfoChanged) {
            AppStartRecordHelper.getInstance().setOnAppStartRecordLoad(this);
            AppStartRecordHelper.getInstance().startLoadAppStartRecord();

            AppInfoHelper.getInstance().setOnAppInfoLoad(this);
            boolean startLoadSuccess = AppInfoHelper.getInstance().startLoadAppInfo();
            if (true == startLoadSuccess) {

            }
        }else {
            if(true==SettingsHelper.getInstance().isVoiceSearchEnable()){
                if(true==SettingsHelper.getInstance().isEnterAppStartVoiceSearch()){
                    startVoiceInput();
                }
            }
        }
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
        //退出时释放连接
        if (null != mIat) {
            mIat.cancel();
            mIat.destroy();
        }
    }

    @Override
    protected void initData() {
        setContext(getActivity());
        initSpeechRecognizer();
        mPartnerViews = new ArrayList<PartnerView>();
        /* start: T9 search view */
        T9SearchFragment t9SearchFragment = new T9SearchFragment();
        t9SearchFragment.setOnT9SearchFragment(this);

        PartnerView t9PartnerView = new PartnerView(SearchMode.T9, t9SearchFragment);
        mPartnerViews.add(t9PartnerView);

        /* end: T9 search view */

        /* start: Qwerty search view */
        QwertySearchFragment qwertySearchFragment = new QwertySearchFragment();
        qwertySearchFragment.setOnQwertySearchFragment(this);
        PartnerView qwertyPartnerView = new PartnerView(SearchMode.QWERTY,
                qwertySearchFragment);
        mPartnerViews.add(qwertyPartnerView);
        /* end: Qwerty search view */

        AppStartRecordHelper.getInstance().setOnAppStartRecordLoad(this);
        AppStartRecordHelper.getInstance().startLoadAppStartRecord();

        AppInfoHelper.getInstance().setOnAppInfoLoad(this);
        boolean startLoadSuccess = AppInfoHelper.getInstance()
                .startLoadAppInfo();
        if (true == startLoadSuccess) {
            //getBaseProgressDialog().show(getContext().getString(R.string.app_info_loading));
        }

    }

    @Override
    protected View initView(LayoutInflater inflater, ViewGroup container) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        mUserGuideView=(UserGuideView) view.findViewById(R.id.user_guide_view);
        mUserGuideView.setOnDismissListener(new UserGuideView.OnDismissListener() {
            @Override
            public void onDismiss(UserGuideView userGuideView) {
                showUserGuideView(mUserGuideViewIndex);
                refreshUserGuideView();
            }
        });
        //
        mTipTextView=LayoutInflater.from(getActivity()).inflate(R.layout.custom_tipview,null);

        mCustomViewPager = (CustomViewPager) view
                .findViewById(R.id.custom_view_pager);
        mCustomViewPager.setPagingEnabled(false);

        mTopTabView = (TopTabView) view.findViewById(R.id.top_tab_view);
        mTopTabView.setTextColorFocused(getContext().getResources().getColor(R.color.sea_green4));
        mTopTabView.setTextColorUnfocused(getContext().getResources().getColor(R.color.dim_grey));
        mTopTabView.setTextColorUnselected(getContext().getResources().getColor(R.color.dim_grey));
        mTopTabView.setHideIcon(true);
        mTopTabView.removeAllViews();

        mDataCountTv=view.findViewById(R.id.data_count_text_view);


        /* start: T9 search tab */
        IconButtonValue t9IconBtnValue = new IconButtonValue(SearchMode.T9, 0, R.string.t9_search);
        t9IconBtnValue.setHideIcon(mTopTabView.isHideIcon());
        IconButtonData t9IconBtnData = new IconButtonData(getContext(),
                t9IconBtnValue);
        mTopTabView.addIconButtonData(t9IconBtnData);
        
        /* end: T9 search tab */

        /* start: Qwerty search tab */
        IconButtonValue qwertyIconBtnValue = new IconButtonValue(
                SearchMode.QWERTY, 0, R.string.qwerty_search);
        t9IconBtnValue.setHideIcon(mTopTabView.isHideIcon());
        IconButtonData qwertyIconBtnData = new IconButtonData(getContext(),
                qwertyIconBtnValue);
        mTopTabView.addIconButtonData(qwertyIconBtnData);
        /* end: Qwerty search tab */

        mTopTabView.setOnTabChange(this);
        mShareIv = (ImageView) view.findViewById(R.id.share_image_view);
        return view;
    }

    @Override
    protected void initListener() {
        mShareIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<AppDownloadInfo> appDownloadInfos=loadAppDownloadInfo();
                StringBuffer textContentStringBuffer=new StringBuffer();
                textContentStringBuffer.append(getString(R.string.app_function)).append(Constant.NEW_LINE).append(Constant.NEW_LINE);
                textContentStringBuffer.append(getString(R.string.x_desktop_helper_download_link)).append(Constant.NEW_LINE);
                AppDownloadInfo appDownloadInfo=null;
                for(int i=0; i<appDownloadInfos.size();i++){
                    appDownloadInfo=appDownloadInfos.get(i);
                    textContentStringBuffer.append(appDownloadInfo.getAppMarket()).append(Constant.NEW_LINE);
                    textContentStringBuffer.append(appDownloadInfo.getDownloadAddress()).append(Constant.NEW_LINE).append(Constant.NEW_LINE);
                }

                ShareUtil.shareTextToMore(getContext(),getString(R.string.app_name),textContentStringBuffer.toString());
            }
        });

        FragmentManager fm = getChildFragmentManager();
        mCustomPartnerViewPagerAdapter = new CustomPartnerViewPagerAdapter(fm,
                mPartnerViews);
        mCustomViewPager.setAdapter(mCustomPartnerViewPagerAdapter);
        mCustomViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

                    @Override
                    public void onPageSelected(int pos) {

                        PartnerView partnerView = mPartnerViews.get(pos);
                        // Toast.makeText(getContext(),addressBookView.getTag().toString()+"+++"
                        // , Toast.LENGTH_LONG).show();
                        mTopTabView.setCurrentTabItem(partnerView.getTag());
                        SettingsHelper.getInstance().setSearchMode((SearchMode) partnerView.getTag());
                        refreshView();
                    }

                    @Override
                    public void onPageScrolled(int pos, float posOffset,
                                               int posOffsetPixels) {

                    }

                    @Override
                    public void onPageScrollStateChanged(int state) {

                    }
                });


        mCustomViewPager.setCurrentItem(getPartnerViewItem(SettingsHelper.getInstance().getSearchMode()));

    }

    /* start: OnAppInfoLoad */
    @Override
    public void onAppInfoLoadSuccess() {
        getBaseProgressDialog().hide();

        if (AppStartRecordHelper.getInstance().getAppStartRecordsLoadStatus() == LoadStatus.LOAD_FINISH) {
            Log.i(TAG, "onAppInfoLoadSuccess before");
            if (true == AppStartRecordHelper.getInstance().parseAppStartRecord()) {
                Log.i(TAG, "onAppInfoLoadSuccess ture");
            } else {
                Log.i(TAG, "onAppInfoLoadSuccess false");
            }
            AppSettingInfoHelper.getInstance().startLoadAppSettingInfo(this);
        }
        AppInfoHelper.getInstance().qwertySearch(null);
        AppInfoHelper.getInstance().t9Search(null, false);
        refreshView();

    }

    @Override
    public void onAppInfoLoadFailed() {
        getBaseProgressDialog().hide();
        refreshView();
    }

    /* end: OnAppInfoLoad */

    /* start: OnAppStartRecordLoad */
    @Override
    public void onAppStartRecordSuccess() {
        if (AppInfoHelper.getInstance().getBaseAllAppInfosLoadStatus() == LoadStatus.LOAD_FINISH) {
            // Log.i(TAG, "onAppStartRecordSuccess before");
            if (true == AppStartRecordHelper.getInstance().parseAppStartRecord()) {
                Log.i(TAG, "onAppStartRecordSuccess ture");
                refreshView();
            } else {
                Log.i(TAG, "onAppStartRecordSuccess false");
            }
            AppSettingInfoHelper.getInstance().startLoadAppSettingInfo(this);
        }

    }

    @Override
    public void onAppStartRecordFailed() {
        // TODO Auto-generated method stub

    }

    /* end: OnAppStartRecordLoad */

    /*start: OnAppSettingInfoLoad*/
    @Override
    public void onAppSettingInfoLoadSuccess() {
        AppSettingInfoHelper.getInstance().parseAppSettingInfo();
        refreshView();
        if (true == SettingsHelper.getInstance().isUserGuideTips()) {

            showUserGuideView(mUserGuideViewIndex);
        }

    }

    @Override
    public void onAppSettingInfoLoadFailed() {
        // TODO Auto-generated method stub

    }
    /*end: OnAppSettingInfoLoad*/

    /* start: OnTabChange */
    @Override
    public void onChangeToTab(Object fromTab, Object toTab,
                              TAB_CHANGE_STATE tabChangeState) {
        int item = getPartnerViewItem(toTab);
        mCustomViewPager.setCurrentItem(item);

    }

    @Override
    public void onClickTab(Object currentTab, TAB_CHANGE_STATE tabChangeState) {
        Fragment fragment = mPartnerViews.get(getPartnerViewItem(currentTab))
                .getFragment();
        switch ((SearchMode) currentTab) {
            case T9:
                onChangeToTab(SearchMode.T9, SearchMode.QWERTY, tabChangeState);
               /* if (fragment instanceof T9SearchFragment) {
                    // ((T9SearchFragment) fragment).updateView(tabChangeState);
                    ((T9SearchFragment) fragment).refreshView();
                }*/
                break;
            case QWERTY:
                onChangeToTab(SearchMode.QWERTY, SearchMode.T9, tabChangeState);
               /* if (fragment instanceof QwertySearchFragment) {
                    ((QwertySearchFragment) fragment).refreshView();
                }*/
                break;
            default:
                break;
        }

    }

    /* end: OnTabChange */

    /*start: OnT9SearchFragment*/
    @Override
    public void onT9SearchVoiceInput() {
        //ToastUtil.toastLengthshort(getContext(),"onT9SearchVoiceInput main");
        startVoiceInput();
    }

    @Override
    public void onT9SearchRefreshView() {
        Object currentTab = mTopTabView.getCurrentTab();
        int itemIndex = getPartnerViewItem(currentTab);
        Fragment fragment = mPartnerViews.get(itemIndex).getFragment();
        if(fragment instanceof T9SearchFragment) {
            setDataCount(((T9SearchFragment) fragment).getDataCount());
            refreshDataCountTv();
        }

    }
    /*end: OnT9SearchFragment*/

    /*start: OnQwertySearchFragment*/
    @Override
    public void onQwertySearchVoiceInput() {
        //ToastUtil.toastLengthshort(getContext(),"onQwertySearchVoiceInput main");
        startVoiceInput();
    }

    @Override
    public void onQwertySearchRefreshView() {
        Object currentTab = mTopTabView.getCurrentTab();
        int itemIndex = getPartnerViewItem(currentTab);
        Fragment fragment = mPartnerViews.get(itemIndex).getFragment();
        if(fragment instanceof QwertySearchFragment) {
            setDataCount(((QwertySearchFragment) fragment).getDataCount());
            refreshDataCountTv();
        }
    }
    /*end: OnQwertySearchFragment*/

    public BaseProgressDialog getBaseProgressDialog() {
        if (null == mBaseProgressDialog) {
            mBaseProgressDialog = new BaseProgressDialog(getContext());
        }
        return mBaseProgressDialog;
    }

    public void setBaseProgressDialog(BaseProgressDialog baseProgressDialog) {
        mBaseProgressDialog = baseProgressDialog;
    }


    public PopupWindow getSearchModeSwitchTipsPw() {
        if (null == mSearchModeSwitchTipsPw) {
            LayoutInflater inflater = (LayoutInflater) getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View conference_start_tips_popup_window = (View) inflater.inflate(
                    R.layout.popup_window_search_mode_switch_tips, null);
            mSearchModeSwitchTipsPw = new PopupWindow(conference_start_tips_popup_window,
                    LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            ColorDrawable dw = new ColorDrawable(getContext().getResources().getColor(R.color.grey21_transparent));
            mSearchModeSwitchTipsPw.setBackgroundDrawable(dw);
            mSearchModeSwitchTipsPw.setOutsideTouchable(true);
         /*   mSearchModeSwitchTipsPw.setFocusable(true);*/
            mSearchModeSwitchTipsPw.setTouchable(true);

            conference_start_tips_popup_window.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    mSearchModeSwitchTipsPw.dismiss();

                }
            });
        }

        return mSearchModeSwitchTipsPw;
    }

    public void setSearchModeSwitchTipsPw(PopupWindow searchModeSwitchTipsPw) {
        mSearchModeSwitchTipsPw = searchModeSwitchTipsPw;
    }

    public boolean isVoiceSearch() {
        return mVoiceSearch;
    }

    public void setVoiceSearch(boolean voiceSearch) {
        mVoiceSearch = voiceSearch;
    }

    public String getVoiceText() {
        return mVoiceText;
    }

    public void setVoiceText(String voiceText) {
        mVoiceText = voiceText;
    }

    public int getDataCount() {
        return mDataCount;
    }

    public void setDataCount(int dataCount) {
        mDataCount = dataCount;
    }

    private void refreshView() {
        refreshUserGuideView();
        refreshShareIv();
        showTopTabView(SettingsHelper.getInstance().getSearchMode());
        Object currentTab = mTopTabView.getCurrentTab();
        int itemIndex = getPartnerViewItem(currentTab);
        Fragment fragment = mPartnerViews.get(itemIndex).getFragment();
        switch ((SearchMode) currentTab) {
            case T9:
                if (fragment instanceof T9SearchFragment) {
                    if (true == isVoiceSearch()) {
                        ((T9SearchFragment) fragment).voiceSearch(getVoiceText());
                    } else {
                        ((T9SearchFragment) fragment).search();
                    }
                    ((T9SearchFragment) fragment).refreshView();
                    setDataCount(((T9SearchFragment) fragment).getDataCount());
                }
                break;
            case QWERTY:

                if (fragment instanceof QwertySearchFragment) {
                    if (true == isVoiceSearch()) {
                        ((QwertySearchFragment) fragment).voiceSearch(getVoiceText());
                    } else {
                        ((QwertySearchFragment) fragment).search();
                    }
                    ((QwertySearchFragment) fragment).refreshView();
                    setDataCount(((QwertySearchFragment) fragment).getDataCount());
                }
                break;
            default:
                break;
        }
        refreshDataCountTv();
    }

    private int getPartnerViewItem(Object tag) {
        int item = 0;
        ;
        do {
            if (null == tag) {
                break;
            }

            for (int i = 0; i < mPartnerViews.size(); i++) {
                if (mPartnerViews.get(i).getTag().equals(tag)) {
                    item = i;
                    break;
                }
            }
        } while (false);

        return item;
    }

    private void showTopTabView(SearchMode searchMode) {
        for (int i = 0; i < mTopTabView.getChildCount(); i++) {
            ViewUtil.hideView(mTopTabView.getChildAt(i));
        }
        ViewUtil.showView(mTopTabView.getChildAt(searchMode.ordinal()));

    }

    /*start: xfyun voice input*/
    private void startVoiceInput() {
        // 移动数据分析，收集开始听写事件
        //FlowerCollector.onEvent(getActivity(), "iat_recognize");
        int ret = 0; // 函数调用返回值
        mIatResults.clear();
        // 设置参数
        setParam();
        boolean isShowDialog = mSharedPreferences.getBoolean(
                getString(R.string.pref_key_iat_show), true);
        if (isShowDialog) {
            // 显示听写对话框
            mIatDialog.setListener(mRecognizerDialogListener);
            mIatDialog.show();
            showTip(getString(R.string.text_begin));
        } else {
            // 不显示听写对话框
            ret = mIat.startListening(mRecognizerListener);
            if (ret != ErrorCode.SUCCESS) {
                showTip("听写失败,错误码：" + ret);
            } else {
                showTip(getString(R.string.text_begin));
            }
        }


    }

    private void initSpeechRecognizer() {
        // 初始化识别无UI识别对象
        // 使用SpeechRecognizer对象，可根据回调消息自定义界面；
        mIat = SpeechRecognizer.createRecognizer(getActivity(), mInitListener);

        // 初始化听写Dialog，如果只使用有UI听写功能，无需创建SpeechRecognizer
        // 使用UI听写功能，请根据sdk文件目录下的notice.txt,放置布局文件和图片资源
        mIatDialog = new RecognizerDialog(getActivity(), mInitListener);

        mSharedPreferences = getContext().getSharedPreferences(IatSettings.PREFER_NAME,
                Activity.MODE_PRIVATE);
        mToast = Toast.makeText(getContext(), "", Toast.LENGTH_SHORT);
    }

    /**
     * 初始化监听器。
     */
    private InitListener mInitListener = new InitListener() {

        @Override
        public void onInit(int code) {
            Log.d(TAG, "SpeechRecognizer init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                showTip("初始化失败，错误码：" + code);
            }
        }
    };

    /**
     * 参数设置
     */
    public void setParam() {
        // 清空参数
        mIat.setParameter(SpeechConstant.PARAMS, null);

        // 设置听写引擎
        mIat.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
        // 设置返回结果格式
        mIat.setParameter(SpeechConstant.RESULT_TYPE, "json");

        String lag = mSharedPreferences.getString("iat_language_preference",
                "mandarin");
        if (lag.equals("en_us")) {
            // 设置语言
            mIat.setParameter(SpeechConstant.LANGUAGE, "en_us");
        } else {
            // 设置语言
            mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
            // 设置语言区域
            mIat.setParameter(SpeechConstant.ACCENT, lag);
        }

        // 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
        mIat.setParameter(SpeechConstant.VAD_BOS, mSharedPreferences.getString("iat_vadbos_preference", "4000"));

        // 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
        mIat.setParameter(SpeechConstant.VAD_EOS, mSharedPreferences.getString("iat_vadeos_preference", "1000"));

        // 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
        mIat.setParameter(SpeechConstant.ASR_PTT, mSharedPreferences.getString("iat_punc_preference", "0"));

        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        // 注：AUDIO_FORMAT参数语记需要更新版本才能生效
        mIat.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
        mIat.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory() + "/msc/iat.wav");
    }

    /**
     * 听写UI监听器
     */
    private RecognizerDialogListener mRecognizerDialogListener = new RecognizerDialogListener() {
        public void onResult(RecognizerResult results, boolean isLast) {
            final String voiceText = parseResult(results);
            if (true == isLast) {
                setVoiceSearch(true);
                if (CommonUtil.isEmpty(BaiduConstant.APP_ID) || CommonUtil.isEmpty(BaiduConstant.API_KEY) || CommonUtil.isEmpty(BaiduConstant.SECRET_KEY)) {

                    setVoiceText(voiceText);
                    //ToastUtil.toastLengthshort(getContext(), "2" + voiceText);
                    refreshView();
                    setVoiceSearch(false);
                } else {
                    BaiduAipHelper.getInstance().startNlp(voiceText, new BaiduAipHelper.OnAipNlp() {
                        @Override
                        public void onAipNlpSuccess(String text, NlpLexer nlpLexer) {
                            //ToastUtil.toastLengthshort(getContext(),nlpLexer.getText()+ Constant.NEW_LINE+ JsonUtil.toJson(nlpLexer.getItems()));
                            List<Event> events = ContextAnalysisUtil.parse(nlpLexer);
                            if (null == events || events.size() <= 0) {
                                String tips = getString(R.string.i_can_not_understand_your_words, nlpLexer.getText());
                                ToastUtil.toastLengthshort(getContext(), tips);
                                //ToastUtil.toastLengthshort(getContext(),tips+ Constant.NEW_LINE+ JsonUtil.toJson(nlpLexer.getItems()));
                            } else {
                                Event event = events.get(events.size() - 1);
                                processEvent(event);
                                //ToastUtil.toastLengthshort(getContext(),JsonUtil.toJson(event));

                            }

                            //test
                            if(false==CommonUtil.isEmpty(voiceText)){
                                LogUtil.i(TAG,"================================start================================");
                                LogUtil.i(TAG,"voiceText:["+voiceText+"]");
                                if(null!=nlpLexer){
                                    LogUtil.i(TAG, "log_id["+nlpLexer.getLog_id()+"]");
                                    LogUtil.i(TAG, "text:["+nlpLexer.getText()+"]");
                                    for(int i=0;i<nlpLexer.getItems().size();i++){
                                        LogUtil.i(TAG, "item"+"["+i+"]["+PartOfSpeechUtil.getPartOfSpeechDesc(nlpLexer.getItems().get(i))+"]"+JsonUtil.toJson(nlpLexer.getItems().get(i)));
                                    }
                                }

                                for(int i=0;i<events.size();i++){
                                    LogUtil.i(TAG,"i=["+i+"] "+events.get(i).toString());
                                }
                                LogUtil.i(TAG,"================================end================================");
                            }
                            setVoiceSearch(false);
                        }

                        @Override
                        public void onAipNlpFailed(String text) {
                            setVoiceSearch(false);
                        }
                    });
                }
            }
        }

        /**
         * 识别回调错误.
         */
        public void onError(SpeechError error) {

            // showTip(error.getPlainDescription(true));
            dealSpeechError(error);
        }

    };


    private void dealSpeechError(SpeechError error) {
        do {
            if (null == error) {
                break;
            }
            int errorCode = error.getErrorCode();
            if (errorCode == ErrorCode.MSP_ERROR_NO_DATA) {
                break;
            }
            String xfyunErrorCodeDescription = XfyunErrorCodeUtil.getXfyunErrorCodeDescription(getContext(), errorCode);
            //  showTip(error.getPlainDescription(true));
            showTip(xfyunErrorCodeDescription);
        } while (false);

        return;
    }

    /**
     * 听写监听器。
     */
    private RecognizerListener mRecognizerListener = new RecognizerListener() {

        @Override
        public void onBeginOfSpeech() {
            // 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
            showTip("开始说话");
        }

        @Override
        public void onError(SpeechError error) {
            // Tips：
            // 错误码：10118(您没有说话)，可能是录音机权限被禁，需要提示用户打开应用的录音权限。
            // 如果使用本地功能（语记）需要提示用户开启语记的录音权限。

            //showTip(error.getPlainDescription(true));
            dealSpeechError(error);
        }

        @Override
        public void onEndOfSpeech() {
            // 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
            showTip("结束说话");
        }

        @Override
        public void onResult(RecognizerResult results, boolean isLast) {
            Log.d(TAG, results.getResultString());
            String voiceText = parseResult(results);
            //   ToastUtil.toastLengthshort(getContext(),"1"+voiceText);
            if (isLast) {
                setVoiceSearch(true);
                if (CommonUtil.isEmpty(BaiduConstant.APP_ID) || CommonUtil.isEmpty(BaiduConstant.API_KEY) || CommonUtil.isEmpty(BaiduConstant.SECRET_KEY)) {

                    setVoiceText(voiceText);
                    //ToastUtil.toastLengthshort(getContext(), "2" + voiceText);
                    refreshView();
                    setVoiceSearch(false);
                } else {
                    BaiduAipHelper.getInstance().startNlp(voiceText, new BaiduAipHelper.OnAipNlp() {
                        @Override
                        public void onAipNlpSuccess(String text, NlpLexer nlpLexer) {
                            //ToastUtil.toastLengthshort(getContext(),nlpLexer.getText()+ Constant.NEW_LINE+ JsonUtil.toJson(nlpLexer.getItems()));
                            List<Event> events = ContextAnalysisUtil.parse(nlpLexer);
                            if (null == events || events.size() <= 0) {
                                String tips = getString(R.string.i_can_not_understand_your_words, nlpLexer.getText());
                                ToastUtil.toastLengthshort(getContext(), tips);
                                //ToastUtil.toastLengthshort(getContext(),tips+ Constant.NEW_LINE+ JsonUtil.toJson(nlpLexer.getItems()));
                            } else {
                                Event event = events.get(events.size() - 1);
                                processEvent(event);
                                //ToastUtil.toastLengthshort(getContext(),JsonUtil.toJson(event));

                            }
                            setVoiceSearch(false);
                        }

                        @Override
                        public void onAipNlpFailed(String text) {
                            setVoiceSearch(false);
                        }
                    });
                }
            }
        }

        @Override
        public void onVolumeChanged(int volume, byte[] data) {
            showTip("当前正在说话，音量大小：" + volume);
            Log.d(TAG, "返回音频数据：" + data.length);
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            // 若使用本地能力，会话id为null
            //	if (SpeechEvent.EVENT_SESSION_ID == eventType) {
            //		String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
            //		Log.d(TAG, "session id =" + sid);
            //	}
        }
    };

    private String parseResult(RecognizerResult results) {
        String iatResultStr = null;
        String text = JsonParser.parseIatResult(results.getResultString());

        String sn = null;
        // 读取json结果中的sn字段
        try {
            JSONObject resultJson = new JSONObject(results.getResultString());
            sn = resultJson.optString("sn");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mIatResults.put(sn, text);

        StringBuffer resultBuffer = new StringBuffer();
        for (String key : mIatResults.keySet()) {
            resultBuffer.append(mIatResults.get(key));
        }

        iatResultStr = resultBuffer.toString();

        return iatResultStr;
    }

    private void showTip(final String str) {
        mToast.setText(str);
        mToast.show();
    }

    /*end: xfyun voice input*/

    /**
     * 处理事件
     *
     * @param event
     */
    public void processEvent(Event event) {
        do {
            if (null == event) {
                break;
            }
            AppInfo appInfo = AppInfoHelper.getInstance().qwertySearchMatch(event.getContext());
            if (CommonUtil.isEmpty(event.getAction())) {
                if (null == appInfo) {
                    ToastUtil.toastLengthshort(getContext(), getString(R.string.no_related_app_can_be_opened, event.getContext()));
                } else {
                    AppUtil.startApp(getContext(), appInfo);
                }
                break;
            }
            switch (event.getAction()) {
                case EventAction.UNINSTALL:

                    if (null == appInfo) {
                        ToastUtil.toastLengthshort(getContext(), getString(R.string.no_related_app_can_be_uninstalled, event.getContext()));
                    } else {
                        AppUtil.uninstallApp(getContext(), appInfo);
                    }
                    break;
                case EventAction.OPEN:
                    if (null == appInfo) {
                        ToastUtil.toastLengthshort(getContext(), getString(R.string.no_related_app_can_be_opened, event.getContext()));
                    } else {
                        AppUtil.startApp(getContext(), appInfo);
                    }
                    break;
                default:
                    if (null == appInfo) {
                        ToastUtil.toastLengthshort(getContext(), getString(R.string.no_related_app_can_be_opened, event.getContext()));
                    } else {
                        AppUtil.startApp(getContext(), appInfo);
                    }
                    break;
            }

        } while (false);

        return;
    }


    private void showUserGuideView(View targetView,String tips){
        mUserGuideViewIndex++;
        TextView tipsTv=(TextView) mTipTextView.findViewById(R.id.tips_text_view);
        tipsTv.setText(tips);
        mUserGuideView.setTipView(mTipTextView,600,200);
        mUserGuideView.setHighLightView(targetView);
    }

    private void showUserGuideView(int index){
        switch (index){
            case Constant.ZERO_OF_INTEGER:
                showUserGuideView(mTopTabView,getString(R.string.search_mode_swich_tips));
                break;
            case Constant.ONE_OF_INTEGER:
                showUserGuideView(mCustomViewPager,getString(R.string.app_operation_tips));
                break;
            case Constant.TWO_OF_INTEGER:
                if(getActivity() instanceof  MainActivity){
                    showUserGuideView(mUserGuideView,getString(R.string.menu_operation_tips));
                }
                break;
            default:
                SettingsHelper.getInstance().setUserGuideTips(false);
                break;
        }

        return;
    }

    private void refreshUserGuideView(){
        if (true == SettingsHelper.getInstance().isUserGuideTips()){
            ViewUtil.showView(mUserGuideView);
        }else {
            ViewUtil.hideView(mUserGuideView);
        }
    }

    private void refreshDataCountTv() {
        if(true==SettingsHelper.getInstance().isSearchDataCountShow()){
            mDataCountTv.setText(getContext().getString(R.string.data_count,getDataCount()));
            ViewUtil.showView(mDataCountTv);
        }else {
            ViewUtil.hideView(mDataCountTv);
        }

    }

    private void refreshShareIv(){
        if(true==SettingsHelper.getInstance().isShareShow()){
            ViewUtil.showView(mShareIv);
        }else {
            ViewUtil.hideView(mShareIv);
        }
    }

    private List<AppDownloadInfo> loadAppDownloadInfo(){
        String[] appDownloadInfoItems=getContext().getResources().getStringArray(R.array.app_download_info);

        List<AppDownloadInfo> appDownloadInfos=new ArrayList<AppDownloadInfo>();

        for(String item:appDownloadInfoItems){
            try {
                JSONObject jsonObject=new JSONObject(item);
                if(null!=jsonObject){
                    AppDownloadInfo appDownloadInfo=new AppDownloadInfo();
                    if(jsonObject.has(AppDownloadInfo.KEY_ID)){
                        appDownloadInfo.setId(jsonObject.getInt(AppDownloadInfo.KEY_ID));
                    }

                    if(jsonObject.has(AppDownloadInfo.KEY_APP_MARKET)){
                        appDownloadInfo.setAppMarket(jsonObject.getString(AppDownloadInfo.KEY_APP_MARKET));
                    }

                    if(jsonObject.has(AppDownloadInfo.KEY_DOWNLOAD_ADDRESS)){
                        appDownloadInfo.setDownloadAddress(jsonObject.getString(AppDownloadInfo.KEY_DOWNLOAD_ADDRESS));
                    }

                    appDownloadInfos.add(appDownloadInfo);

                }


            } catch (JSONException e) {
                LogUtil.i(TAG, TimeUtil.getLogTime()+e.getMessage());
                e.printStackTrace();
            }


        }
        return appDownloadInfos;
    }
}
