package dc.pay.business.xinweizhifu;

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
 * Feb 25, 2019
 */
@ResponsePayHandler("XINWEIZHIFU")
public final class XinWeiZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //参数名称            变量名          类型          说明
    //状态                code           String          0      成功
    //商户号              merId          String          后台分配的商户号
    //商户订单号          orderId        String          商户下单订单号
    //平台订单号          tradeId        String          平台交易订单号
    //支付类型            payWay         String          1:微信      2:支付宝      
    //价格                money          String          分，支付金额
    //自定义参数          remark         String          下单透传自定义参数
    //计费时间            time           String          yyyymmddhhmmss
    //签名串              sign           String          参照签名说明
    private static final String code                      ="code";
    private static final String merId                     ="merId";
    private static final String orderId                   ="orderId";
    private static final String tradeId                   ="tradeId";
    private static final String payWay                    ="payWay";
    private static final String money                     ="money";
    private static final String remark                    ="remark";
    private static final String time                      ="time";

    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(merId);
        String ordernumberR = API_RESPONSE_PARAMS.get(orderId);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[鑫微支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        StringBuilder signStr = new StringBuilder();
        signStr.append(code).append(api_response_params.get(code));
        signStr.append(merId).append(api_response_params.get(merId));
        signStr.append(money).append(api_response_params.get(money));
        signStr.append(orderId).append(api_response_params.get(orderId));
        signStr.append(payWay).append(api_response_params.get(payWay));
        signStr.append(remark).append(api_response_params.get(remark));
        signStr.append(time).append(api_response_params.get(time));
        signStr.append(tradeId).append(api_response_params.get(tradeId));
        signStr.append(channelWrapper.getAPI_KEY());
        String paramsStr =signStr.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toUpperCase();
        log.debug("[鑫微支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //0      成功
        String payStatusCode = api_response_params.get(code);
        String responseAmount = api_response_params.get(money);
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        
        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("0")) {
            my_result = true;
        } else {
            log.error("[鑫微支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[鑫微支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：0");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[鑫微支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[鑫微支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}