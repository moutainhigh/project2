package dc.pay.business.huidezhifu;

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

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * May 27, 2019
 */
@ResponsePayHandler("HUIDEZHIFU")
public final class HuiDeZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //参数名称    变量名 类型长度    说明
    //订单状态    status  int(1)  1:成功，其他失败
    private static final String status                     ="status";
    //商户编号    customerid  int(8)  
    private static final String customerid                     ="customerid";
    //平台订单号   sdpayno varchar(20) 
    private static final String sdpayno                     ="sdpayno";
    //商户订单号   sdorderno   varchar(20) 
    private static final String sdorderno                     ="sdorderno";
    //交易金额    total_fee   decimal(10,2)   最多两位小数
    private static final String total_fee                     ="total_fee";
    //支付类型    paytype varchar(20) 
    private static final String paytype                     ="paytype";
    //订单备注说明  remark  varchar(50) 原样返回
//    private static final String remark                     ="remark";
    //微信订单号   transactionId   Varchar(100)    不一定有（没有时为0）
//    private static final String transactionId                     ="transactionId";

    //md5验证签名串    sign    varchar(32) 参照签名方法
//    private static final String sign                     ="sign";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(customerid);
        String ordernumberR = API_RESPONSE_PARAMS.get(sdorderno);
        
//        String ordernumberR = API_RESPONSE_PARAMS.get(fxddh);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[汇德支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(customerid+"=").append(api_response_params.get(customerid)).append("&");
        signSrc.append(status+"=").append(api_response_params.get(status)).append("&");
        signSrc.append(sdpayno+"=").append(api_response_params.get(sdpayno)).append("&");
        signSrc.append(sdorderno+"=").append(api_response_params.get(sdorderno)).append("&");
        signSrc.append(total_fee+"=").append(api_response_params.get(total_fee)).append("&");
        signSrc.append(paytype+"=").append(api_response_params.get(paytype)).append("&");
        signSrc.append(api_key);
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[汇德支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {

        boolean my_result = false;
        //1:成功，其他失败
        String payStatusCode = api_response_params.get(status);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(total_fee));

        //tony(Tony) 01-17 15:09:28
        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("1")) {
            my_result = true;
        } else {
            log.error("[汇德支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[汇德支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {

        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[汇德支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[汇德支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}