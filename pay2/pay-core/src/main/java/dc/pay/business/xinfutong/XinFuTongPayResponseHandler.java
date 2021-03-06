package dc.pay.business.xinfutong;

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
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * May 15, 2018
 */
@ResponsePayHandler("XINFUTONG")
public final class XinFuTongPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

	//参数名				变量名					参数类型				参数说明
	//外部交易号			order_no				String(64)				该交易在合作伙伴系统的商户订单号
	//通讯状态				is_success				String（TRADE_FINISHED）				T，表示成功；F，表示失败
	//交易金额				total_fee				Number(TRADE_FINISHED3,2)			单位为RMB元
	//交易状态				trade_status			String					成功状态：TRADE_FINISHED
	//卖家ID				seller_id				String(30)				卖家ID
	//签名				sign					String					加签结果
	//签名类型				signType				String					SHA
	private static final String	order_no		="order_no";
//	private static final String	is_success		="is_success";
	private static final String	total_fee		="total_fee";
	private static final String	trade_status		="trade_status";
	private static final String	seller_id		="seller_id";
	private static final String	signType		="signType";

    //signature	数据签名	32	是	　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(seller_id);
        String ordernumberR = API_RESPONSE_PARAMS.get(order_no);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[信付通]-[响应支付]-TRADE_FINISHED.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
    	List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
    	StringBuilder signSrc = new StringBuilder();
    	for (int i = 0; i < paramKeys.size(); i++) {
    		if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i))) && !signType.equals(paramKeys.get(i)) && !signature.equals(paramKeys.get(i))) {
    			signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
    		}
    	}
    	String paramsStr = signSrc.toString();
    	//去除最后一个&符
    	paramsStr = paramsStr.substring(0,paramsStr.length()-1);
    	paramsStr = paramsStr+channelWrapper.getAPI_KEY();
    	String signMd5 = null;
    	try {
    		signMd5 = Sha1Util.getSha1(paramsStr).toUpperCase();
    	} catch (Exception e) {
    		e.printStackTrace();
    		throw new PayException("签名异常，请查检参数！");
    	};
        log.debug("[信付通]-[响应支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean result = false;
        //成功状态：TRADE_FINISHED
        String payStatusCode = api_response_params.get(trade_status);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(total_fee));
        //db_amount数据库存入的是分 	第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //TRADE_FINISHED代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("TRADE_FINISHED")) {
            result = true;
        } else {
            log.error("[信付通]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[信付通]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：TRADE_FINISHED");
        return result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[信付通]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[信付通]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}