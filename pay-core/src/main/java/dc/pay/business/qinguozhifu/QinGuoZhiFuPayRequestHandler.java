package dc.pay.business.qinguozhifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.Sha1Util;
import dc.pay.utils.UnicodeUtil;
import dc.pay.utils.ValidateUtil;


/**
 * 
 * 
 * @author sunny
 * Dec 18, 2018
 */
@RequestPayHandler("QINGUOZHIFU")
public final class QinGuoZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(QinGuoZhiFuPayRequestHandler.class);

//    字段名				字段说明				最大长度				是否必填				备注
//    merchno			商户号				8					是				商户注册签约后，支付平台分配的唯一标识号
//    traceno			商户订单号			32					是				由商户系统生成的唯一订单编号，最大长度为32位
//    paytype			支付方式				Strign				是				见支付方式说明
//    total_fee			商户订单总金额			Number(10)			是				订单总金额以分为单位
//    notifyurl			通知地址				100					是				支付成功后，支付平台会主动通知商家系统，因此商家必须指定接收通知的地址，例如：https://www.baidu.com/
//    returnurl			支付跳转地址			100					是				支付跳转地址
//    sign				数据签名				32					是				对签名数据进行两次MD5加密后转大写

    private static final String merchno               	="merchno";
    private static final String traceno           		="traceno";
    private static final String paytype           		="paytype";
    private static final String total_fee           	="total_fee";
    private static final String notifyurl          		="notifyurl";
    private static final String returnurl               ="returnurl";
    
    private static final String sign                ="sign";
    private static final String key                 ="key";
    
    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merchno, channelWrapper.getAPI_MEMBERID());
                put(traceno,channelWrapper.getAPI_ORDER_ID());
                put(paytype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(total_fee,channelWrapper.getAPI_AMOUNT());
                put(notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(returnurl,channelWrapper.getAPI_WEB_URL());
            }
        };
        log.debug("[嗪果支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
    	List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
          if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
          	signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
          }
        }
      signSrc.append(key+"="+channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMD5 = HandlerUtil.getMD5UpperCase(HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase());
        log.debug("[嗪果支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return signMD5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        if (1==2&&(HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)  ||  HandlerUtil.isWapOrApp(channelWrapper))) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());  //.replace("method='post'","method='get'"));
            payResultList.add(result);
        }else{
        	String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
	        if (StringUtils.isBlank(resultStr)) {
	            log.error("[嗪果支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(EMPTYRESPONSE);
	        }
	        resultStr = UnicodeUtil.unicodeToString(resultStr);
	        resultStr=resultStr.replaceAll("\\\\", "");
	        if (!resultStr.contains("{") || !resultStr.contains("}")) {
	            log.error("[嗪果支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        JSONObject resJson;
	        try {
	            resJson = JSONObject.parseObject(resultStr);
	        } catch (Exception e) {
	            e.printStackTrace();
	            log.error("[嗪果支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        if (null != resJson && resJson.containsKey("respCode") && resJson.getString("respCode").equals("E0000")) {
	            String code_url = resJson.getString("barUrl");
	            result.put(JUMPURL , code_url);
	        }else {
	            log.error("[嗪果支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        payResultList.add(result);
        }
        log.debug("[嗪果支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[嗪果支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}