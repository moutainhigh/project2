package dc.pay.business.chengyitong2zhifu;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.business.kuaitongbaozhifu.RSAUtils;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;

/**
 * @author sunny
 */
@ResponsePayHandler("CHENGYITONG2ZHIFU")
public final class ChengYiTong2ZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

//    字段名			字段说明			数据类型			最大长度
//    partner		商户ID			int	
//    ordernumber	商户订单号		String 	
//    reqdata		RSA加密，
//    UrlEncode编码	String 			4000

    private static final String partner                   ="partner";
    private static final String ordernumber               ="ordernumber";
    private static final String reqdata                   ="reqdata";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("ok");

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(partner);
        String ordernumberR = API_RESPONSE_PARAMS.get(ordernumber);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[诚易通2支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
    	String deRsa="";
    	try {
			 deRsa = RSAUtils.decryptByPrivateKey(api_response_params.get(reqdata),channelWrapper.getAPI_KEY());
		} catch (Exception e) {
			e.printStackTrace();
		}
    	Map<String, String> resultMap = HandlerUtil.jsonToMap(deRsa);
    	String signSrc=String.format("%s%s%s%s%s", 
    			"partner="+resultMap.get("partner")+"&",
    			"ordernumber="+resultMap.get("ordernumber")+"&",
    			"orderstatus="+resultMap.get("orderstatus")+"&",
    			"paymoney="+resultMap.get("paymoney"),
    			channelWrapper.getAPI_PUBLIC_KEY().split("&")[0]
    	);
        String paramsStr = signSrc.toString();
        String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[诚易通2支付]-[响应支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return signMD5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
    	String deRsa="";
    	try {
			 deRsa = RSAUtils.decryptByPrivateKey(api_response_params.get(reqdata),channelWrapper.getAPI_KEY());
		} catch (Exception e) {
			e.printStackTrace();
		}
    	Map<String, String> resultMap = HandlerUtil.jsonToMap(deRsa);
        boolean my_result = false;
        String payStatusCode = resultMap.get("orderstatus");
        String responseAmount = HandlerUtil.getFen(resultMap.get("paymoney"));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        boolean checkAmount =HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差
        //1代表第三方支付成功
        if (checkAmount&&payStatusCode.equalsIgnoreCase("1")) {
            my_result = true;
        } else {
            log.error("[诚易通2支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[诚易通2支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
    	String deRsa="";
    	try {
			 deRsa = RSAUtils.decryptByPrivateKey(api_response_params.get(reqdata),channelWrapper.getAPI_KEY());
		} catch (Exception e) {
			e.printStackTrace();
		}
    	Map<String, String> resultMap = HandlerUtil.jsonToMap(deRsa);
        boolean my_result = resultMap.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[诚易通2支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[诚易通2支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return  RESPONSE_PAY_MSG;
    }
}