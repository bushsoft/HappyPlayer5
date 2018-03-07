package com.zlm.hp.ui.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.zlm.hp.R;
import com.zlm.hp.adapter.TabFragmentAdapter;
import com.zlm.hp.application.HPApplication;
import com.zlm.hp.net.api.DownloadLyricsUtil;
import com.zlm.hp.net.api.SearchLyricsUtil;
import com.zlm.hp.net.entity.DownloadLyricsResult;
import com.zlm.hp.net.entity.SearchLyricsResult;
import com.zlm.hp.model.AudioInfo;
import com.zlm.hp.model.AudioMessage;
import com.zlm.hp.receiver.AudioBroadcastReceiver;
import com.zlm.hp.ui.fragment.LrcFragment;
import com.zlm.hp.ui.widget.transformer.ZoomOutPageTransformer;

import java.util.ArrayList;
import java.util.List;

import base.utils.ThreadUtil;
import base.utils.ToastUtil;
import base.widget.IconfontTextView;

/**
 * 歌词搜索窗口
 */
public class SearchLrcActivity extends BaseActivity {
    /**
     * 歌曲名称
     */
    private EditText mSongNameEditText;
    private IconfontTextView mSongNameCleanImg;

    /**
     * 歌手名称
     */
    private EditText mSingerNameEditText;
    private IconfontTextView mSingerNameCleanImg;
    private TextView mSearchBtn;
    //
    private String mDuration = "";
    private String mHash = "";
    private AudioInfo mCurAudioInfo;
    private boolean mResetLrcViewFlag = false;
    //
    private final int LOADDATA = 0;
    private final int INITDATA = 1;
    private final int SHOWLOADINGVIEW = 2;
    private final int SHOWCONTENTVIEW = 3;

    private List<DownloadLyricsResult> mDatas;
    private ArrayList<Fragment> mLrcViews;
    private ViewPager mViewPager;

    //
    private TextView mSumTv;
    private TextView mCurIndexTv;

    //
    /**
     * 加载中布局
     */
    private RelativeLayout mLoadingContainer;

    /**
     * 内容布局
     */
    private RelativeLayout mContentContainer;


