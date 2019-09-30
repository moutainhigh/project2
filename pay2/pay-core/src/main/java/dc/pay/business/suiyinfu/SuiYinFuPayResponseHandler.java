package dc.pay.business.suiyinfu;

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
import dc.pay.utils.MapUtils;
import dc.pay.utils.Sha1Util;

/**
 * 
 * @author beck
 * Aug 08, 2018
 */
@ResponsePayHandler("SUIYINFU")
public final class SuiYinFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String trade_no     ="trade_no";
    private static final String fee      ="fee";
    private static final String trade_status = "trade_status";
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "SUCCESS";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(trade_no);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[随银付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        
        List<String> paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            String keyName = paramKeys.get(i);
            String value = api_response_params.get(keyName);
            
            if (!signature.equals(keyName) && StringUtils.isNotBlank(value)) {
                signSrc.append(keyName).append("=").append(value).append("&");
            }
        }
        signSrc.append("key="+api_key);
        String paramsStr = signSrc.toString();
        String signMd5 = Sha1Util.getSha1(paramsStr);
        log.debug("[随银付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        String payStatusCode = api_response_params.get(trade_status);
        String responseAmount = api_response_params.get(fee);
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        if (checkAmount && payStatusCode.equalsIgnoreCase("SUCCESS")) {
            my_result = true;
        } else {
            log.error("[随银付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[随银付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：SUCCESS");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[随银付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[随银付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}