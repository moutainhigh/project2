package dc.pay.business.xunjietong;

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

@ResponsePayHandler("XUNJIETONG")
public final class XunJieTongPayResponseHandler extends PayResponseHandler {
   private final Logger log =  LoggerFactory.getLogger(XunJieTongPayResponseHandler.class);
         private static final String  amount = "amount" ;
         private static final String  transDate = "transDate" ;
         private static final String  merchno = "merchno" ;
         private static final String  status = "status" ;
         private static final String  channelTraceno = "channelTraceno" ;
         private static final String  traceno = "traceno" ;
         private static final String  transTime = "transTime" ;
         private static final String  payType = "payType" ;
         private static final String  orderno = "orderno" ;
         private static final String  merchName = "merchName" ;
         private static final String  signature = "signature" ;
         private static final String  channelOrderno = "channelOrderno" ;
         private static final String  RESPONSE_PAY_MSG= "success";



    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if(null==API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
            String memberId = API_RESPONSE_PARAMS.get(merchno);
            String orderId = API_RESPONSE_PARAMS.get(traceno);
        if(StringUtils.isBlank(memberId) || StringUtils.isBlank(orderId))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[迅捷通]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}",orderId);
         return orderId;
    }

    @Override
    protected String buildPaySign(Map<String, String> payParam, String api_key) throws PayException {
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(StringUtils.isBlank(payParam.get(paramKeys.get(i))) || signature.equalsIgnoreCase(paramKeys.get(i).toString()))
                continue;
            sb.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
        }
        sb.append(channelWrapper.getAPI_KEY());//"key="+
        pay_md5sign = HandlerUtil.getMD5UpperCase(sb.toString()).toUpperCase();
        log.debug("[迅捷通]-[请求支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params,String amount) throws PayException {
        boolean result = false;
        String payStatusCode = api_response_params.get(status);
        String responseAmount = api_response_params.get("amount");
        responseAmount = HandlerUtil.getFen(responseAmount);
        boolean checkAmount = amount.equalsIgnoreCase(responseAmount);
        if(checkAmount && payStatusCode.equalsIgnoreCase("1")  ){
            result = true;
        }else{
            log.error("[迅捷通]-[响应支付]金额及状态验证错误,订单号："+channelWrapper.getAPI_ORDER_ID()+",第三方支付状态："+payStatusCode +" ,支付金额："+responseAmount+" ，应支付金额："+amount);
        }
        log.debug("[迅捷通]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果："+ result+" ,金额验证："+checkAmount+" ,responseAmount="+responseAmount +" ,数据库金额："+amount+",第三方响应支付成功标志:"+payStatusCode+" ,计划成功：1");
        return result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params,String signMd5) {
        boolean result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[迅捷通]-[响应支付]-4.验证MD5签名：{}", result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[迅捷通]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}