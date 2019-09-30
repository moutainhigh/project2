package dc.pay.business.haifu;

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
 * Jun 24, 2019
 */
@ResponsePayHandler("HAIFU")
public final class HaiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //参数名 参数值 回调测试值   备注
    //请求网关地址  URL http://XXXX.com/notify.php  
    //传递方式    POST        
    //            
    //订单状态    status  1   会传递各种可能，需要验证一下传递过去的值    0未付 1已付款 2已冻结 3已关闭
    private static final String status                ="status";
    //商户ID    customerid  11050   
    private static final String customerid                ="customerid";
    //商户提交的订单编号   sdorderno   1551457040  
    private static final String sdorderno                ="sdorderno";
    //支付金额    total_fee   10.00   带有小数点
    private static final String total_fee                ="total_fee";
    //支付类型    paytype unionpay    
    private static final String paytype                ="paytype";
    //三方系统编号  sdpayno 2019030121511525619 
    private static final String sdpayno                ="sdpayno";
    //备注信息    remark  空   可能为空
//    private static final String remark                ="remark";
    //签名  sign    039ba42e23be836e6b18f9ec5b9d5c60    32位小写MD5签名值
//    private static final String sign                ="sign";
    
//    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
       //String api_KEY = channelWrapper.getAPI_KEY();
       //if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 2) {
       //    log.error("[海付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //    throw new PayException("[海付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //}
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(customerid);
        String ordernumberR = API_RESPONSE_PARAMS.get(sdorderno);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[海付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        StringBuilder signStr = new StringBuilder();
        signStr.append(customerid+"=").append(api_response_params.get(customerid)).append("&");
        signStr.append(status+"=").append(api_response_params.get(status)).append("&");
        signStr.append(sdpayno+"=").append(api_response_params.get(sdpayno)).append("&");
        signStr.append(sdorderno+"=").append(api_response_params.get(sdorderno)).append("&");
        signStr.append(total_fee+"=").append(api_response_params.get(total_fee)).append("&");
        signStr.append(paytype+"=").append(api_response_params.get(paytype)).append("&");
        signStr.append(channelWrapper.getAPI_KEY());
        String paramsStr =signStr.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[海付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //订单状态  status  1   会传递各种可能，需要验证一下传递过去的值        0未付 1已付款 2已冻结 3已关闭
        String payStatusCode = api_response_params.get(status);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(total_fee));

        //tony(Tony) 01-17 15:09:28
        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        //boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("1")) {
            my_result = true;
        } else {
            log.error("[海付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[海付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[海付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[海付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}