    /**
     *
     */
    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case LOADDATA:
                    loadDataUtil(500);
                    break;
                case INITDATA:
                    initData();
                    break;
                case SHOWCONTENTVIEW:
                    showContentViewHandler();
                    break;
                case SHOWLOADINGVIEW:
                    showLoadingViewHandler();
                    break;
            }
        }
    };
    /**
     * 音频广播
     */
    private AudioBroadcastReceiver mAudioBroadcastReceiver;

    /**
     * 广播监听
     */
    private AudioBroadcastReceiver.AudioReceiverListener mAudioReceiverListener = new AudioBroadcastReceiver.AudioReceiverListener() {
        @Override
        public void onReceive(Context context, Intent intent) {
            doAudioReceive(context, intent);
        }
    };

    private TabFragmentAdapter adapter;
    private Runnable runnable;

    @Override
    protected int setContentViewId() {
        return R.layout.activity_search_lrc;
    }

    @Override
    protected boolean isAddStatusBar() {
        return true;
    }

    @Override
    public int setStatusBarParentView() {
        return 0;
    }

    @Override
    protected void initViews(Bundle savedInstanceState) {
        TextView titleView = findViewById(R.id.title);
        titleView.setText(mContext.getString(R.string.choose_lyrics));


        //关闭
        IconfontTextView backTextView = findViewById(R.id.closebtn);
        backTextView.setOnClickListener(new View.OnClickListener()

        {
            @Override
            public void onClick(View view) {

                //关闭输入法
                InputMethodManager im = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                im.hideSoftInputFromWindow(getCurrentFocus().getApplicationWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

                ThreadUtil.runInThread(new Runnable() {
                    @Override public void run() {
                        try { Thread.sleep(200);  } catch (InterruptedException e) {  e.printStackTrace();  }
                        finish();
                    }  });


            }
        });
        //歌曲
        mSongNameEditText = findViewById(R.id.songNameEt);
        mSongNameCleanImg = findViewById(R.id.songclean_img);
        mSongNameCleanImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mSongNameEditText.setText("");
                mSongNameEditText.requestFocus();

            }
        });
        mSongNameEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {

                    //关闭输入法
                    InputMethodManager im = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    im.hideSoftInputFromWindow(getCurrentFocus().getApplicationWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);


                    doSearch();
                }
                return false;
            }
        });
        mSongNameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String searchKey = mSongNameEditText.getText().toString();
                if (searchKey == null || searchKey.equals("")) {
                    if (mSongNameCleanImg.getVisibility() != View.INVISIBLE) {
                        mSongNameCleanImg.setVisibility(View.INVISIBLE);
                    }
                } else {
                    if (mSongNameCleanImg.getVisibility() != View.VISIBLE) {
                        mSongNameCleanImg.setVisibility(View.VISIBLE);
                    }
                }
            }
        });
        //歌手
        mSingerNameEditText = findViewById(R.id.singerNameEt);
        mSingerNameCleanImg = findViewById(R.id.singclean_img);
        mSingerNameCleanImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mSingerNameEditText.setText("");
                mSingerNameEditText.requestFocus();

            }
        });
        mSingerNameEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {

                    //关闭输入法
                    InputMethodManager im = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    im.hideSoftInputFromWindow(getCurrentFocus().getApplicationWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);


                    doSearch();
                }
                return false;
            }
        });
        mSingerNameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String searchKey = mSingerNameEditText.getText().toString();
                if (searchKey == null || searchKey.equals("")) {
                    if (mSingerNameCleanImg.getVisibility() != View.INVISIBLE) {
                        mSingerNameCleanImg.setVisibility(View.INVISIBLE);
                    }
                } else {
                    if (mSingerNameCleanImg.getVisibility() != View.VISIBLE) {
                        mSingerNameCleanImg.setVisibility(View.VISIBLE);
                    }
                }
            }
        });
        //搜索按钮
        mSearchBtn = findViewById(R.id.searchbtn);
        mSearchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doSearch();
            }
        });

        //
        mViewPager = findViewById(R.id.viewpage);
        mViewPager.setPageTransformer(true, new ZoomOutPageTransformer());
        mViewPager.setOffscreenPageLimit(0);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {

                mCurIndexTv.setText((position + 1) + "");
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        //
        mSumTv = findViewById(R.id.sum);
        mSumTv.setText("0");
        mCurIndexTv = findViewById(R.id.cur_index);
        mCurIndexTv.setText("0");
        //
        mLoadingContainer = findViewById(R.id.loading);
        //
        mContentContainer = findViewById(R.id.content);

        //注册接收音频播放广播
        mAudioBroadcastReceiver = new AudioBroadcastReceiver(getApplicationContext());
        mAudioBroadcastReceiver.setAudioReceiverListener(mAudioReceiverListener);
        mAudioBroadcastReceiver.registerReceiver(getApplicationContext());


    }

    /**
     * 显示加载窗口
     */
    public void showLoadingView() {
        mHandler.sendEmptyMessage(SHOWLOADINGVIEW);
    }

    /**
     * 显示加载窗口
     */
    private void showLoadingViewHandler() {
        mContentContainer.setVisibility(View.GONE);
        mLoadingContainer.setVisibility(View.VISIBLE);
    }

    /**
     * 显示主界面
     */
    public void showContentView() {
        mHandler.sendEmptyMessage(SHOWCONTENTVIEW);
    }

    /**
     * 显示主界面
     */
    private void showContentViewHandler() {
        mContentContainer.setVisibility(View.VISIBLE);
        mLoadingContainer.setVisibility(View.GONE);
    }

    /**
     * 初始化数据
     */
    private void initData() {
        mCurAudioInfo = HPApplication.getInstance().getCurAudioInfo();
        if (mCurAudioInfo != null) {
            mSongNameEditText.setText(mCurAudioInfo.getSongName());
            mSingerNameEditText.setText(mCurAudioInfo.getSingerName());
            mDuration = mCurAudioInfo.getDuration() + "";
            mHash = mCurAudioInfo.getHash();

            loadDataUtil(500);
        }

    }


    @Override
    protected void loadData(boolean isRestoreInstance) {
        mHandler.sendEmptyMessage(INITDATA);
    }

    /**
     * 搜索歌词
     */
    private void doSearch() {
        String songName = mSongNameEditText.getText().toString();
        String singerName = mSingerNameEditText.getText().toString();
        if (songName.equals("") && singerName.equals("")) {
            ToastUtil.showTextToast(getApplicationContext(), mContext.getString(R.string.please_enter_keyword));
            return;
        }

        loadDataUtil(500);
    }

    /**
     * @param sleepTime
     */
    private void loadDataUtil(int sleepTime) {
        mSumTv.setText("0");
        mCurIndexTv.setText("0");
        showLoadingView();
        if (mDatas == null) {
            mLrcViews = new ArrayList<Fragment>();
            mDatas = new ArrayList<DownloadLyricsResult>();
        } else {

            if (mLrcViews.size() > 0) {
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                for (int i = 0; i < mLrcViews.size(); i++) {
                    transaction.remove(mLrcViews.get(i));
                }
                transaction.commit();
            }

            mLrcViews.clear();
            mDatas.clear();
        }

        if(runnable != null) {
            ThreadUtil.cancelThread(runnable);
        }
        runnable = new Runnable() {
            @Override
            public void run() {
                String songName = mSongNameEditText.getText().toString();
                String singerName = mSingerNameEditText.getText().toString();
                //加载歌词
                String keyWords = "";
                if (singerName.equals(mContext.getString(R.string.unknown))) {
                    keyWords = songName;
                } else {
                    keyWords = singerName + " - " + songName;
                }

                //获取歌曲列表
                List<SearchLyricsResult> results = SearchLyricsUtil.searchLyrics(mContext, keyWords, mDuration, "");
                if (results != null && results.size() > 0) {
                    for (int i = 0; i < results.size(); i++) {
                        SearchLyricsResult searchLyricsResult = results.get(i);
                        if (searchLyricsResult != null) {
                            DownloadLyricsResult downloadLyricsResult = DownloadLyricsUtil.downloadLyrics(mContext, searchLyricsResult.getId(), searchLyricsResult.getAccesskey(), "krc");
                            if (downloadLyricsResult != null) {
                                mDatas.add(downloadLyricsResult);
                            }
                        }
                    }
                }

                Runnable runnable = ThreadUtil.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < mDatas.size(); i++) {
                            DownloadLyricsResult downloadLyricsResult = mDatas.get(i);
                            LrcFragment lrcFragment = new LrcFragment(downloadLyricsResult, mCurAudioInfo);
                            mLrcViews.add(lrcFragment);
                        }
                        //
                        if (mLrcViews.size() == 0) {
                            ToastUtil.showTextToast(getApplicationContext(), mContext.getString(R.string.not_lyrics));

                        } else {
                            mCurIndexTv.setText("1");
                        }
                        //
                        mSumTv.setText(mLrcViews.size() + "");
                        adapter = new TabFragmentAdapter(getSupportFragmentManager(), mLrcViews);
                        mViewPager.setAdapter(adapter);


                        showContentView();
                    }
                });
                ThreadUtil.cancelUIThread(runnable);
            }
        };
        ThreadUtil.runInThread(runnable);

    }

    /**
     * 处理音频广播事件
     *
     * @param context
     * @param intent
     */

    private void doAudioReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(AudioBroadcastReceiver.ACTION_SERVICE_PLAYINGMUSIC)) {
            AudioInfo audioInfo = HPApplication.getInstance().getCurAudioInfo();
            if (audioInfo != null && audioInfo.getHash().equals(mHash)) {
                //播放中
                AudioMessage audioMessage = HPApplication.getInstance().getCurAudioMessage();//(AudioMessage) intent.getSerializableExtra(AudioMessage.KEY);
                if (audioMessage != null) {
                    if (mLrcViews != null && mLrcViews.size() > 0) {
                        for (int i = 0; i < mLrcViews.size(); i++) {
                            LrcFragment lrcFragment = (LrcFragment) mLrcViews.get(i);
                            lrcFragment.updateView((int) audioMessage.getPlayProgress(), audioInfo.getHash());
                        }
                    }
                }
            } else {
                if (!mResetLrcViewFlag) {
                    mResetLrcViewFlag = true;
                    if (mLrcViews != null && mLrcViews.size() > 0) {
                        for (int i = 0; i < mLrcViews.size(); i++) {
                            LrcFragment lrcFragment = (LrcFragment) mLrcViews.get(i);
                            lrcFragment.updateView(0, mHash);
                        }
                    }
                }

            }
        } else if (action.equals(AudioBroadcastReceiver.ACTION_LRCUSE)) {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        if(runnable != null) {
            ThreadUtil.cancelThread(runnable);
        }
        //注销广播
        mAudioBroadcastReceiver.unregisterReceiver(getApplicationContext());

        super.onDestroy();
    }


    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.out_to_bottom);
    }

    @Override
    public void setStatusColor(int statusColor) {
        super.setStatusColor(statusColor);
    }
}
