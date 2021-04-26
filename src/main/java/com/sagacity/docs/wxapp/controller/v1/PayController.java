package com.sagacity.docs.wxapp.controller.v1;

import com.jfinal.aop.Before;
import com.jfinal.ext.route.ControllerBind;
import com.jfinal.kit.HttpKit;
import com.jfinal.kit.PropKit;
import com.jfinal.kit.StrKit;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.tx.Tx;
import com.jfinal.weixin.sdk.kit.IpKit;
import com.jfinal.weixin.sdk.kit.PaymentKit;
import com.jfinal.weixin.sdk.utils.PaymentException;
import com.jfinal.wxaapp.api.WxaOrder;
import com.jfinal.wxaapp.api.WxaPayApi;
import com.sagacity.docs.base.extend.OrderState;
import com.sagacity.docs.base.extend.ResponseCode;
import com.sagacity.docs.model.UserDao;
import com.sagacity.docs.model.order.OrderInfo;
import com.sagacity.docs.model.order.PayInfo;
import com.sagacity.docs.wxapp.common.WXSSBaseController;
import com.sagacity.utility.DateUtils;
import com.sagacity.utility.IPUtil;

import java.util.HashMap;
import java.util.Map;

@ControllerBind(controllerKey = "/wxss/pay", viewPath = "/wxss")
public class PayController extends WXSSBaseController{

    private static String appid = PropKit.get("wxss.appid");
    private static String partner = PropKit.get("wxPay.partner");
    private static String partnerKey = PropKit.get("wxPay.partnerKey");
    private static String certPath = PropKit.get("wxPay.certPath");
    private static String certPass = PropKit.get("wxPay.certPass");
    private static String notify_url = PropKit.get("base.url")+"wxss/pay/pay_notify";

    @Override
    public void index() {

    }

    /**
     * 获得打赏项目
     */
    public void getPayItem(){

        data.put(ResponseCode.LIST, Db.find("select * from pay_item where state=1"));
        rest.success().setData(data);

        renderJson(rest);
    }

    @Before(Tx.class)
    public void genOrder(){
        boolean r = false;
        StringBuilder msg = new StringBuilder();

        UserDao userInfo = getCurrentUser(getPara("token"));
        int dataId = getParaToInt("id");
        String type = getPara("type");
        double cost = getParaToLong("cost").doubleValue();

        //
        PayInfo pi = new PayInfo().set("data_id", dataId).set("data_type", type).set("user_id", userInfo.getUser_id())
                .set("cost", cost).set("created_at", DateUtils.nowDateTime()).set("state", 1);
        r = pi.save();

        //根据报名费用判断是否生成订单
        int orderState = OrderState.PrePay_Confirm;
        String orderCode = OrderInfo.dao.genOrderCode("V");
        if(cost > 0.0f){
            OrderInfo oi = new OrderInfo().set("pay_id", pi.get("pay_id")).set("order_state", orderState)
                    .set("order_code", orderCode).set("order_date", DateUtils.nowDate()).set("order_time", DateUtils.getTimeShort())
                    .set("user_id", userInfo.getUser_id()).set("total_price", Math.round(cost*100));
            r = oi.save();
            data.put("order", oi);
        }
        if(r){
            data.put("pay_id", pi.get("pay_id"));
            rest.success("打赏成功！").setData(data);
        }else{
            rest.error("打赏失败！");
        }
        renderJson(rest);
    }

    @Before(Tx.class)
    public void wxPay(){
        UserDao userInfo = getCurrentUser(getPara("token"));

        WxaOrder order = new WxaOrder(appid, partner, partnerKey);
        order.setBody(getPara("body","enroll cost"));
        order.setNotifyUrl(notify_url);
        order.setOutTradeNo(getPara("out_trade_no"));
        order.setTotalFee(getPara("total_fee"));
        order.setOpenId(userInfo.getOpen_id());

        String ip = IpKit.getRealIp(getRequest()); //IPV6会出现问题
        if (StrKit.isBlank(ip)) {
            ip = "127.0.0.1";
        }
        try {
            ip = IPUtil.hexToIP(IPUtil.ipToHex(ip));
        }catch (Exception ex){
            ip = "127.0.0.1";
        }

        order.setSpbillCreateIp(ip);

        //发起支付
        WxaPayApi wxaPayApi = new WxaPayApi();
        try{
            Map<String, String> packageParams = wxaPayApi.unifiedOrder(order);
            String prepayId = packageParams.get("package").split("=")[1];
            OrderInfo oi = OrderInfo.dao.findFirst("select * from pay_order where order_code=?",getPara("out_trade_no"));
            oi.set("prepay_id", prepayId).update();
            rest.success().setData(packageParams);
        } catch (PaymentException ex){
            rest.error(ex.getReturnMsg());
        }

        renderJson(rest);
    }

    @Before(Tx.class)
    public void pay_notify(){
        String xmlMsg = HttpKit.readData(getRequest());
        System.out.println("支付通知="+xmlMsg);
        Map<String, String> params = PaymentKit.xmlToMap(xmlMsg);

        String result_code  = params.get("result_code");
        // 总金额
        String totalFee     = params.get("total_fee");
        // 商户订单号
        String orderId      = params.get("out_trade_no");
        // 微信支付订单号
        String transId      = params.get("transaction_id");
        // 支付完成时间，格式为yyyyMMddHHmmss
        String timeEnd      = params.get("time_end");

        // 注意重复通知的情况，同一订单号可能收到多次通知，请注意一定先判断订单状态
        // 避免已经成功、关闭、退款的订单被再次更新

        if(PaymentKit.verifyNotify(params, partnerKey)){
            if (("SUCCESS").equals(result_code)) {
                //在数据库中，更新订单信息
                System.out.println("订单号：" + orderId);
                OrderInfo oi = OrderInfo.dao.findFirst("select * from pay_order where orderCode=?", orderId);
                if(oi != null && oi.get("TransID") == null){
                    oi.set("orderState", OrderState.FeedBack_Confirm).set("transID", transId)
                            .set("totalFee", totalFee).set("timeEnd", timeEnd).update();
                    if(Double.parseDouble(totalFee) == oi.getBigDecimal("totalPrice").doubleValue() ){
                        PayInfo.dao.findById(oi.get("pay_id"))
                                .set("order_code", orderId).set("state", 2).update();
                        //向支付用户推送模板消息
//                        ProfileInfo pi = ProfileInfo.dao.findById(oi.get("openid"));
//                        WXMessage.dao.sendPayResult(pi.getStr("openid"), orderId);

                    }else{ //订单异常
                        oi.set("orderState", OrderState.Order_Error).update();
                    }
                    //向微信支付的反馈信息
                    Map<String, String> xml = new HashMap<String, String>();
                    xml.put("return_code", "SUCCESS");
                    xml.put("return_msg", "OK");
                    renderText(PaymentKit.toXml(xml));
                    return;
                }
            }
        }
        renderText("");
    }


}
