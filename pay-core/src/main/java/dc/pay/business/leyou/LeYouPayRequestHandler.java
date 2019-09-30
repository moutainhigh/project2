package dc.pay.business.leyou;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import dc.pay.business.RequestPayResult;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.UnicodeUtil;
import dc.pay.utils.ValidateUtil;

/**
 *
 * 
 * @author kevin
 * Aug 10, 2018
 */
@RequestPayHandler("LEYOU")
public final class LeYouPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(LeYouPayRequestHandler.class);

    private static final String      fxid	  	  	  = "fxid";                         
    private static final String      fxddh	  	  	  = "fxddh";                       
    private static final String      fxdesc	  		  = "fxdesc";                       
    private static final String      fxfee	      	  = "fxfee";                         
    private static final String      fxnotifyurl	  = "fxnotifyurl";                         
    private static final String      fxbackurl	  	  = "fxbackurl";                        
    private static final String      fxpay	  		  = "fxpay";                       
    private static final String      fxip	  	  	  = "fxip";                

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
            	put(fxid,channelWrapper.getAPI_MEMBERID());
                put(fxddh,channelWrapper.getAPI_ORDER_ID());
                put(fxdesc,"GOODS");
                put(fxfee,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(fxnotifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(fxbackurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(fxpay,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(fxip,channelWrapper.getAPI_Client_IP());
            }
        };
        log.debug("[乐游]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        StringBuilder signStr = new StringBuilder();
        signStr.append(fxid+"=").append(api_response_params.get(fxid)).append("&");
        signStr.append(fxddh+"=").append(api_response_params.get(fxddh)).append("&");
        signStr.append(fxfee+"=").append(api_response_params.get(fxfee)).append("&");
        signStr.append(fxnotifyurl+"=").append(api_response_params.get(fxnotifyurl)).append("&");
        signStr.append(channelWrapper.getAPI_KEY());
        String paramsStr = signStr.toString();
        String pay_md5sign = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[乐游]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
        if (StringUtils.isBlank(resultStr)) {
            log.error("[乐游]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(payParam));
            throw new PayException("返回空,参数："+JSON.toJSONString(payParam));
        }
        resultStr = UnicodeUtil.unicodeToString(resultStr);
        if (!resultStr.contains("{") || !resultStr.contains("}")) {
            log.error("[乐游]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        JSONObject resJson = null;
        try {
            resJson = JSONObject.parseObject(new String(resultStr.getBytes("ISO-8859-1"), "GBK"));
        } catch (Exception e) {
            log.error("[乐游]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            //log.error("[盛盈付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + resultStr + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(e.getMessage(),e);
        }
        Map result = Maps.newHashMap();
        //只取正确的值，其他情况抛出异常
        if(null !=resJson && resJson.containsKey("status") && "1".equalsIgnoreCase(resJson.getString("status")) && resJson.containsKey("payurl")){
            result.put(JUMPURL, HandlerUtil.UrlDecode(resJson.getString("payurl")));
        }else {
            log.error("[乐游]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		payResultList.add(result);
        log.debug("[乐游]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
        return payResultList;
    }

    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (null != resultListMap && !resultListMap.isEmpty()) {
            if (resultListMap.size() == 1) {
                Map<String, String> resultMap = resultListMap.get(0);
                requestPayResult = buildResult(resultMap,channelWrapper,requestPayResult);
            }
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[乐游]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }

}