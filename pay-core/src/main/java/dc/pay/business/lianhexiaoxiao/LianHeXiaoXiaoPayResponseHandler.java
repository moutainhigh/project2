package dc.pay.business.lianhexiaoxiao;

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
 * Dec 11, 2018
 */
@ResponsePayHandler("LIANHEXIAOXIAO")
public final class LianHeXiaoXiaoPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //merchno        商户号        15    是    　
    //status         交易状态      1     是    0-未支付    1-支付成功        2-支付失败
    //traceno        商户流水号    30    是    商家的流水号
    //orderno        系统订单号    12    是    系统订单号,同上面接口的refno。
    //merchName      商户名称      30    是    　
    //amount         交易金额      12    是    单位/元
    //transDate      交易日期      10    是    　
    //transTime      交易时间      8     是    　
    //payType        支付方式      1     是    1-支付宝    2-微信    3-百度钱包    4-QQ钱包    5-京东钱包    
    //openId         用户OpenId    50    否    支付的时候返回
    
    //商户ID  mchId   是   String(30)  20001222    支付中心分配的商户号
    private static final String mchId                ="mchId";
    //商户订单号 mchOrderNo  是   String(30)  20160427210604000490    商户生成的订单号
    private static final String mchOrderNo                 ="mchOrderNo";
    //支付金额  amount  是   int 100 支付金额,单位分
    private static final String amount                ="amount";
    //入账金额    income  是   int 100 入账金额,单位分
//    private static final String income                ="income";
    //状态    status  是   int 1   支付状态,0-订单生成,1-支付中,2-支付成功,3-业务处理完成
    private static final String status              ="status";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(mchId);
        String ordernumberR = API_RESPONSE_PARAMS.get(mchOrderNo);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[联合小小]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!signature.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        signSrc.append(key +"="+ channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[联合小小]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //状态    status  是   int 1   支付状态,0-订单生成,1-支付中,2-支付成功,3-业务处理完成
        String payStatusCode = api_response_params.get(status);
        String responseAmount = api_response_params.get(amount );
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
//        boolean checkAmount =  HandlerUtil.isRightAmount(db_amount,responseAmount,"100");//第三方回调金额差额1元内
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("2")) {
            my_result = true;
        } else {
            log.error("[联合小小]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[联合小小]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：2");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[联合小小]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[联合小小]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}