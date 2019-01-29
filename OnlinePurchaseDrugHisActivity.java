package com.hundsun.onlinepurchase.activity;

import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;

import com.hundsun.R;
import com.hundsun.abs.param.BaseJSONObject;
import com.hundsun.base.activity.HundsunBaseActivity;
import com.hundsun.base.annotation.InjectView;
import com.hundsun.base.view.RefreshAndMoreListView;
import com.hundsun.bridge.contants.BridgeListPageContants;
import com.hundsun.bridge.contants.BundleDataContants;
import com.hundsun.bridge.contants.OnlinePurchaseActionContants;
import com.hundsun.bridge.enums.DrugItemViewType;
import com.hundsun.bridge.util.ImageUtils;
import com.hundsun.core.adapter.PagedListDataModel;
import com.hundsun.core.util.Handler_Verify;
import com.hundsun.net.listener.IHttpRequestListener;
import com.hundsun.netbus.request.OnlinepurchaseRequestManager;
import com.hundsun.netbus.response.onlinepurchase.OnlinePurchaseDrugHisListRes;
import com.hundsun.netbus.response.onlinepurchase.OnlinePurchaseDrugOrderRes;
import com.hundsun.onlinepurchase.adapter.DrugListAdapter;
import com.nostra13.universalimageloader.core.DisplayImageOptions;

import java.util.List;

/**
 * @Description: 我的药品订单
 * @ClassName: OnlinePurchaseDrugHisActivity.java
 * @Package: com.hundsun.onlinepurchase.activity
 * @Author: zhongyj@hsyuntai.com
 * @Date: 2016年06月06日 上午10:45
 * <ModifyLog>
 * @ModifyContent:
 * @Author:
 * @Date: </ModifyLog>
 */
public class OnlinePurchaseDrugHisActivity extends HundsunBaseActivity implements PagedListDataModel.PagedListDataHandler
        , IHttpRequestListener<OnlinePurchaseDrugHisListRes>, AdapterView.OnItemClickListener {
    @InjectView
    private Toolbar hundsunToolBar;
    @InjectView
    private RefreshAndMoreListView refreshListView;
    // 列表总数
    private int total;
    private DrugListAdapter<OnlinePurchaseDrugOrderRes> adapter;
    private PagedListDataModel<OnlinePurchaseDrugOrderRes> pagedListDataModel;
    private boolean isRefresh;

    @Override
    protected int getLayoutId() {
        return R.layout.hundsun_activity_common_toolbar_refreshlistview_a1;
    }

    @Override
    protected void initLayout() {
        // 设置ToolBar
        setToolBar(hundsunToolBar);
        initListView();
    }

    /**
     * @Description: 初始化listview
     * @Author: zhongyj@hsyuntai.com
     * @Date: 16/4/1 上午9:22
     */
    private void initListView() {
        if (adapter != null) {
            return;
        }
        // 设置图片加载配置
        final DisplayImageOptions options = ImageUtils.createDisplayImageOptions(R.drawable.hundsun_app_small_image_loading,
                R.drawable.hundsun_onlinepurchase_default_drug, R.drawable.hundsun_onlinepurchase_default_drug);
        pagedListDataModel = new PagedListDataModel<OnlinePurchaseDrugOrderRes>(BridgeListPageContants.PAGE_SIZE_SMALL);
        pagedListDataModel.setPageListDataHandler(this);
        refreshListView.setPagedListDataModel(pagedListDataModel);
        List<OnlinePurchaseDrugOrderRes> dataList = pagedListDataModel.getListPageInfo().getDataList();
        adapter = new DrugListAdapter<OnlinePurchaseDrugOrderRes>(options, dataList, DrugItemViewType.FooterView);
        refreshListView.setAdapter(adapter);
        adapter.setOnItemClickListener(this);
        refreshListView.autoLoadData();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        OnlinePurchaseDrugOrderRes orderRes = (OnlinePurchaseDrugOrderRes) view.getTag();
        BaseJSONObject intentJson = new BaseJSONObject();
        // 处方单ID
        intentJson.put(BundleDataContants.BUNDLE_DATA_PSCRIPT_ID, orderRes.getOrderNo());
        intentJson.put(DrugItemViewType.class.getName(), DrugItemViewType.Prescription);
        // 网上购药处方单,跳到网上购药详情页面
        openNewActivity(OnlinePurchaseActionContants.ACTION_ONLINEPURCHASE_DETAIL.val(), intentJson);
    }

    @Override
    public void loadData(int mNumPerPage, int mPageOff, boolean isRefresh) {
        this.isRefresh = isRefresh;
        OnlinepurchaseRequestManager.getMyDrugOrderList(this, mPageOff, mNumPerPage, this);
    }

    @Override
    public void onSuccess(OnlinePurchaseDrugHisListRes data, List<OnlinePurchaseDrugHisListRes> dataList, String html) {
        if (data!=null && !Handler_Verify.isListTNull(data.getList())) {
            total = data.getTotal();
            pagedListDataModel.addRequestResult(data.getList(), total, isRefresh);
        } else {
            pagedListDataModel.addRequestResult(null, total, isRefresh);
        }
        adapter.notifyDataSetChanged();
        refreshListView.loadMoreFinish(pagedListDataModel.isEmpty(), pagedListDataModel.hasMore());
    }

    @Override
    public void onFail(String kind, String errMsg) {
        pagedListDataModel.addRequestResult(null, total, isRefresh);
        adapter.notifyDataSetChanged();
        pagedListDataModel.loadFail();
        refreshListView.loadMoreFinish(pagedListDataModel.isEmpty(), pagedListDataModel.hasMore());
    }

}
