package dc.pay.business.xintongbao;

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
import dc.pay.utils.Sha1Util;

/**
 * 
 * @author sunny
 * Dec 14, 2018
 */
@ResponsePayHandler("XINTONGBAO")
public final class XinTongBaoPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

//    参数名称			变量名				类型长度			说明
//    状态				status				varchar(10)		success:成功，fail失败
//    版本号				bb					varchar(10)		1.0
//    商户编号			shid				int(8)			订单对应的商户ID
//    商户订单号			ddh					varchar(20)		商户网站上的订单号
//    订单金额			je					decimal(18,2)	支付金额
//    支付通道			zftd				varchar(10)		渠道代码
//    异步通知URL			ybtz				varchar(50)		POST异步通知
//    同步跳转URL			tbtz				varchar(50)		GET同步跳转
//    订单名称			ddmc				varchar(50)		支付订单的名称
//    订单备注			ddbz				varchar(50)		支付订单的备注
//    md5签名串			sign				varchar(32)		参照通知MD5签名

    private static final String status                   	="status";
    private static final String bb                    		="bb";
    private static final String shid                  		="shid";
    private static final String ddh                			="ddh";
    private static final String je             				="je";
    private static final String zftd                 		="zftd";
    private static final String ybtz              			="ybtz";
    private static final String tbtz              			="tbtz";
    private static final String ddmc              			="ddmc";
    private static final String ddbz              			="ddbz";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("success");

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(shid);
        String ordernumberR = API_RESPONSE_PARAMS.get(ddh);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[新通宝支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
    	String signSrc=String.format("%s%s%s%s%s%s%s%s%s%s%s",
    			status+"="+api_response_params.get(status)+"&",
    			shid+"="+api_response_params.get(shid)+"&",
    			bb+"="+api_response_params.get(bb)+"&",
    			zftd+"="+api_response_params.get(zftd)+"&",
    			ddh+"="+api_response_params.get(ddh)+"&",
    			je+"="+api_response_params.get(je)+"&",
    			ddmc+"="+api_response_params.get(ddmc)+"&",
    			ddbz+"="+api_response_params.get(ddbz)+"&",
    			ybtz+"="+api_response_params.get(ybtz)+"&",
    			tbtz+"="+api_response_params.get(tbtz)+"&",
    			channelWrapper.getAPI_KEY()
    	);
        String paramsStr = signSrc.toString();
        String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[新通宝支付]-[响应支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return signMD5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        String payStatusCode = api_response_params.get(status);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(je));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount&&payStatusCode.equalsIgnoreCase("success")) {
            my_result = true;
        } else {
            log.error("[新通宝支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[新通宝支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：success");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[新通宝支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[新通宝支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return  RESPONSE_PAY_MSG;
    }
}