package dc.pay.business.zhaocaizhifu;

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
 * May 15, 2019
 */
@ResponsePayHandler("ZHAOCAIZHIFU")
public final class ZhaoCaiZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //名称  说明  类型  可否为空    最大长度
    //payKey  商户唯一标识  String  否   32
    private static final String payKey                       ="payKey";
    //orderPrice  订单金额，单位：元    保留小数点后两位    String  否   12
    private static final String orderPrice                       ="orderPrice";
    //outTradeNo  商户支付订单号 String  否   30
    private static final String outTradeNo                       ="outTradeNo";
    //productType 产品类型，请查阅本文档2.4  String  否   8
//    private static final String productType                       ="productType";
    //orderTime   下单时间，格式：yyyyMMddHHmmss  String  否   14
//    private static final String orderTime                       ="orderTime";
    //productName 支付产品名称  String  否   200
//    private static final String productName                       ="productName";
    //tradeStatus 订单状态    SUCCESS交易成功 ,其他都是等待支付   String  否   20
    private static final String tradeStatus                       ="tradeStatus";
    //successTime 成功时间，格式：yyyyMMddHHmmss  String  否   300
//    private static final String successTime                       ="successTime";
    //sign    签名  String  否   50
//    private static final String sign                       ="sign";
    //remark  备注  String  是   200
//    private static final String remark                       ="remark";
    //trxNo   交易流水号   String  否   50
//    private static final String trxNo                       ="trxNo";

    private static final String key        ="paySecret";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "SUCCESS";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(payKey);
        String ordernumberR = API_RESPONSE_PARAMS.get(outTradeNo);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[招财支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        //Map<String, String> jsonToMap = handlerUtil.jsonToMap(api_response_params.get(data));
        //或者直接取出数值类型
        //String responseAmount = JSON.parseObject(api_response_params.get(data)).getString(payMoney);
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!signature.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        signSrc.append(key +"="+ channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[招财支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //tradeStatus   订单状态        SUCCESS交易成功 ,其他都是等待支付
        String payStatusCode = api_response_params.get(tradeStatus);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(orderPrice));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        
        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差
        
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("SUCCESS")) {
            my_result = true;
        } else {
            log.error("[招财支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[招财支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：SUCCESS");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[招财支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[招财支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}