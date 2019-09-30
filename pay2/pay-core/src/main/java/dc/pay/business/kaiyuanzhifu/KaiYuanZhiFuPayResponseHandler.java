package dc.pay.business.kaiyuanzhifu;

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
import dc.pay.utils.Sha1Util;

/**
 * 
 * @author sunny
 * Dec 14, 2018
 */
@ResponsePayHandler("KAIYUANZHIFU")
public final class KaiYuanZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());
//    参数名			字段名称			出现  		次数			说明
//    1	result		返回码			1..1	请求结果标志success：请求授理成功，不代表交易成功error：业务失败exception：网络异常失败， 不代表交易失败
//    2	code		返回码描述		1..1	错误编码
//    3	msg			错误描述			例:账户余额不足
//    4	mer_id		商户号			小天支付分配给商户的mer_id
//    5	businessnumber	业务订单号	1..1	
//    6	status		交易状态			1..1	成功/处理中/失败
//    7	transactiondate	交易时间		1..1	格式 yyyy-MM-dd HH:mm:ss
//    8	amount		交易金额			1..1	单位 分
//    9	real_amount	交易金额			1..1	单位 分
//    10transactiontype	交易类型		1..1	例:代收
//    11inputdate	交易创建时间		1..1	格式 yyyy-MM-dd HH:mm:ss
//    12remark		结果说明			1..1	
//    13sign		签名				1..1	
//    14sign_type	签名算法类型		1..1	

    private static final String result                   ="result";
    private static final String code                     ="code";
    private static final String msg                      ="msg";
    private static final String mer_id                   ="mer_id";
    private static final String businessnumber           ="businessnumber";
    private static final String status                   ="status";
    private static final String transactiondate          ="transactiondate";
    private static final String amount                   ="amount";
    private static final String real_amount              ="real_amount";
    private static final String sign                     ="sign";
    
    private static final String sign_type                     ="sign_type";

    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("success");

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(mer_id);
        String ordernumberR = API_RESPONSE_PARAMS.get(businessnumber);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[开元支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
    	List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        paramKeys.remove(sign);
        paramKeys.remove(sign_type);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
//            if (!pay_productname.equals(paramKeys.get(i)) && !pay_attach.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
            /*if(paramKeys.get(i).equals(sign_type)){
            	signSrc.append(ServerUrl).append("=").append(api_response_params.get(ServerUrl)).append("&");
            }*/
//        	if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
//            	signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
//            }
        	signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
        }
        //最后一个&转换成#
        //signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "#" );
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[开元支付]-[响应支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return signMD5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        String payStatusCode = api_response_params.get(status);
        String responseAmount = api_response_params.get(amount);
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount&&payStatusCode.equalsIgnoreCase("成功")) {
            my_result = true;
        } else {
            log.error("[开元支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[开元支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：成功");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[开元支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[开元支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return  RESPONSE_PAY_MSG;
    }
}