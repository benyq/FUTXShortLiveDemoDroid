package com.tencent.qcloud.xiaoshipin.mainui.list;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.tencent.qcloud.xiaoshipin.R;
import com.tencent.qcloud.xiaoshipin.common.utils.TCConstants;
import com.tencent.qcloud.xiaoshipin.common.widget.DividerGridItemDecoration;
import com.tencent.qcloud.xiaoshipin.play.TCVodPlayerActivity;
import com.tencent.qcloud.xiaoshipin.videoeditor.common.widget.BaseRecyclerAdapter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


/**
 * 短视频列表页面
 * 界面展示使用：GridView+SwipeRefreshLayout
 * 列表数据Adapter：TCLiveListAdapter, TCUGCVideoListAdapter
 * 数据获取接口： TCLiveListMgr
 */
public class TCLiveListFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    public static final int START_LIVE_PLAY = 100;
    private static final String TAG = "TCLiveListFragment";
    private List<TCVideoInfo> mVideoList;
    private RecyclerView mRvVideoList;
    private TCUGCVideoListAdapter mUGCListViewAdapter = null;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    //避免连击
    private long mLastClickTime = 0;
    private View mEmptyView;
    private int mPullIndex;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_videolist, container, false);

        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout_list);
        mSwipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light, android.R.color.holo_orange_light, android.R.color.holo_red_light);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mEmptyView = view.findViewById(R.id.tv_listview_empty);
        mRvVideoList = (RecyclerView) view.findViewById(R.id.main_rv_video_list);
        initVideoListView();
        refreshListView();
        return view;
    }

    @Override
    public void onRefresh() {
        refreshListView();
    }

    private void refreshListView() {
        if (reloadLiveList()) {
            mSwipeRefreshLayout.post(new Runnable() {
                @Override
                public void run() {
                    mSwipeRefreshLayout.setRefreshing(true);
                }
            });
        }
    }

    private boolean reloadLiveList() {
        TCVideoListMgr.getInstance().fetchUGCList(new TCVideoListMgr.Listener() {
            @Override
            public void onVideoList(final int retCode, final ArrayList<TCVideoInfo> result, final int index, final int total, final boolean refresh) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (retCode == 0) {
                                if (mPullIndex == index) {
                                    //更新当前页
                                    mVideoList.clear();
                                } else {
                                    //更新下一页

                                }
                                if (result != null) {
                                    mVideoList.addAll((ArrayList<TCVideoInfo>) result.clone());
                                }
                                if (refresh) {
                                    mUGCListViewAdapter.notifyDataSetChanged();
                                }
                                mPullIndex = index;
                            } else {
                                Toast.makeText(getActivity(), "刷新列表失败", Toast.LENGTH_LONG).show();
                            }
                            mEmptyView.setVisibility(mUGCListViewAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
                            mSwipeRefreshLayout.setRefreshing(false);
                        }
                    });
                }
            }
        });
        return true;
    }

    /**
     * 开始播放视频
     *
     * @param item 视频数据
     */
    private void startLivePlay(final TCVideoInfo item, int position) {
        Intent intent = new Intent(getActivity(), TCVodPlayerActivity.class);
        intent.putExtra(TCConstants.PLAY_URL, item.playurl);
        intent.putExtra(TCConstants.PUSHER_ID, item.userid);
        intent.putExtra(TCConstants.PUSHER_NAME, item.nickname == null ? item.userid : item.nickname);
        intent.putExtra(TCConstants.PUSHER_AVATAR, item.headpic);
        intent.putExtra(TCConstants.COVER_PIC, item.frontcover);
        intent.putExtra(TCConstants.FILE_ID, item.fileid != null ? item.fileid : "");
        intent.putExtra(TCConstants.TCLIVE_INFO_LIST, (Serializable) mVideoList);
        intent.putExtra(TCConstants.TIMESTAMP, item.createTime);
        intent.putExtra(TCConstants.TCLIVE_INFO_POSITION, position);
        startActivityForResult(intent, START_LIVE_PLAY);
    }

    private void initVideoListView() {
        mVideoList = new ArrayList<TCVideoInfo>();
        mUGCListViewAdapter = new TCUGCVideoListAdapter(mVideoList);
        mUGCListViewAdapter.setOnItemClickListener(new BaseRecyclerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                try {
                    if (0 == mLastClickTime || System.currentTimeMillis() - mLastClickTime > 1000) {
                        TCVideoInfo item = mVideoList.get(position);
                        if (item == null) {
                            Log.e(TAG, "live list item is null at position:" + position);
                            return;
                        }
                        startLivePlay(item, position);
                    }
                    mLastClickTime = System.currentTimeMillis();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        RecyclerView.LayoutManager layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        mRvVideoList.setLayoutManager(layoutManager);
        mRvVideoList.addItemDecoration(new DividerGridItemDecoration(getContext()));
        mRvVideoList.setAdapter(mUGCListViewAdapter);

        mEmptyView.setVisibility(mUGCListViewAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }
}