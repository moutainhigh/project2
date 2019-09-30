package dc.pay.business.xiaoxiongbao;

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
 * Jul 24, 2018
 */
@ResponsePayHandler("XIAOXIONGBAO")
public final class XiaoXiongBaoPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //参数名                    含义                        类型              说明                                                                                                                                                            
    //orderNo                  paysapi生成的订单ID号        string(24)        必须。该笔交易在小熊宝中的支付订单                                                                                                                              
    //merchantOrderNo          您的自定义订单号             string(50)        必须。是您在发起付款接口传入的您的自定义订单号                                                                                                                  
    //money                    订单定价                     float             必须。是您在发起付款接口传入的订单价格                                                                                                                          
    //payAmount                实际支付金额                 float             必须。表示用户实际支付的金额。一般会和price值一致，如果同时存在多个用户支付同一金额，就会和price存在一定差额，差额一般在1-2分钱上下，越多人同时付款，差额越大。
    //paytype                  支付类型                      string(32)        选填。如果是收单时有传递该参数，那么该值不会为空，原样返回。                                                                                                  
    //sign                     签名                         string(32)        必须。我们把使用到的所有参数，连您的密钥一起，作md5签名。                                                                                                      
    private static final String orderNo                        ="orderNo";
    private static final String merchantOrderNo                ="merchantOrderNo";
    private static final String money                          ="money";
    private static final String payAmount                      ="payAmount";
//    private static final String paytype                        ="paytype";

    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
//        String partnerR = API_RESPONSE_PARAMS.get(merchno);
        String ordernumberR = API_RESPONSE_PARAMS.get(merchantOrderNo);
//        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[小熊宝]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        StringBuilder signStr = new StringBuilder();
        signStr.append(api_response_params.get(orderNo)).append("&");
        signStr.append(api_response_params.get(merchantOrderNo)).append("&");
        signStr.append(api_response_params.get(money)).append("&");
        signStr.append(api_response_params.get(payAmount)).append("&");
        signStr.append(channelWrapper.getAPI_KEY());
        String paramsStr =signStr.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[小熊宝]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //status        交易状态    1    是    0-未支付    1-支付成功        2-支付失败
//        String payStatusCode = api_response_params.get(status);
        String payStatusCode = "无";
//        String responseAmount = HandlerUtil.getFen(api_response_params.get(money));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        
        //业主16：允许偏差1元。详情阅读文档备案资料
        String responseAmount = HandlerUtil.getFen(api_response_params.get(payAmount));
        boolean checkAmount = handlerUtil.isAllowAmountt(db_amount, responseAmount, "100");
        
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("无")) {
            my_result = true;
        } else {
            log.error("[小熊宝]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[小熊宝]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：无");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[小熊宝]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[小熊宝]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}