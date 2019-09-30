package dc.pay.business.douyazhifu;

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
@ResponsePayHandler("DOUYAZHIFU")
public final class DouYaZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

//    参数 					类型 					说明
//    flowid				String				我方的交易订单号
//    paytime				String				对账时间，格式为 14 位标准时间：yyyyMMddHHmmss
//    fee					String				支付金额，因币种而变化，故此值不与提交时的金额做对比，以订单支付成功时的人民币金额为准。单位元
//    ext					String				支付时，渠道提交的渠道流水号，现原封返回。
//    status				String				支付状态
//    sign					String				只签名参数值，不签名参数名。
    private static final String flowid                   	="flowid";
    private static final String paytime                    	="paytime";
    private static final String fee                  		="fee";
    private static final String ext                			="ext";
    private static final String status             			="status";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("success");

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(ext);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[豆芽支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
    	String repsStatus="";
    	if(api_response_params.get(status).equals("success")){
    		repsStatus="1";
    	}else{
    		repsStatus=api_response_params.get(status);
    	}
    	String signSrc=String.format("%s|%s|%s", 
        		api_response_params.get(flowid),
        		repsStatus,
        		channelWrapper.getAPI_KEY()
        );
        String paramsStr = signSrc.toString();
        String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[豆芽支付]-[响应支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return signMD5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        String payStatusCode = api_response_params.get(status);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(fee));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount&&payStatusCode.equalsIgnoreCase("success")) {
            my_result = true;
        } else {
            log.error("[豆芽支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[豆芽支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：success");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[豆芽支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[豆芽支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return  RESPONSE_PAY_MSG;
    }
}