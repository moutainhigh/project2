package dc.pay.business.huifubaozhifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import dc.pay.business.RequestPayResult;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.ValidateUtil;

/**
 * @author Cobby
 * Mar 5, 2019
 */
@RequestPayHandler("HUIFUBAOZHIFU")
public final class HuiFuBaoZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(HuiFuBaoZhiFuPayRequestHandler.class);

    private static final String version               ="version";     //默认1.0
    private static final String customerid            ="customerid";  //商户编号
    private static final String sdorderno             ="sdorderno";   //商户订单号
    private static final String total_fee             ="total_fee";   //订单金额  精确到小数点后两位，例10.00
    private static final String paytype               ="paytype";     //支付编号
    private static final String notifyurl             ="notifyurl";    //异步通知URL
    private static final String returnurl             ="returnurl";   //同步跳转URL
    private static final String remark                ="remark";      //订单备注说明
    private static final String get_code              ="get_code";    //获取二维码链接值1为获取 值0不获取，可为空。


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(version,"1.0");
                put(customerid, channelWrapper.getAPI_MEMBERID());
                put(sdorderno,channelWrapper.getAPI_ORDER_ID());
                put(total_fee,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(paytype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(returnurl,channelWrapper.getAPI_WEB_URL());
                put(remark,"remark");
                put(get_code,"0");
            }
        };
        log.debug("[汇付宝]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
//        version={value}&customerid={value}&total_fee={value}&sdorderno={value}&notifyurl={value}&returnurl={value}&{apikey};
        String paramsStr = String.format("version=%s&customerid=%s&total_fee=%s&sdorderno=%s&notifyurl=%s&returnurl=%s&%s",
                api_response_params.get(version),
                api_response_params.get(customerid),
                api_response_params.get(total_fee),
                api_response_params.get(sdorderno),
                api_response_params.get(notifyurl),
                api_response_params.get(returnurl),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[汇付宝]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();
        try {

                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());

        } catch (Exception e) {
            log.error("[汇付宝]-[请求支付]-3.1.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[汇付宝]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
        return payResultList;
    }

    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (null != resultListMap && !resultListMap.isEmpty()) {
            if (resultListMap.size() == 1) {
                Map<String, String> resultMap = resultListMap.get(0);
                requestPayResult=  buildResult(resultMap, channelWrapper,requestPayResult);
            }
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[汇付宝]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}