package dc.pay.business.shanshuifu;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Jul 7, 2018
 */
@ResponsePayHandler("SHANSHUIFU")
public final class ShanShuiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //参数名称         含义                    参数长度      是否必填      备注                  样例
    //merId            商户ID                  String(16)      是            签约合作方的唯一用户号      2088001159940003
    //appId            应用ID                  String(16)      是            商户网站使用的编码格式，统一使用UTF-8      908265290880774144
    //outerTradeNo     平台订单号              String(32)      是            由平台返回对应的订单号      30000043551252390
    //orderNo          商户网站唯一订单号      String(16)      是            商户网站唯一订单号（确保在商户系统中唯一）。      6741334835157966
    //totalFee         交易金额                Number(12)      是            金额单位（元）      157.00
    //attach           附加报文                String(128)     否            商户附加报文      {"trade_no":"A60701220214249470002850","total_fee":"2.00","send_pay_date":"2016-07-01 22:02:14","buyer_logon_id":"","plat":"1"}
    //sign             签名                    String(128)     是            除了sign和空值以外，用公钥加密
//    private static final String merId                        ="merId";
//    private static final String appId                        ="appId";
//    private static final String outerTradeNo                 ="outerTradeNo";
    private static final String orderNo                      ="orderNo";
    private static final String totalFee                     ="totalFee";
//    private static final String attach                       ="attach";
//    private static final String status                       ="status";

    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
//        String partnerR = API_RESPONSE_PARAMS.get(merId);
        String ordernumberR = API_RESPONSE_PARAMS.get(orderNo);
//        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[山水付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!signature.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        signSrc.append("key=" + channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
		System.out.println("签名源串："+paramsStr);
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[山水付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        // status       4：交易成功
//        String payStatusCode = api_response_params.get(status);
        String payStatusCode = "无";
        String responseAmount = HandlerUtil.getFen(api_response_params.get(totalFee));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("无")) {
            my_result = true;
        } else {
            log.error("[山水付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[山水付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：无");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[山水付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[山水付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}