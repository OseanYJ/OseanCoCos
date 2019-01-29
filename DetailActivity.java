package com.hundsun.onlinepurchase.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.hundsun.R;
import com.hundsun.abs.param.BaseJSONObject;
import com.hundsun.base.annotation.InjectView;
import com.hundsun.base.util.ToastUtil;
import com.hundsun.bridge.activity.HundsunBridgeActivity;
import com.hundsun.bridge.contants.BridgeContants;
import com.hundsun.bridge.contants.BundleDataContants;
import com.hundsun.bridge.contants.DoctorActionContants;
import com.hundsun.bridge.contants.PrescriptionActionContants;
import com.hundsun.bridge.dialog.ProvideAndInsuredDialog;
import com.hundsun.bridge.enums.DrugItemViewType;
import com.hundsun.bridge.enums.OrderStatusEnums;
import com.hundsun.bridge.enums.PurchaseBizType;
import com.hundsun.bridge.util.ImageUtils;
import com.hundsun.bridge.util.StringUtil;
import com.hundsun.core.app.Ioc;
import com.hundsun.core.util.Handler_String;
import com.hundsun.net.listener.IHttpRequestListener;
import com.hundsun.netbus.request.OnlinepurchaseRequestManager;
import com.hundsun.netbus.response.onlinepurchase.OnlinePurchaseDrugStore;
import com.hundsun.netbus.response.onlinepurchase.OnlinePurchaseMatchResultRes;
import com.hundsun.netbus.response.onlinepurchase.OnlinePurchaseOltDrugMatchRes;
import com.hundsun.netbus.response.onlinepurchase.OnlinePurchaseProviderVoRes;
import com.hundsun.netbus.response.onlinepurchase.OnlinePurchaseRecordDetailRes;
import com.hundsun.netbus.response.onlinepurchase.OnlinePurchaseShopDrugDeliveryRes;
import com.hundsun.onlinepurchase.adapter.DrugListAdapter;
import com.hundsun.onlinepurchase.fragment.OnlinePurchaseDetailHeaderFragment;
import com.hundsun.onlinepurchase.fragment.OnlinePurchaseGetDrugBySelfFragment;
import com.hundsun.onlinepurchase.util.OnlinePurchaseUtil;
import com.hundsun.onlinepurchase.view.DrugListFooterView;
import com.hundsun.onlinetreatment.util.OnlineChatUtil;
import com.nostra13.universalimageloader.core.DisplayImageOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @Description: 网上购药详情
 * @ClassName: OnlinePurchaseDetailActivity.java
 * @Package: com.hundsun.prescription.app.activity
 * @Author: zhongyj@hsyuntai.com
 * @Date: 2016年05月03日 上午10:52
 * <ModifyLog>
 * @ModifyContent:
 * @Author:
 * @Date: </ModifyLog>
 */
public class DetailActivity extends HundsunBridgeActivity implements IHttpRequestListener<OnlinePurchaseRecordDetailRes>, View.OnClickListener {
    @InjectView
    private Toolbar hundsunToolBar;
    @InjectView
    private ListView listView;
    private DrugListAdapter<OnlinePurchaseMatchResultRes> adapter;


    //开发票
    private LinearLayout insuredLL;
    //发票信息页面
    private View invidTitleLayout;
    private TextView olpCreateTimeTV,
    // 发票类型|发票抬头|纳税人|发票内容|订单编号|复制|开发票|保价文字说明|联系药商
    invidTypeTV, invidTitleTV, invidNoTV, invidGoodsDetailTV, mOlpOrderNoTV, mOlpCopyOrderTV,
    //实付的金额|药店咨询|联系客服|联系客服|药店咨询|支付完成的保价介绍|用药咨询|展开|预计送达时间
    realPayCountTV, mContractServiceTV, mConsultTV, mMedicalConsTV, checkPurchaseTV, freightTV;
    private View checkPurchaseDivideLine, freightLayout, consultingDividerV;
    //商品总价|医保|优惠券|运费
    private DrugListFooterView totalDLF, healthDLF, couponDLF;

