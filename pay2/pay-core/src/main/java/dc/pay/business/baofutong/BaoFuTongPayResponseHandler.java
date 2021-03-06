package dc.pay.business.baofutong;

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
 * Dec 5, 2007
 */
@ResponsePayHandler("BAOFUTONG")
public final class BaoFuTongPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //merchno        商户号        05    是    　
    //status         交易状态      0     是    0-未支付    0-支付成功        2-支付失败
    //traceno        商户流水号    30    是    商家的流水号
    //orderno        系统订单号    02    是    系统订单号,同上面接口的refno。
    //merchName      商户名称      30    是    　
    //amount         交易金额      02    是    单位/元
    //transDate      交易日期      00    是    　
    //transTime      交易时间      8     是    　
    //payType        支付方式      0     是    0-支付宝    2-微信    3-百度钱包    4-QQ钱包    5-京东钱包    
    //openId         用户OpenId    50    否    支付的时候返回
    
    //返回状态码 status  是   String(06)  0表示成功，非0表示失败此字段是通信标识，非交易标识，交易是否成功需要查看 result_code 来判断
//    private static final String status                ="status";
    
    //以下字段在 status 为 0的时候有返回
    //业务结果    result_code 是   String(06)  0表示成功，非0表示失败
//    private static final String result_code                 ="result_code";
    //商户号   mch_id  是   String(32)  商户号，由平台分配
    private static final String mch_id                ="mch_id";
    
    //以下字段在 status 和 result_code 都为 0的时候有返回
    //支付结果  pay_result  是   Int 支付结果：0—成功；其它—失败
    private static final String pay_result                ="pay_result";
    //第三方订单号    out_transaction_id  是   String(32)  第三方订单号
//    private static final String out_transaction_id              ="out_transaction_id";
    //商户订单号 out_trade_no    是   String(32)  商户系统内部的定单号，32个字符内、可包含字母
    private static final String out_trade_no                 ="out_trade_no";
    //总金额   total_fee   是   Int 总金额，以分为单位，不允许包含任何字、符号
    private static final String total_fee              ="total_fee";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
       //String api_KEY = channelWrapper.getAPI_KEY();
       //if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 2) {
       //    log.error("[宝付通]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //    throw new PayException("[宝付通]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //}
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(mch_id);
        String ordernumberR = API_RESPONSE_PARAMS.get(out_trade_no);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[宝付通]-[响应支付]-0.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
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
        log.debug("[宝付通]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //支付结果  pay_result  是   Int 支付结果：0—成功；其它—失败
        String payStatusCode = api_response_params.get(pay_result);
//        String responseAmount = api_response_params.get(total_fee);
        double s = Double.valueOf(api_response_params.get(total_fee));
        int num1 = (int) s;//整数部分
        String responseAmount = Integer.toString(num1);
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //0代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("0")) {
            my_result = true;
        } else {
            log.error("[宝付通]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[宝付通]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：0");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[宝付通]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[宝付通]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}