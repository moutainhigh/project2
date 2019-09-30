package dc.pay.business.juxianzhifu;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

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
@ResponsePayHandler("JUXIANZHIFU")
public final class JuXianZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

//    字段 			字段说明 				字段类型 			 备注
//    code 			状态码 				String 			是10000：成功 其他：失败
//    msg 			状态信息 				String 			是 状态信息说明。
//    appId 		商户标识 				String 			是您的商户唯一标识，登陆后台系统在个人中心获得。
//    tradeNo 		平台订单号 			String 			是 平台订单号，方便对账使 用。
//    outTradeNo 	商户订单号			String 			是商户平台自定义的订单号，下单时商户自己传入的。
//    amount 		订单金额 				String 			是 商户支付时的订单金额。
//    payMethod 	支付方式 				String 			是 支付方式，固定枚举值
//    sign 			签名结果 				String 			是 对返回数据签名后的结 果。

    private static final String mch_id                   ="mch_id";
    private static final String out_trade_no             ="out_trade_no";
    private static final String amount                   ="amount";
    private static final String status                	 ="status";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("success");

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        JSONObject resJson=getResponseJson();
        String partnerR = resJson.getString(mch_id);
        String ordernumberR = resJson.getString(out_trade_no);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[聚贤支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
    	JSONObject resJson=getResponseJson();
    	Map<String, String> responseMap=HandlerUtil.jsonToMap(resJson.toJSONString());
    	List paramKeys = MapUtils.sortMapByKeyAsc(responseMap);
    	paramKeys.remove(signature);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
//            if (!pay_productname.equals(paramKeys.get(i)) && !pay_attach.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
            if (StringUtils.isNotBlank(responseMap.get(paramKeys.get(i)))) {
            	signSrc.append(paramKeys.get(i)).append("=").append(responseMap.get(paramKeys.get(i))).append("&");
            }
        }
        //最后一个&转换成#
        signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "" );
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[聚贤支付]-[响应支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return signMD5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
    	JSONObject resJson=getResponseJson();
        boolean my_result = false;
        String payStatusCode = resJson.getString(status);
        String responseAmount = HandlerUtil.getFen(resJson.getString(amount));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        boolean checkAmount = HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差
        //1代表第三方支付成功
        if (checkAmount&&payStatusCode.equalsIgnoreCase("SUCCESS")) {
            my_result = true;
        } else {
            log.error("[聚贤支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[聚贤支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：SUCCESS");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
    	boolean my_result=false;
    	JSONObject resJson=null;
    	try {
			 resJson=getResponseJson();
			 my_result = resJson.getString(signature).equalsIgnoreCase(signMd5);
		} catch (PayException e) {
			e.printStackTrace();
		}
        log.debug("[聚贤支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[聚贤支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return  RESPONSE_PAY_MSG;
    }
    
    
    private JSONObject getResponseJson() throws PayException{
    	String responseJson = "";
        for (String keyValue : API_RESPONSE_PARAMS.keySet()) {
        	 responseJson = keyValue;
        }
        JSONObject resJson;
        try {
            resJson = JSONObject.parseObject(responseJson);
        } catch (Exception e) {
            e.printStackTrace();
            throw new PayException(responseJson);
        }
        if(null==resJson){
        	throw new PayException(responseJson);
        }
        return resJson;
    }
}