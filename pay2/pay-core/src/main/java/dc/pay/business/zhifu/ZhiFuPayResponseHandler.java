package dc.pay.business.zhifu;

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
 * @author andrew
 * Dec 7, 2017
 */
@ResponsePayHandler("ZHIFU")
public final class ZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //基本参数
    //merchant_code	商家号		String(10)	必选	商户签约时，智付支付平台分配的唯一商家号。
    private static final String merchant_code = "merchant_code";            	
    //notify_type	通知类型	String(14)	必选	取值如下：服务器后台异步通知：offline_notify
    private static final String notify_type = "notify_type";            	
    //notify_id	通知校验ID	String(100)	必选	商家系统接收到此通知消息后，用此校验ID向智付支付平台校验此通知的合法性，由32位数字和字母组成。例如：e722dceae317466bbf9cc5f1254b8b0a注：此版本暂不校验，但参数依旧保留。
    private static final String notify_id = "notify_id";             	
    //interface_version接口版本	String(10)	必选	接口版本，固定值：V3.0
    private static final String interface_version = "interface_version";            	
    //sign_type	签名方式	String(10)	必选	RSA或RSA-S，不参与签名
    private static final String sign_type = "sign_type";            	
    //sign		签名		String		必选	签名数据，详见附录中的签名规则定义。
    private static final String sign = "sign";             	
    //业务参数
    //order_no	商户网站唯一订单号	String(100)	必选	商户系统订单号，由商户系统保证唯一性，最长64位字母、数字组成，举例：1000201555。
    private static final String order_no = "order_no";            	
    //order_time	商户订单时间		Date		必选	商户订单时间，格式：yyyy-MM-dd HH:mm:ss，举例：2013-11-01 12:34:54。
    private static final String order_time = "order_time";            	
    //order_amount	商户订单总金额		Number(13,2)	必选	该笔订单的总金额，以元为单位，精确到小数点后两位，举例：12.01。
    private static final String order_amount = "order_amount";             	
    //trade_no	智付交易订单号		String(30)	必选	智付交易订单号，举例：1000004817
    private static final String trade_no = "trade_no";            	
    //trade_time	智付交易订单时间	Date		必选	智付交易订单时间，格式为：yyyy-MM-dd HH:mm:ss，举例：2013-12-01 12:23:34。
    private static final String trade_time = "trade_time";            	
    //trade_status	交易状态		String(7)	必选	该笔订单交易状态 SUCCESS 交易成功 FAILED 交易失败
    private static final String trade_status = "trade_status";             	
    
    //银行交易流水号，举例：2013060911235456。
    private static final String bank_seq_no = "bank_seq_no";
    
    private static final String RESPONSE_PAY_MSG = "SUCCESS";



    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(merchant_code);
        String ordernumberR = API_RESPONSE_PARAMS.get(order_no);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[智付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        StringBuilder signStr = new StringBuilder();
        signStr.append(bank_seq_no+"=").append(params.get(bank_seq_no)).append("&");
        signStr.append(interface_version+"=").append(params.get(interface_version)).append("&");
        signStr.append(merchant_code+"=").append(params.get(merchant_code)).append("&");
        signStr.append(notify_id+"=").append(params.get(notify_id)).append("&");
        signStr.append(notify_type+"=").append(params.get(notify_type)).append("&");
        signStr.append(order_amount+"=").append(params.get(order_amount)).append("&");
        signStr.append(order_no+"=").append(params.get(order_no)).append("&");
        signStr.append(order_time+"=").append(params.get(order_time)).append("&");
        signStr.append(trade_no+"=").append(params.get(trade_no)).append("&");
        signStr.append(trade_status+"=").append(params.get(trade_status)).append("&");
        signStr.append(trade_time+"=").append(params.get(trade_time));
        String signInfo =signStr.toString();
        boolean result = false;
        if("RSA-S".equals(params.get(sign_type))){ // sign_type = "RSA-S"
            result = RsaUtil.validateSignByPublicKey(signInfo, channelWrapper.getAPI_PUBLIC_KEY(), params.get(sign));	
        }
        log.debug("[智付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(result));
        return String.valueOf(result);
    }




    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amount) throws PayException {
        boolean result = false;
        //该笔订单交易状态        SUCCESS 交易成功        FAILED 交易失败
        String payStatusCode = api_response_params.get(trade_status);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(order_amount));
        //amount数据库存入的是分 	第三方返回的responseAmount是元
        boolean checkAmount = amount.equalsIgnoreCase(responseAmount);
        if (checkAmount && payStatusCode.equalsIgnoreCase("SUCCESS")) {
            result = true;
        } else {
            log.error("[智付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + amount);
        }
        log.debug("[智付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：SUCCESS");
        return result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        Boolean signMd5Boolean = Boolean.valueOf(signMd5);
        log.debug("[智付]-[响应支付]-4.验证MD5签名：" + signMd5Boolean.booleanValue());
        return signMd5Boolean.booleanValue();
    }

    @Override
    protected String responseSuccess() {
        log.debug("[智付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}