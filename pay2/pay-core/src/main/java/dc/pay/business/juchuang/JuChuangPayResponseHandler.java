package dc.pay.business.juchuang;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Mar 11, 2018
 */
@ResponsePayHandler("JUCHUANG")
public final class JuChuangPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //外部订单号	outOrderNo	商户系统的订单编号
    private static final String outOrderNo  ="outOrderNo";
    //商品名称	goodsClauses	商户系统的商品名称
    private static final String goodsClauses  ="goodsClauses";
    //交易金额	tradeAmount	商户 商品价格（元）两位小数
    private static final String tradeAmount  ="tradeAmount";
    //商户code	shopCode 	点击头像，查看code
    private static final String shopCode  ="shopCode";
    //回调状态	code		回调状态0：成功  -1：失败
    private static final String code  ="code";
    //随机字符串	nonStr		随机字符串
    private static final String nonStr  ="nonStr";
    //支付状态	msg		成功：SUCCESS   失败：FAIL
    private static final String msg  ="msg";
    //MD5签名		sign		MD5签名值，utf-8编码
    private static final String signature  ="sign";
    private static final String key  ="key";

    private static final String RESPONSE_PAY_MSG = "SUCCESS";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        Set<String> keys = API_RESPONSE_PARAMS.keySet();
        Iterator<String> iterator = keys.iterator();
        String next = iterator.next();
        JSONObject parseObject = null;
        try {
        	parseObject = JSON.parseObject(next);
		} catch (Exception e) {
			throw new PayException(SERVER_MSG.REQUEST_PAY_CHANNEL_RESULT_ERROR);
		}
		String partnerR = parseObject.get(shopCode).toString();
		String ordernumberR = parseObject.get(outOrderNo).toString();
		if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
		    throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[聚创]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        Set<String> keys = API_RESPONSE_PARAMS.keySet();
        Iterator<String> iterator = keys.iterator();
        String next = iterator.next();
        JSONObject parseObject = JSON.parseObject(next);
        List paramKeys = MapUtils.sortMapByKeyAsc(parseObject);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!signature.equals(paramKeys.get(i)) && StringUtils.isNotBlank(parseObject.get(paramKeys.get(i))+"")) {
        		signSrc.append(paramKeys.get(i)).append("=").append(parseObject.get(paramKeys.get(i))).append("&");
			}
        }
        signSrc.append(key+"=").append(channelWrapper.getAPI_KEY());
		String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[聚创]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
    	Set<String> keys = api_response_params.keySet();
        Iterator<String> iterator = keys.iterator();
        String next = iterator.next();
        JSONObject parseObject = JSON.parseObject(next);
        boolean result = false;
        //支付状态	msg		成功：SUCCESS   失败：FAIL
        String payStatusCode = parseObject.get(msg).toString();
        String responseAmount = HandlerUtil.getFen(parseObject.get(tradeAmount).toString());
        //db_amount数据库存入的是分 	第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("SUCCESS")) {
            result = true;
        } else {
            log.error("[聚创]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[聚创]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：SUCCESS");
        return result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
    	Set<String> keys = api_response_params.keySet();
        Iterator<String> iterator = keys.iterator();
        String next = iterator.next();
        JSONObject parseObject = JSON.parseObject(next);
        boolean result = parseObject.get(signature).toString().equalsIgnoreCase(signMd5);
        log.debug("[聚创]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[聚创]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}