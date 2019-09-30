package dc.pay.business.wanchanghong;

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


/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Nov 10, 2018
 */
@ResponsePayHandler("WANCHANGHONG")
public final class WanChangHongPayResponseHandler extends PayResponseHandler {
     private final Logger log = LoggerFactory.getLogger(getClass());
     private static final String RESPONSE_PAY_MSG = "SUCCESS";
     
     private static final String  retCode = "retCode";  
     //private static final String  msg = "msg";
     private static final String  orderId = "orderId";  
     //private static final String  merchantId = "merchantId";
     //private static final String  payType = "payType";
     //private static final String  chnlTradeNo = "chnlTradeNo";
     private static final String  amount = "amount";
     //private static final String  createOrderTime = "createOrderTime";
     //private static final String  processOrderTime = "processOrderTime";
     
     private static final String  sign = "sign";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(orderId);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[万昌红]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!sign.equals(paramKeys.get(i)) && StringUtils.isNotBlank(params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
            }
        }
        
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toUpperCase();
        log.debug("[万昌红]-[响应支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean checkResult = false;
        String resultStr = api_response_params.get(retCode);
        String responseAmount =  HandlerUtil.getFen(api_response_params.get(amount));
        boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        if (checkAmount && resultStr.equalsIgnoreCase("2")) {
            checkResult = true;
        } else {
            log.error("[万昌红]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + resultStr + " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
        }
        log.debug("[万昌红]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + checkResult + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + ",第三方响应支付成功标志:" + resultStr + " ,计划成功：2");
        return checkResult;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("[万昌红]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[万昌红]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}