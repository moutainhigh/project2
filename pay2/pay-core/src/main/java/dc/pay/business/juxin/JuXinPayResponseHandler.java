package dc.pay.business.juxin;

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
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Nov 10, 2018
 */
@ResponsePayHandler("JUXIN")
public final class JuXinPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //响应参数：
    //序号          参数名              参数名称             类型             是否必填           说明
    //49.           busi_code           业务编码             String             是               详见：附件业务编码
    //50.           err_code            错误码               String             否               详见：附件错误码
    //51.           err_msg             错误信息             String             否               详见：附件错误码中错误描述
    //52.           mer_no              商户号               String             是               平台分配的唯一商户编号
    //53.           mer_order_no        商户订单号           Number             是               商户唯一订单号
    //54.           order_amount        订单金额             String             是               分为单位；整数
    //55.           order_no            平台订单号           String             是               平台唯一订单号
    //56.           order_time          订单时间             String             是               原样返回
    //57.           pay_amount          支付金额             String             是               分为单位；整数
    //58.           pay_time            支付时间             String             是               详见：数字签名
    //59.           reserver            订单保留信息         String             否               原样返回
    //60.           status              通知状态             String             是               SUCCESS：成功 FAIL:失败
    //61.           sign                数字签名             String             是               详见：数字签名
//    private static final String busi_code                                        ="busi_code";
//    private static final String err_code                                         ="err_code";
//    private static final String err_msg                                          ="err_msg";
    private static final String mer_no                                           ="mer_no";
    private static final String mer_order_no                                     ="mer_order_no";
//    private static final String order_amount                                     ="order_amount";
//    private static final String order_no                                         ="order_no";
//    private static final String order_time                                       ="order_time";
    private static final String pay_amount                                       ="pay_amount";
//    private static final String pay_time                                         ="pay_time";
//    private static final String reserver                                         ="reserver";
    private static final String status                                           ="status";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "SUCCESS";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(mer_no);
        String ordernumberR = API_RESPONSE_PARAMS.get(mer_order_no);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[聚信]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!signature.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        signSrc.append(key +"="+ channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[聚信]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //60.   status  通知状态    String  是   SUCCESS：成功 FAIL:失败
        String payStatusCode = api_response_params.get(status);
        String responseAmount = api_response_params.get(pay_amount);
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("SUCCESS")) {
            my_result = true;
        } else {
            log.error("[聚信]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[聚信]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：SUCCESS");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[聚信]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[聚信]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}