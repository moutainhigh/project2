package dc.pay.business.diliuzhifu;

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
 * Apr 13, 2019
 */
@ResponsePayHandler("DILIUZHIFU")
public final class DiLiuZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String mchNo                ="mchNo";   //商户ID       mchNo   Y
    private static final String tradeno             ="tradeno";  //商户订单号    tradeno y
    private static final String money               ="money";    //订单金额      money   Y
    private static final String orderid             ="orderid";  //第六支付订单号 orderid Y
    private static final String paytype             ="paytype";  //支付类型      paytype Y
    private static final String status              ="status";   //状态         status  Y
    private static final String attach              ="attach";   //备注信息      attach  Y

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(mchNo);
        String ordernumberR = API_RESPONSE_PARAMS.get(tradeno);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[第六支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        //attach=值 &mchNo=值&money=值&orderid=值&paytype=值&status=值&tradeno=值&key=$key
        StringBuilder signStr = new StringBuilder();
        signStr.append(attach+"=").append(api_response_params.get(attach)).append("&");
        signStr.append(mchNo+"=").append(api_response_params.get(mchNo)).append("&");
        signStr.append(money+"=").append(api_response_params.get(money)).append("&");
        signStr.append(orderid+"=").append(api_response_params.get(orderid)).append("&");
        signStr.append(paytype+"=").append(api_response_params.get(paytype)).append("&");
        signStr.append(status+"=").append(api_response_params.get(status)).append("&");
        signStr.append(tradeno+"=").append(api_response_params.get(tradeno)).append("&");
        signStr.append(key+"=").append(channelWrapper.getAPI_KEY());
        String paramsStr =signStr.toString();
        
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[第六支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //状态 success 为已支付，failed为未支付
        String payStatusCode = api_response_params.get(status);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(money));

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("success")) {
            my_result = true;
        } else {
            log.error("[第六支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[第六支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：success");
        return my_result;
    }

    
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[第六支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[第六支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}