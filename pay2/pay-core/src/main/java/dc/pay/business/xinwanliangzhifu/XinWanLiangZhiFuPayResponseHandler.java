package dc.pay.business.xinwanliangzhifu;

import com.alibaba.fastjson.JSON;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;


/**
 * @author Cobby
 * July 09, 2019
 */
@ResponsePayHandler("XINWANLIANGZHIFU")
public final class XinWanLiangZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String mchId       = "mchId";         //    商户ID    是    String    平台分配的商户号
    private static final String mchOrderNo  = "mchOrderNo";    //    商户订单号    是    string    商户生成的订单号，必须唯一
    private static final String orderStatus = "orderStatus";   //    订单状态    是    String    2：支付成功
    private static final String amount      = "amount";        //    实际支付金额    是    String    用户实际支付的金额
//  private static final String orderNo                ="orderNo";       //    平台订单号    是    String    平台生成的订单号
//  private static final String productCode            ="productCode";   //    产品编码    是    String    支付产品，见附录
//  private static final String mchOrderAmount         ="mchOrderAmount";//    商户订单金额    是    String    商户提交订单中的金额

    //signature    数据签名    32    是    　
    private static final String signature = "sign";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR     = API_RESPONSE_PARAMS.get(mchId);
        String ordernumberR = API_RESPONSE_PARAMS.get(mchOrderNo);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[新万两支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}", ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        List          paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc   = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!signature.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //删除最后一个字符
        signSrc.deleteCharAt(signSrc.length() - 1);
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5   = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[新万两支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5));
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        // 2:成功，其他失败
        String payStatusCode  = api_response_params.get(orderStatus);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(amount));

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount = HandlerUtil.isAllowAmountt(db_amount, responseAmount, "100");//我平台默认允许一元偏差

        // 2 代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("2")) {
            my_result = true;
        } else {
            log.error("[新万两支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[新万两支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：2");
        return my_result;
    }


    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[新万两支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[新万两支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}