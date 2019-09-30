package dc.pay.business.longshengzhifu;

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
 * Mar 25, 2019
 */
@RequestPayHandler("LONGSHENGZHIFU")
public final class LongShengZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(LongShengZhiFuPayRequestHandler.class);

    private static final String parter          ="parter";      // 商户 id，由隆盛支付平台分配
    private static final String type            ="type";        // 银行类型
    private static final String value           ="value";       // 单位元（人民币），2位小数，最小支付金额请于商务沟通
    private static final String orderid         ="orderid";     // 商户订单号
    private static final String callbackurl     ="callbackurl"; // 下行异步通知地址
    private static final String IP              ="IP";          // 支付用户

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(parter, channelWrapper.getAPI_MEMBERID());
                put(type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(value,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(orderid,channelWrapper.getAPI_ORDER_ID());
                put(callbackurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(IP,channelWrapper.getAPI_Client_IP());
            }
        };
        log.debug("[隆盛支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //parter={}&type={}&value={}&orderid ={}&callbackurl={}key
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(parter+"=").append(api_response_params.get(parter)).append("&");
        signSrc.append(type+"=").append(api_response_params.get(type)).append("&");
        signSrc.append(value+"=").append(api_response_params.get(value)).append("&");
        signSrc.append(orderid+"=").append(api_response_params.get(orderid)).append("&");
        signSrc.append(callbackurl+"=").append(api_response_params.get(callbackurl));
        signSrc.append(channelWrapper.getAPI_KEY());
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[隆盛支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();
        try {

                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));

        } catch (Exception e) {
            log.error("[隆盛支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[隆盛支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[隆盛支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}