package dc.pay.business.canbaobaozhifu;

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
 * 
 * @author andrew
 * Sep 27, 2019
 */
@ResponsePayHandler("CANBAOBAOZHIFU")
public final class CanBaoBaoZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //回调参数：
    //amount:  订单请求时的金额
//    private static final String amount                ="amount";
    //pay_money： 实际支付的金额， 如果该金额和订单请求金额不相等时，可以不给会员上分，也可按此实际金额上分
    private static final String pay_money                ="pay_money";
    //status:支付状态   “1”：已支付  “0”：未支付 
    private static final String status                ="status";
    //mch_id：商户ID
    private static final String mch_id                ="mch_id";
    //out_trade_no：订单请求时带的out_trade_no参数，原样返回
    private static final String out_trade_no                ="out_trade_no";
    //sign：签名
//    private static final String sign                ="sign";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "SUCCESS";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
       //String api_KEY = channelWrapper.getAPI_KEY();
       //if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 2) {
       //    log.error("[蚕宝宝支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //    throw new PayException("[蚕宝宝支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //}
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(mch_id);
        String ordernumberR = API_RESPONSE_PARAMS.get(out_trade_no);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[蚕宝宝支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }
    
    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        //Map<String, String> jsonToMap = handlerUtil.jsonToMap(api_response_params.get(data));
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!signature.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        signSrc.append(key +"="+ channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
//        System.out.println("签名源串=========>"+paramsStr);
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[蚕宝宝支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }
    
    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //status:支付状态   “1”：已支付  “0”：未支付 
        String payStatusCode = api_response_params.get(status);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(pay_money));

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        // 1 代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("1")) {
            my_result = true;
        } else {
            log.error("[蚕宝宝支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[蚕宝宝支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return my_result;
    }

    
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[蚕宝宝支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }


    @Override
    protected String responseSuccess() {
        log.debug("[蚕宝宝支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}