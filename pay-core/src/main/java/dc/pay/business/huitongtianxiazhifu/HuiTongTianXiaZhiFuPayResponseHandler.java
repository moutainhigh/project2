package dc.pay.business.huitongtianxiazhifu;

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
 * Jun 19, 2019
 */
@ResponsePayHandler("HUITONGTIANXIAZHIFU")
public final class HuiTongTianXiaZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    // 协议参数
    //字段名 填写类型    说明
    //orderid 必填  商户订单号
    private static final String orderid                       ="orderid";
    //ffpayorder_id   选填  汇通平台流水号
//    private static final String ffpayorder_id                       ="ffpayorder_id";
    //status  必填  支付状态:200-成功，其他为未知状态
    private static final String status                       ="status";
    //merchantNo  必填  商户号
    private static final String merchantNo                       ="merchantNo";
    //orderPrice  必填  订单金额（单位：元）
//    private static final String orderPrice                       ="orderPrice";
    //realPayAmout    必填  实际支付金额（单位：元）    与orderPrice相同
    private static final String realPayAmout                       ="realPayAmout";
    //msg 必填  返回信息BASE64加密，如需查看需要decode(不加入签名)
    private static final String msg                       ="msg";
    //sign    必填  签名
//    private static final String sign                       ="sign";

    private static final String key        ="paySecret";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "200";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(merchantNo);
        String ordernumberR = API_RESPONSE_PARAMS.get(orderid);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[汇通天下支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!msg.equals(paramKeys.get(i)) && !signature.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {                
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //最后一个&转换成#
        //signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "#" );
        signSrc.append(key +"="+ channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[汇通天下支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //status    必填  支付状态:200-成功，其他为未知状态
        String payStatusCode = api_response_params.get(status);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(realPayAmout));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        
        //tony(Tony) 01-17 15:09:28
        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("200")) {
            my_result = true;
        } else {
            log.error("[汇通天下支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[汇通天下支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：200");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[汇通天下支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[汇通天下支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}