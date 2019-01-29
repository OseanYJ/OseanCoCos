package com.hundsun.onlinepurchase.activity;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.hundsun.R;
import com.hundsun.abs.param.BaseJSONObject;
import com.hundsun.base.activity.HundsunBaseActivity;
import com.hundsun.base.annotation.InjectView;
import com.hundsun.bridge.contants.BundleDataContants;
import com.hundsun.bridge.contants.OnlinePurchaseActionContants;
import com.hundsun.bridge.contants.OnlinetreatActionContants;
import com.hundsun.bridge.contants.PrescriptionActionContants;
import com.hundsun.bridge.enums.ChatMessageConsType;
import com.hundsun.core.listener.OnClickEffectiveListener;
import com.hundsun.core.util.PixValue;
import com.hundsun.net.listener.IHttpRequestTimeListener;
import com.hundsun.netbus.request.SystemRequestManager;
import com.hundsun.netbus.response.system.PersonalizedParamsRes;
import com.hundsun.netbus.v1.manager.HundsunUserManager;

import java.util.List;

/**
 * @Description: 送药到家入口界面
 * @ClassName: OnlinePurchaseDistEntryActivity.java
 * @Package: com.hundsun.onlinepurchase.activity
 * @Author: zhongyj@hsyuntai.com
 * @Date: 2017/8/14 上午10:53
 * <ModifyLog>
 * @ModifyContent:
 * @Author:
 * @Date: </ModifyLog>
 */
public class OnlinePurchaseDistEntryActivity extends HundsunBaseActivity implements View.OnClickListener {
    @InjectView
    private Toolbar hundsunToolBar;
    @InjectView
    private View attentionsLayout;
    @InjectView
    private TextView distHosBtn, distHosPicture, attentionsTV;

    @Override
    protected int getLayoutId() {
        return R.layout.hundsun_activity_prescription_dist_entry_a1;
    }

    @Override
    protected void initLayout() {
        setToolBar(hundsunToolBar);
        setTitle(R.string.hundsun_dist_title);
        initPrompt();
        distHosBtn.setOnClickListener(this);
        distHosPicture.setOnClickListener(this);
        new PurchaseNoticeDialog(this).show();
    }

    @Override
    public void onClick(View v) {
        if (!HundsunUserManager.isUserRealLogined()) {
            openLoginActivity();
            return;
        }
        if (v == distHosBtn) {
            BaseJSONObject bundle = new BaseJSONObject();
            bundle.put(BundleDataContants.BUNDLE_DATA_IS_FROM_DIST, true);
            openNewActivity(PrescriptionActionContants.ACTION_PRESCRIPTION_ENTRY.val(), bundle);
        } else if (v == distHosPicture) {
            BaseJSONObject bundle = new BaseJSONObject();
            bundle.put(BundleDataContants.BUNDLE_DATA_IS_CAN_EDIT, true);
            openNewActivity(OnlinePurchaseActionContants.ACTION_ONLINEPURCHASE_DIST_RES_NOTE.val(), bundle);
        }
    }


    class PurchaseNoticeDialog extends Dialog {

        public PurchaseNoticeDialog(@NonNull Context context) {
            super(context, R.style.HundsunStyleAlertDialog);
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            final View view = getLayoutInflater().inflate(R.layout.hundsun_dialog_purchase_notice_a1, null);
            this.setContentView(view);
            this.setCancelable(true);
            this.setCanceledOnTouchOutside(true);

            View closeBtn = view.findViewById(R.id.closeBtn);
            final View noPurchaseBtn = view.findViewById(R.id.noPurchaseBtn);
            View knowPurchaseBtn = view.findViewById(R.id.knowPurchaseBtn);
            OnClickEffectiveListener clickListener = new OnClickEffectiveListener() {
                @Override
                public void onClickEffective(View view) {
                    if (view == noPurchaseBtn) {
                        BaseJSONObject param = new BaseJSONObject();
                        param.put(BundleDataContants.BUNDLE_DATA_ONLINECHAT_CONS_TYPE, ChatMessageConsType.PHOTO_TEXT.getName());
                        openNewActivity(OnlinetreatActionContants.ACTION_ONLINETREAT_DOCTORLIST.val(), param);
                    }
                    dismiss();
                }
            };
            noPurchaseBtn.setOnClickListener(clickListener);
            knowPurchaseBtn.setOnClickListener(clickListener);
            closeBtn.setOnClickListener(clickListener);
            final Window window = this.getWindow();
            window.getDecorView().setPadding(0, 0, 0, 0);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.CENTER);
            // 设置高宽
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.width = (int) (PixValue.m.widthPixels * 0.88f);
        }
    }/**
     * @Description: 初始化图文咨询提示语
     * @Author: zhongyj@hsyuntai.com
     * @Date: 2016/11/22 15:35
     */
    private void initPrompt() {
        attentionsTV.setVisibility(View.GONE);
        attentionsLayout.setVisibility(View.GONE);
        // 请求图文咨询订单过期时间
        SystemRequestManager.getPersonalizedParamsRes(this, "MEDICINE_TOHOME_NOTICE", new IHttpRequestTimeListener<PersonalizedParamsRes>() {
            @Override
            public void onSuccess(PersonalizedParamsRes personalizedParamsRes, List<PersonalizedParamsRes> list, String s, String s1, String s2) {
                if (personalizedParamsRes != null && personalizedParamsRes.getParamValue() != null) {
                    attentionsTV.setText(Html.fromHtml(personalizedParamsRes.getParamValue()));
                    attentionsTV.setVisibility(View.VISIBLE);
                    attentionsLayout.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFail(String s, String s1) {}
        });
    }
}
