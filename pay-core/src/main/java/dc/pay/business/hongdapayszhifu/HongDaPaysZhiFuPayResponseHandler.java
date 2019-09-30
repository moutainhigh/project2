package dc.pay.business.hongdapayszhifu;

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
 * @author Cobby
 * Mar 6, 2019
 */
@ResponsePayHandler("HONGDAPAYSZHIFU")
public final class HongDaPaysZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());
//"type"=>支付通道alipay
//"money"=>支付金额
//"extend"=>扩展信息
//"out_order_id"=>商户订单号
//"no"=>支付宝交易号
//"pid"=>商户id
//"sign"=>数字签名
//异步返回数字签名规则：
//md5(pid+type+no+money+ extend+out_order_id+apikey)
    private static final String pid                   ="pid";
    private static final String type                  ="type";
    private static final String no                    ="no";
    private static final String money                 ="money";
    private static final String extend                ="extend";
    private static final String out_order_id          ="out_order_id";

    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
       //String api_KEY = channelWrapper.getAPI_KEY();
       //if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 2) {
       //    log.error("[宏达pays支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //    throw new PayException("[宏达pays支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //}
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(out_order_id);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[宏达pays支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
            //md5(pid+type+no+money+ extend+out_order_id+apikey)
        String paramsStr = String.format("%s%s%s%s%s%s%s",
                api_response_params.get(pid),
                api_response_params.get(type),
                api_response_params.get(no),
                api_response_params.get(money),
                api_response_params.get(extend),
                api_response_params.get(out_order_id),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[宏达pays支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //status        交易状态    1    是    0-未支付    1-支付成功        2-支付失败
//        String payStatusCode = api_response_params.get(status);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(money));

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        //boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount) {
            my_result = true;
        } else {
            log.error("[宏达pays支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID()  + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[宏达pays支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount );
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[宏达pays支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[宏达pays支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}