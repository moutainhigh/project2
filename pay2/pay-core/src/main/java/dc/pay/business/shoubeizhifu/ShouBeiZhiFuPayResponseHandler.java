package dc.pay.business.shoubeizhifu;

/**
 * ************************
 * @author tony 3556239829
 */

import com.alibaba.fastjson.JSON;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@ResponsePayHandler("SHOUBEIZHIFU")
public final class ShouBeiZhiFuPayResponseHandler extends PayResponseHandler {
     private final Logger log = LoggerFactory.getLogger(getClass());
     private static final String RESPONSE_PAY_MSG = "SUCCESS";

     private static final String  dealId = "dealId"  ;    // "201809171616152d64c3540354409",
     private static final String  dealTime = "dealTime"  ;    // "20180917161748",
     private static final String  orderAmount = "orderAmount"  ;    // "1.00",
     private static final String  payAmount = "payAmount"  ;    // "1.00",
     private static final String  payType = "payType"  ;    // "0301",
     private static final String  retCode = "retCode"  ;    // "RC0000",
     private static final String  transStatus = "transStatus"  ;    // "2",
     private static final String  transactionId = "transactionId"  ;    // "20180917161615",
     private static final String  signData = "signData"  ;    // "AD72E888FB2E1708F635466878923037"


    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(transactionId);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[收呗支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(StringUtils.isBlank(params.get(paramKeys.get(i))) || signData.equalsIgnoreCase(paramKeys.get(i).toString()) )  //
                continue;
            sb.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
        }
        sb.append("key=" + channelWrapper.getAPI_KEY());
        String signStr = sb.toString(); //.replaceFirst("&key=","")
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr);
        log.debug("[收呗支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }



    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean checkResult = false;
        String payStatus = api_response_params.get(transStatus);
        String responseAmount =  HandlerUtil.getFen(api_response_params.get(payAmount));
        boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        if (checkAmount && payStatus.equalsIgnoreCase("2")) {
            checkResult = true;
        } else {
            log.error("[收呗支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatus + " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
        }
        log.debug("[收呗支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + checkResult + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + ",第三方响应支付成功标志:" + payStatus + " ,计划成功：1");
        return checkResult;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(signData).equalsIgnoreCase(signMd5);
        log.debug("[收呗支付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[收呗支付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}