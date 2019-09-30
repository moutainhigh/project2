package dc.pay.business.mingshu;

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
 * @author sunny
 * Dec 14, 2018
 */
@ResponsePayHandler("MINGSHU")
public final class MingShuResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

//    参数					参数说明					类型			备注
//    transactionId			商户订单号				varchar(50)	平台唯一交易编号
//    orderAmount			商户订单金额				decimal(18, 2)	以元为单位，例如10元，金额格式为10.00
//    payType				支付方式					varchar(50)	
//    payAmount				订单实际支付金额			varchar(20)	
//    dealId				支付平台订单号				varchar(50)	
//    dealTime				支付平台付款时间			datetime	
//    transStatus			订单状态					varchar(50)	
//    retCode				应答码					varchar(50)	
//    signData				签名						varchar(32)	参照签名方法


    private static final String transactionId              ="transactionId";
    private static final String orderAmount                ="orderAmount";
    private static final String payType               	   ="payType";
    private static final String payAmount                  ="payAmount";
    private static final String dealId                     ="dealId";
    private static final String dealTime                   ="dealTime";
    private static final String transStatus                ="transStatus";
    private static final String retCode                    ="retCode";

    //signature    数据签名    32    是    　
    private static final String signature  ="signData";
    
    private static final String key        ="key";

    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("SUCCESS");

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(transactionId);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[明书支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
    	//签名规则
    	List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
    	paramKeys.remove(signature);
    	StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
        	
            if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        signSrc.append(key+"="+channelWrapper.getAPI_KEY());
        String paramsStr =signSrc.toString();
        String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr, "utf-8");
        log.debug("[明书支付]-[响应支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return signMD5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //transStatus          交易状态         是            “2” 为成功
        String payStatusCode = api_response_params.get(transStatus);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(orderAmount));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        //实际支付金额
        boolean checkAmount=db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount&&payStatusCode.equalsIgnoreCase("2")) {
            my_result = true;
        } else {
            log.error("[明书支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[明书支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：2");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[明书支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[明书支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}