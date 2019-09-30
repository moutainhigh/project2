package dc.pay.business.vvpay;

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
 * Mar 4, 2019
 */
@ResponsePayHandler("VVPAYZHIFU")
public final class VVPayZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String xxorder              ="xxorder"; // VVPay系统的订单号,唯一
    private static final String order                ="order";   //你方订单的订单号
    private static final String paytype              ="paytype"; //支付类型
    private static final String fee                  ="fee";     //订单实际付款金额
    private static final String paytm                ="paytm";   //订单完成时间
    //signature    数据签名    32    是    　
    private static final String signature  ="safe";

    private static final String RESPONSE_PAY_MSG = "ok";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(order);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[VVPay支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        String paramsStr = String.format("%s%s%s%s%s%s",
                params.get(xxorder),
                params.get(order ),
                params.get(paytype ),
                params.get(fee ),
                params.get(paytm ),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[VVPay支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
//        //status        交易状态    1    是    0-未支付    1-支付成功        2-支付失败
//        String payStatusCode = api_response_params.get(status);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(fee));

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
//        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount) {
            my_result = true;
        } else {
            log.error("[VVPay支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() +  " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[VVPay支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount );
        return my_result;
    }

    
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[VVPay支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }


    @Override
    protected String responseSuccess() {
        log.debug("[VVPay支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}