    //处方单ID
    private long pscriptId, orderNo;
    private CheckBox insuredCheckBox;
    private DrugItemViewType drugItemViewType;
    private OnlinePurchaseRecordDetailRes detailRes;
    //药商和保价说明的通用Dialog
    private ProvideAndInsuredDialog mProvideAndInsuredDialog;

    @Override
    protected void getIntentData(Intent intent) {
        // 处方单ID
        pscriptId = intent.getLongExtra(BundleDataContants.BUNDLE_DATA_PSCRIPT_ID, BridgeContants.HUNDSUN_INT_NULL);
        orderNo = intent.getLongExtra(BundleDataContants.BUNDLE_DATA_ORDER_FEE_FLAG, BridgeContants.HUNDSUN_INT_NULL);
        drugItemViewType = (DrugItemViewType) intent.getSerializableExtra(DrugItemViewType.class.getName());
    }

    @Override
    protected int getLayoutId() {
        return R.layout.hundsun_activity_prescription_detail_a1;
    }

    @Override
    protected void initLayout() {
        setToolBar(hundsunToolBar);
        setTitle(R.string.hundsun_prescription_goods_detail_label);
        initWholeView();
        getOnlinePurchaseDetail();
    }

    @Override
    protected Bundle getFragmentBundle(int layoutId, int fragmentClassResId) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(OnlinePurchaseRecordDetailRes.class.getName(), detailRes);
        bundle.putSerializable(DrugItemViewType.class.getName(), drugItemViewType);
        if (detailRes != null) {
            bundle.putParcelable(OnlinePurchaseProviderVoRes.class.getName(),detailRes.getProvider());
        }
        return bundle;
    }

    /**
     * @Description: 获取网上购药记录详情
     * @Author: zhongyj@hsyuntai.com
     * @Date: 16/5/3 下午3:38
     */
    private void getOnlinePurchaseDetail() {
        startProgress();
        Long rPId = pscriptId == BridgeContants.HUNDSUN_INT_NULL ? null : pscriptId;
        Long rONo = orderNo == BridgeContants.HUNDSUN_INT_NULL ? null : orderNo;
        OnlinepurchaseRequestManager.getOnlinePurchaseDetail(this, rPId, rONo, this);
    }

    @Override
    public void onSuccess(OnlinePurchaseRecordDetailRes data, List<OnlinePurchaseRecordDetailRes> dataList, String html) {
        endProgress();
        if (data != null) {
            detailRes = data;
            OnlinePurchaseOltDrugMatchRes match = data.getMatch();
            if (match != null && match.getProvider() == null) {
                match.setProvider(data.getProvider());
            }
            setViewByStatus(SUCCESS_VIEW);
            initListView();
            displaySuccessView();

        } else {
            setViewByStatus(EMPTY_VIEW);
        }
    }


    /**
     * @Description: 初始化listview
     * @Author: zhongyj@hsyuntai.com
     * @Date: 16/4/1 上午9:22
     */
    private void initListView() {
        initHeaderView();
        addTotalFooterView();
        // 设置图片加载配置
        final DisplayImageOptions options = ImageUtils.createDisplayImageOptions(R.drawable.hundsun_app_small_image_loading,
                R.drawable.hundsun_onlinepurchase_default_drug, R.drawable.hundsun_onlinepurchase_default_drug);
        OnlinePurchaseOltDrugMatchRes druggist = detailRes.getMatch();
        ArrayList<OnlinePurchaseMatchResultRes> psIds = druggist == null ? null : druggist.getPsIds();
        PurchaseBizType bizType = getPurchaseBizType();
        adapter = new DrugListAdapter<OnlinePurchaseMatchResultRes>(options, psIds, drugItemViewType, bizType, detailRes != null, druggist);
        listView.setAdapter(adapter);
    }

    /**
     * @Description: 添加总计的footerview
     * @Author: zhongyj@hsyuntai.com
     * @Date: 16/4/1 下午1:35
     */
    private void addTotalFooterView() {
        View footerView = LayoutInflater.from(this).inflate(R.layout.hundsun_include_onlinepurchase_shop_delivery_a1, null);
        String createTime = detailRes.getCreateTime();
        footerView.findViewById(R.id.olpCreateTimeTV).setVisibility(TextUtils.isEmpty(createTime) ? View.GONE : View.VISIBLE);
        olpCreateTimeTV = (TextView) footerView.findViewById(R.id.olpCreateTimeTV);
        olpCreateTimeTV.setText(new StringBuilder(getResources().getString(R.string.hundsun_onlinepurchase_create_time_hint)).append(createTime));
        insuredCheckBox = footerView.findViewById(R.id.insuredCheckBox);
        //实付
        realPayCountTV = (TextView) footerView.findViewById(R.id.realPayCountTV);
        insuredLL = (LinearLayout) footerView.findViewById(R.id.hadInsuredLL);
        mContractServiceTV = (TextView) footerView.findViewById(R.id.contractServiceTV);
        mConsultTV = (TextView) footerView.findViewById(R.id.consultingTV);
        consultingDividerV = footerView.findViewById(R.id.consultingDividerV);
        //用药咨询
        mMedicalConsTV = (TextView) footerView.findViewById(R.id.medicalConsTV);
        checkPurchaseTV = (TextView) footerView.findViewById(R.id.checkPurchaseTV);
        checkPurchaseDivideLine = footerView.findViewById(R.id.checkPurchaseDivideLine);

        //订单编号
        mOlpOrderNoTV = (TextView) footerView.findViewById(R.id.olpOrderNoTV);
        //复制
        mOlpCopyOrderTV = (TextView) footerView.findViewById(R.id.olpCopyOrderTV);
        //发票详情相关
        invidTypeTV = (TextView) footerView.findViewById(R.id.invidTypeTV);
        invidTitleTV = (TextView) footerView.findViewById(R.id.invidTitleTV);
        invidTitleLayout = footerView.findViewById(R.id.invidTitleLayout);
        invidNoTV = (TextView) footerView.findViewById(R.id.invidNoTV);
        invidGoodsDetailTV = (TextView) footerView.findViewById(R.id.invidGoodsDetailTV);
        couponDLF = (DrugListFooterView) footerView.findViewById(R.id.couponDLF);
        freightLayout = footerView.findViewById(R.id.freightLayout);
        freightTV = footerView.findViewById(R.id.freightTV);
        healthDLF = (DrugListFooterView) footerView.findViewById(R.id.healthDLF);
        totalDLF = (DrugListFooterView) footerView.findViewById(R.id.totalDLF);
        listView.addFooterView(footerView);
    }


    private void initHeaderView(){
        if (this.detailRes == null) {
            return;
        }
        View headerView = LayoutInflater.from(this).inflate(R.layout.hundsun_headerview_onlinepurchase_order_list, null);
        listView.addHeaderView(headerView);
        View dividerLine = new View(this);
        dividerLine.setBackgroundResource(R.color.hundsun_app_color_bg);
        AbsListView.LayoutParams params = new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, getResources().getDimensionPixelSize(R.dimen.hundsun_dimen_middle_spacing));
        dividerLine.setLayoutParams(params);
        listView.addHeaderView(dividerLine);

        OnlinePurchaseOltDrugMatchRes druggist = detailRes.getMatch();
        FragmentManager fragmentManager = this.getSupportFragmentManager();
        Fragment fragment;
        if (detailRes.getPayMode() > 0) {
            OnlinePurchaseDrugStore drugStore = detailRes.getDrugStore();
            String reminder = drugStore == null ? null : drugStore.getReminder();
            fragment = new OnlinePurchaseGetDrugBySelfFragment();
            Bundle bundle = new Bundle();
            bundle.putParcelable(OnlinePurchaseOltDrugMatchRes.class.getName(), druggist);
            bundle.putString(BundleDataContants.BUNDLE_DATA_REMINDER, reminder);
            bundle.putParcelable(OnlinePurchaseRecordDetailRes.class.getName(), this.detailRes);
            fragment.setArguments(bundle);
        } else {
            fragment = new OnlinePurchaseDetailHeaderFragment();
            Bundle bundle = new Bundle();
            bundle.putBoolean(BundleDataContants.BUNDLE_DATA_ADD_ADDRESS, false);
            bundle.putParcelable(OnlinePurchaseShopDrugDeliveryRes.class.getName(), detailRes.getDelivery());
            bundle.putParcelable(BundleDataContants.BUNDLE_DATA_RECV_ADDR_PARCEL, detailRes.getAddress());
            String reminder = druggist == null ? null : druggist.getReminder();
            bundle.putString(BundleDataContants.BUNDLE_DATA_REMINDER, reminder);
            bundle.putBoolean(BundleDataContants.BUNDLE_DATA_IS_FINISH_PAY,true);
            bundle.putParcelable(OnlinePurchaseRecordDetailRes.class.getName(),detailRes);
            fragment.setArguments(bundle);
        }
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.framelayout, fragment);
        fragmentTransaction.commit();

    }


    private void displaySuccessView() {
        Integer payMode = detailRes.getPayMode();
        if (payMode != null && payMode > 0) {
            //到店取药 线上和线下共性配置
            insuredLL.setVisibility(View.INVISIBLE);
            mConsultTV.setVisibility(View.GONE);
            consultingDividerV.setVisibility(View.GONE);
            //不显示优惠券、运费、医保等字段
            if (freightLayout != null && healthDLF != null) {
                if (payMode >= 1) {
                    freightLayout.setVisibility(View.GONE);
                    healthDLF.setVisibility(View.GONE);
                    if (payMode == 2) {
                        couponDLF.setVisibility(View.GONE);
                    }
                }
            }
        }
        if (detailRes.getInvoiceInfo() != null) {
            showInvidView();
        }
        Integer status = detailRes == null ? null : detailRes.getDeliveryStatus();
        if (payMode != null && payMode == 0 && status != null && OrderStatusEnums.HaveSign.statusCode != status) {
            //预计送达时间
            adapter.setDeliveryRes(detailRes.getDelivery());
        }
        //联系药商
        mConsultTV.setText(OnlinePurchaseUtil.getSpannableInfo(this, R.string.hundsun_prescription_consult_drug, R.drawable.hundsun_online_purchase_contact_business));
        setCheckPurchaseButtonVisibility();
        disPlayView(detailRes.getDelivery());
    }

    /**
     * @param deliveryRes 快递信息
     * @Description: 展示字段信息
     * @Author: zhongyj@hsyuntai.com
     * @Date: 16/5/3 上午11:16
     */
    @SuppressLint("SetTextI18n")
    private void disPlayView(OnlinePurchaseShopDrugDeliveryRes deliveryRes) {
        String moneyUnit = StringUtil.getMoneyUnit();
        // 运费
        Double deliveryFee = null;
        if (deliveryRes != null) {
            String deliveryFeeStr = deliveryRes.getDeliveryFee();
            try {
                deliveryFee = TextUtils.isEmpty(deliveryFeeStr) ? 0d : Double.valueOf(deliveryFeeStr);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        OnlinePurchaseOltDrugMatchRes druggist = detailRes.getMatch();
        double totalFee = detailRes.getTotalFee();
        // 合计数据
        if (totalFee <= 0) {
            String drugTotalSelfFee = druggist == null ? "0" : druggist.getDrugTotalSelfFee();
            double totalFeeTemp = Handler_String.isBlank(drugTotalSelfFee) ? 0d : Double.parseDouble(drugTotalSelfFee);
            totalFee = totalFeeTemp + (deliveryFee == null ? 0d : deliveryFee);
        }

        //总计的费用
        totalDLF.setRightText(new StringBuilder().append(moneyUnit).append(Handler_String.keepDecimal(Math.abs(totalFee), 2)));
        totalDLF.setRightColor(getResources().getColor(R.color.hundsun_app_color_red));

        boolean isExpressType = druggist.getExpressType() == 1;
        //康发的显示运费到付 和产品确认之后的
        //其他的显示具体金额，金额0时显示0.00
        OnlinePurchaseUtil.setDeliveryCost(freightTV, deliveryFee, isExpressType);

        Double healFee = null;
        // 医保
        String drugTotalHealFee = druggist.getDrugTotalHealFee();
        try {
            healFee = Double.parseDouble(drugTotalHealFee);
        } catch (Throwable e) {
            Ioc.getIoc().getLogger().e(e);
        }

        double couponFee = detailRes.getCouponFee();
        if (couponDLF != null && couponFee != BridgeContants.HUNDSUN_INT_NULL) {
            StringBuilder couponFeeStr = new StringBuilder().append("-").append(moneyUnit).append(Handler_String.keepDecimal(couponFee, 2));
            couponDLF.setRightText(couponFeeStr);
            //优惠券为0 的时候 整行不显示
            if (couponFee == 0.00d) {
                couponDLF.setVisibility(View.GONE);
            }
        }

        //是否保价
        OnlinePurchaseUtil.isNeedInsurance(this, detailRes, insuredLL, insuredCheckBox);

        //设置实付
        if (detailRes != null && realPayCountTV != null) {
            double finalRealPay = (totalFee - (healFee == null ? 0d : healFee) + (deliveryFee == null ? 0 : deliveryFee) - (couponFee <= 0 ? 0 : couponFee));
            //实付=商品总价+运费-医保-优惠券
            if (finalRealPay <= 0) {
                finalRealPay = 0d;
            }
            setSelfPayCost(realPayCountTV, getString(R.string.hundsun_onlinepurchase_true_fee_hint), finalRealPay);
            //针对到店取药付款 覆盖原来的值
            if (detailRes != null && 2 == detailRes.getPayMode()) {
                realPayCountTV.setText(getResources().getString(R.string.hundsun_online_pay_in_store));
            }
        }

        if (healFee == null && healthDLF != null) {
            healthDLF.setRightText("-");
        } else if (healthDLF != null) {
            StringBuilder healthFeeStr = new StringBuilder().append("-").append(moneyUnit).append(Handler_String.keepDecimal(healFee, 2));
            healthDLF.setRightText(healthFeeStr);
        }

        if (detailRes != null && druggist != null) {
            //已支付状态下医保显示0.0显示-¥0.00  其余显示正常的价格
            String totalHealFee = druggist.getDrugTotalHealFee();
            if (getResources().getString(R.string.hundsun_onlinepurchase_no_count).equals(totalHealFee)) {
                healthDLF.setVisibility(View.GONE);
            } else {
                StringBuilder healthFeeSB = new StringBuilder().append("-").append(moneyUnit).append(totalHealFee);
                healthDLF.setRightText(healthFeeSB);
            }
        }
        if (detailRes != null) {
            //设置订单编号
            mOlpOrderNoTV.setText(getString(R.string.hundsun_online_purchase_order_no) + detailRes.getOrderNo());
        }

        List<TextView> viewList = Arrays.asList(
                mContractServiceTV,
                mConsultTV,
                mMedicalConsTV,
                mOlpCopyOrderTV,
                checkPurchaseTV);
        for (View view : viewList) {
            if (view != null) {
                view.setOnClickListener(this);
            }
        }

    }

    /**
     * @param textView 设置文本的文本 小计或者实付
     * @param feeName  费用的名称
     * @param cost     费用值
     */
    private void setSelfPayCost(TextView textView, String feeName, double cost) {
        String selfCostHint = feeName + "：";
        StringBuilder selfCostText = new StringBuilder().append(selfCostHint).append(StringUtil.getMoneyUnit())
                .append(Handler_String.keepDecimal(Math.abs(cost), 2));
        SpannableString span = new SpannableString(selfCostText);
        int start = selfCostHint.length();
        int end = selfCostText.length();
        span.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.hundsun_color_text_red_common)), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        span.setSpan(new AbsoluteSizeSpan((int) getResources().getDimension(R.dimen.hundsun_dimen_middle_text)), start + 1, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        textView.setText(span);
    }

    /**
     * @Description: 设置查看处方按钮的显示与隐藏
     * @Author: zhongyj@hsyuntai.com
     * @Date: 2018/5/24 下午6:34
     */
    private void setCheckPurchaseButtonVisibility() {
        int visibility = getPurchaseBizType() == PurchaseBizType.Prescription ? View.VISIBLE : View.GONE;
        checkPurchaseTV.setVisibility(visibility);
        checkPurchaseDivideLine.setVisibility(visibility);
    }

    /**
     * @Description: 获取bizType
     * @Author: zhongyj@hsyuntai.com
     * @Date: 2018/5/24 下午6:37
     */
    @Nullable
    private PurchaseBizType getPurchaseBizType() {
        PurchaseBizType bizType = null;
        try {
            OnlinePurchaseOltDrugMatchRes druggist = detailRes.getMatch();
            bizType = PurchaseBizType.getPurchaseBizTypeByCode(druggist.getBizType());
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return bizType;
    }

    /**
     * @Description:发票信息填充
     * @Author:zhoub@hsyuntai.com
     * @Date: 2018/2/6 17:16
     * @Params:
     */
    @SuppressLint("SetTextI18n")
    private void showInvidView() {
        com.hundsun.onlinepurchase.util.OnlinePurchaseUtil.showInvidView(
                this,
                detailRes.getInvoiceInfo(),
                invidTypeTV,
                invidTitleTV,
                invidNoTV,
                invidGoodsDetailTV,
                invidTitleLayout);
    }

    @Override
    public void onFail(String kind, String errMsg) {
        endProgress();
        View view = setViewByStatus(FAIL_VIEW);
        if (view != null) {
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getOnlinePurchaseDetail();
                }
            });
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mContractServiceTV) {
            //联系客服
            OnlinePurchaseUtil.showServiceDialog(this, detailRes, detailRes.getMatch(), detailRes.getProvider());
        } else if (v == mConsultTV) {
            //药店咨询
            if (detailRes != null) {
                OnlinePurchaseUtil.goToCallPhoneActivity(this, detailRes.getHotline());
            }
        } else if (v == mMedicalConsTV) {
            //跳转医生主页
            goDoctorDetail();
        } else if (v == mOlpCopyOrderTV) {
            //复制订单编号
            copyOrderNo(detailRes);
        } else if (v == checkPurchaseTV) {
            // 查看处方单
            startToPrescriptionDetail();
        }
    }


    /**
     * 跳转医生主页
     */
    private void goDoctorDetail() {
        BaseJSONObject params = new BaseJSONObject();
        params.put(BundleDataContants.BUNDLE_DATA_DOCTOR_ID, detailRes.getDocId());
        openNewActivity(DoctorActionContants.ACTION_DOCTOR_DETAIL.val(), params);
    }

    /**
     * 复制订单号
     *
     * @param detailRes
     */
    private void copyOrderNo(@NonNull OnlinePurchaseRecordDetailRes detailRes) {
        try {
            OnlineChatUtil.copyText(this, String.valueOf(detailRes.getOrderNo()));
        } catch (Exception e) {
            Ioc.getIoc().getLogger().e(e);
        }
        ToastUtil.showCustomToast(this, getResources().getString(R.string.hundsun_online_purchase_copy_success));
    }

    /**
     * @Description: 跳转到处方单详情页面
     * @Author: zhongyj@hsyuntai.com
     * @Date: 16/6/14 上午10:22
     */
    private void startToPrescriptionDetail() {
        OnlinePurchaseOltDrugMatchRes druggist = detailRes.getMatch();
        ArrayList<OnlinePurchaseMatchResultRes> psIds = druggist == null ? null : druggist.getPsIds();
        if (psIds == null) {
            return;
        }
        Intent intentJson = new Intent(PrescriptionActionContants.ACTION_PRESCRIPTION_ITEM_DETAIL.val());

        String accessPatId = null;
        for (OnlinePurchaseMatchResultRes matchResultRes : psIds) {
            if (accessPatId == null) {
                accessPatId = matchResultRes.getAccessPatId();
            }
            if (accessPatId != null) {
                break;
            }
        }
        intentJson.putStringArrayListExtra(BundleDataContants.BUNDLE_DATA_PSCRIPT_ACCESSPRESCRIPT_ID, (ArrayList<String>) detailRes.getAccessPrescriptIds());
        // 医院ID
        intentJson.putExtra(BundleDataContants.BUNDLE_DATA_HOSPITAL_ID, druggist.getHosId());
        // 患者ID
        intentJson.putExtra(BundleDataContants.BUNDLE_DATA_PATIENT_ID, druggist.getPatId());
        // 对接患者ID
        intentJson.putExtra(BundleDataContants.BUNDLE_DATA_ACCESSPAT_ID, accessPatId);
        try {
            startActivity(intentJson);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }


}
