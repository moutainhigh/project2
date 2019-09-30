package dc.pay.business.chengyou;

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
 * 
 * 
 * @author kevin
 * Jul 27, 2018
 */
@ResponsePayHandler("CHENGYOU")
public final class ChengYouPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());


    private static final String memberid  		="memberid";
    private static final String orderid    		="orderid";
    private static final String amount  		="amount";
    private static final String returncode    	="returncode";
    private static final String attach    		="attach";
    private static final String sign    		="sign";

    private static final String RESPONSE_PAY_MSG = "ok";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(memberid);
        String ordernumberR = API_RESPONSE_PARAMS.get(orderid);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[诚优]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" , ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(StringUtils.isBlank(api_response_params.get(paramKeys.get(i))) || sign.equalsIgnoreCase(paramKeys.get(i).toString()) || attach.equalsIgnoreCase(paramKeys.get(i).toString()))  
                continue;
            sb.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            
        }
        sb.append("key=" + channelWrapper.getAPI_KEY());
        String signStr = sb.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(signStr);
        log.debug("[诚优]-[响应支付]-2.生成加密URL签名完成：{}" , JSON.toJSONString(signMd5));
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //returncode		交易状态	00-支付成功
        String payStatusCode = api_response_params.get(returncode);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(amount));
        //amount数据库存入的是分 	第三方返回的amount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //00代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("00")) {
            my_result = true;
        } else {
            log.error("[诚优]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[诚优]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：00");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
    	//System.out.println("hmac=========>"+api_response_params.get(hmac));
    	//System.out.println("signMd5=========>"+signMd5);
        boolean my_result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("[诚优]-[响应支付]-4.验证MD5签名：{}" , my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[诚优]-[响应支付]-5.第三方支付确认收到消息返回内容：{}" , RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}