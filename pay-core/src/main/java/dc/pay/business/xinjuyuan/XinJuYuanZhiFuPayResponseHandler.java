package dc.pay.business.xinjuyuan;

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
 * Jan 17, 2019
 */
@ResponsePayHandler("XINJUYUANZHIFU")
public final class XinJuYuanZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //参数名               参数                 加入签名          说明
    //商户ID               partner                 Y              商户id,由分配
    //商户订单号           ordernumber             Y              上行过程中商户系统传入的ordernumber
    //订单结果             orderstatus             Y              1:支付成功，非1为支付失败
    //订单金额             paymoney                Y              单位元（人民币）
    //订单标题             subject                 Y              上行中subject原样返回
    //系统订单号           sysnumber               Y              此次交易中接口系统内的订单ID
    //MD5签名              sign                    N              32位小写MD5签名值，发起支付签名一样
    private static final String partner                        ="partner";
    private static final String ordernumber                    ="ordernumber";
    private static final String orderstatus                    ="orderstatus";
    private static final String paymoney                       ="paymoney";
//    private static final String subject                        ="subject";
//    private static final String sysnumber                      ="sysnumber";
//    private static final String sign                           ="sign";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "ok";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(partner);
        String ordernumberR = API_RESPONSE_PARAMS.get(ordernumber);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[新聚源支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(partner+"=").append(api_response_params.get(partner)).append("&");
        signSrc.append(ordernumber+"=").append(api_response_params.get(ordernumber)).append("&");
        signSrc.append(orderstatus+"=").append(api_response_params.get(orderstatus)).append("&");
        signSrc.append(paymoney+"=").append(api_response_params.get(paymoney));
        signSrc.append(channelWrapper.getAPI_KEY());
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[新聚源支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //订单结果             orderstatus             Y              1:支付成功，非1为支付失败
        String payStatusCode = api_response_params.get(orderstatus);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(paymoney));
        
        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差
        
//        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("1")) {
            my_result = true;
        } else {
            log.error("[新聚源支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[新聚源支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[新聚源支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[新聚源支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}