package dc.pay.business.hongniuzhifu3;

import java.util.Map;

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
import dc.pay.utils.UnicodeUtil;


/**
 * 
 * @author andrew
 * Aug 6, 2019
 */
@ResponsePayHandler("HONGNIUZHIFU3")
public final class HongNiuZhiFu3PayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String data         = "data";         // 参数列表
    private static final String requestTime  = "requestTime";  // 时间戳(10位)
    private static final String thirdOrderNo = "thirdOrderNo"; //经纪公司订单号
    private static final String orderNo      = "orderNo";      //订单号
    private static final String amount       = "amount";       //订单金额
    private static final String payStatus    = "payStatus";    //支付状态：待付款，已付款
//    private static final String payType           ="payType";   //支付类型 支付宝，微信
//    private static final String payTime           ="payTime";   //付款时间
//    private static final String reserved1         ="reserved1"; //预留字段1，创建回购订单时传入的，原样返回

    //signature    数据签名    32    是    　
    private static final String signature = "sign";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        JSONObject jsonObject = JSONObject.parseObject(API_RESPONSE_PARAMS.get(data));
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR     = jsonObject.getString(orderNo);
        String ordernumberR = jsonObject.getString(thirdOrderNo);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[红牛支付3]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}", ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        StringBuffer signSrc = new StringBuffer();
        signSrc.append(api_response_params.get(data));
        signSrc.append(api_response_params.get(requestTime));
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5   = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[红牛支付3]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5));
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        JSONObject jsonObject = JSONObject.parseObject(api_response_params.get(data));
        boolean    my_result  = false;
        //已付款:成功，其他失败
        String payStatusCode = jsonObject.getString(payStatus);
        payStatusCode = UnicodeUtil.unicodeToString(payStatusCode);
        String responseAmount = HandlerUtil.getFen(jsonObject.getString(amount));

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount = HandlerUtil.isAllowAmountt(db_amount, responseAmount, "100");//我平台默认允许一元偏差

        // 已付款 代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("已付款")) {
            my_result = true;
        } else {
            log.error("[红牛支付3]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[红牛支付3]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：已付款");
        return my_result;
    }


    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[红牛支付3]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[红牛支付3]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }

}