package dc.pay.business.efutong;

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
import dc.pay.utils.RsaUtil;

/**
 * 
 * 
 * @author kevin
 * Jul 25, 2018
 */
@ResponsePayHandler("EFUTONG")
public final class EFuTongPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String trade_no = "trade_no";             //1003804468
    private static final String sign_type = "sign_type";           //RSA-S
    private static final String notify_type = "notify_type";       //offline_notify
    private static final String merchant_code = "merchant_code";   //123123123000
    private static final String order_no = "order_no";             //20171005100132
    private static final String trade_status = "trade_status";     //SUCCESS
    private static final String sign = "sign";                      //KlQe34MT9iGdW24UucNrD0JqgG6vP9h0VTs+HumKATSvi+6WlDxa0w2BSHSDwnT8GlALCofKjSpU1ttPhzOewW9JfacNnXRLY9X23N4iUWZpNtzHQhX1+7HXr9QcZvjXXLDZ/WNLhpHehPddb/xE54O/FiNd0d7j644zpLGHndY=
    private static final String order_amount = "order_amount";      //5
    private static final String interface_version = "interface_version"; //V3.0
    private static final String bank_seq_no = "bank_seq_no";             //Z1106836224
    private static final String order_time = "order_time";               //2017-10-05 10:01:32
    private static final String notify_id = "notify_id";                 //ef5efeeb0891450eb2551c3e80cdb7cd
    private static final String trade_time = "trade_time";               //2017-10-05 10:01:32

    private static final String RESPONSE_PAY_MSG = "SUCCESS";
    
    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(merchant_code);
        String ordernumberR = API_RESPONSE_PARAMS.get(order_no);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[易富通]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }
    
    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        StringBuilder signStr = new StringBuilder();
        if(null != params.get(bank_seq_no) && StringUtils.isNotBlank(params.get(bank_seq_no))) {
            signStr.append("bank_seq_no=").append(params.get(bank_seq_no)).append("&");
        }
        signStr.append("interface_version=").append(params.get(interface_version)).append("&");
        signStr.append("merchant_code=").append(params.get(merchant_code)).append("&");
        signStr.append("notify_id=").append(params.get(notify_id)).append("&");
        signStr.append("notify_type=").append(params.get(notify_type)).append("&");
        signStr.append("order_amount=").append(params.get(order_amount)).append("&");
        signStr.append("order_no=").append(params.get(order_no)).append("&");
        signStr.append("order_time=").append(params.get(order_time)).append("&");
        signStr.append("trade_no=").append(params.get(trade_no)).append("&");
        signStr.append("trade_status=").append(params.get(trade_status)).append("&");
        signStr.append("trade_time=").append(params.get(trade_time));

        String signInfo =signStr.toString();
        boolean result = false;

        if("RSA-S".equals(params.get(sign_type))){
            String wpay_public_key = channelWrapper.getAPI_PUBLIC_KEY();
            result = RsaUtil.validateSignByPublicKey(signInfo, wpay_public_key, params.get(sign));
        }

        log.debug("[易富通]-[[响应支付]-2.生成加密URL签名完成：" + JSON.toJSONString(result));
        return String.valueOf(result);
    }


    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //trade_status		交易状态	SUCCESS-支付成功
        String payStatusCode = api_response_params.get(trade_status);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(order_amount));
        //order_amount数据库存入的是分 	第三方返回的order_amount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //00代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("SUCCESS")) {
            my_result = true;
        } else {
            log.error("[易富通]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[易富通]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：SUCCESS");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
    	 Boolean signMd5Boolean =   Boolean.valueOf(signMd5);
         log.debug("[易富通]-[响应支付]-4.验证MD5签名：" + signMd5Boolean.booleanValue());
         return signMd5Boolean.booleanValue();
    }

    @Override
    protected String responseSuccess() {
        log.debug("[易富通]-[响应支付]-5.第三方支付确认收到消息返回内容：{}" , RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}