package dc.pay.business.yipinfu;

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


/**
 * ************************
 * @author beck 2229556569
 */

@ResponsePayHandler("YIPINFU")
public final class YiPinFuPayResponseHandler extends PayResponseHandler {
     private final Logger log = LoggerFactory.getLogger(getClass());
     private static final String RESPONSE_PAY_MSG = "SUCCESS";

     private static final String  success = "success";
     private static final String  orderNumber = "orderNumber";
     private static final String  money = "money";
     private static final String  payDate = "payDate";
     private static final String  signature = "signature";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(orderNumber);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[一品付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        StringBuffer sb = new StringBuffer();
        sb.append(params.get(success)).append("&");
        sb.append(params.get(orderNumber)).append("&");
        sb.append(params.get(money)).append("&");
        sb.append(params.get(payDate)).append("&");
        sb.append(channelWrapper.getAPI_KEY());
        
        String paramsStr = sb.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[一品付]-[响应支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }
    

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean checkResult = false;
        String resultStr = api_response_params.get(success);
        String responseAmount =  HandlerUtil.getFen(api_response_params.get(money));
        boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        if (checkAmount && resultStr.equalsIgnoreCase("true")) {
            checkResult = true;
        } else {
            log.error("[一品付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + resultStr + " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
        }
        log.debug("[一品付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + checkResult + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + ",第三方响应支付成功标志:" + resultStr + " ,计划成功：true");
        return checkResult;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[一品付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[一品付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}