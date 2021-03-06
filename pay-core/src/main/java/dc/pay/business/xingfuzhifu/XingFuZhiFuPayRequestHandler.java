package dc.pay.business.xingfuzhifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

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
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.Sha1Util;
import dc.pay.utils.UnicodeUtil;
import dc.pay.utils.ValidateUtil;


/**
 * @author sunny
 * 21 01, 2019
 */
@RequestPayHandler("XINGFUZHIFU")
public final class XingFuZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XingFuZhiFuPayRequestHandler.class);

//    参数名称		参数描述			说明				是否必录		是否签名
//    userId		商户号			平台提供给商户的ID	是			是
//    payAmt		支付金额			单位：元			是			是
//    bankId		银行id			交易类型41的网银支付，代码参见银行代码附表1的bankCode列。	是	否

    private static final String userId               ="userId";
    private static final String orderNo           	 ="orderNo";
    private static final String tradeType            ="tradeType";
    private static final String payAmt           	 ="payAmt";
    private static final String bankId          	 ="bankId";
    private static final String goodsName            ="goodsName";
    private static final String returnUrl            ="returnUrl";
    private static final String notifyUrl            ="notifyUrl";
    private static final String sign                 ="sign";
    
    private static final String key                  ="key";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(userId, channelWrapper.getAPI_MEMBERID());
                put(orderNo,channelWrapper.getAPI_ORDER_ID());
                put(payAmt,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(tradeType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
                put(returnUrl,channelWrapper.getAPI_WEB_URL());
                put(bankId,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
                put(goodsName,channelWrapper.getAPI_ORDER_ID());
            }
        };
        log.debug("[星福支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        paramKeys.remove(bankId);
        paramKeys.remove(goodsName);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
//            if (!pay_productname.equals(paramKeys.get(i)) && !pay_attach.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
            if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
            	signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //最后一个&转换成#
        //signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "#" );
        signSrc.append(key+"="+channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[星福支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
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
	        //String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,null);
	        //Str StringBuilder signSrc = new StringBuilder();
        	List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
        	StringBuilder signSrc = new StringBuilder();
            for (int i = 0; i < paramKeys.size(); i++) {
//              if (!pay_productname.equals(paramKeys.get(i)) && !pay_attach.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
              if (StringUtils.isNotBlank(payParam.get(paramKeys.get(i)))) {
              	signSrc.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
              }
            }
            signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "" );	
        	String resultStr= HttpUtil.sendPost(channelWrapper.getAPI_CHANNEL_BANK_URL(), signSrc.toString());
	        if (StringUtils.isBlank(resultStr)) {
	            log.error("[星福支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(EMPTYRESPONSE);
	        }
	        resultStr = UnicodeUtil.unicodeToString(resultStr);
	        if (!resultStr.contains("{") || !resultStr.contains("}")) {
	            log.error("[星福支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        JSONObject resJson;
	        try {
	            resJson = JSONObject.parseObject(resultStr);
	        } catch (Exception e) {
	            e.printStackTrace();
	            log.error("[星福支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        if (null != resJson && resJson.containsKey("retCode") && resJson.getString("retCode").equals("0")) {
	        	String payUrl=resJson.getString("payUrl");
	        	if(StringUtils.isNotBlank(payUrl) && payUrl.contains("<form") && !payUrl.contains("{")){
	                result.put(HTMLCONTEXT,payUrl);
	                //payResultList.add(result);
	            }
	        }else {
	            log.error("[星福支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        payResultList.add(result);
        }
        log.debug("[星福支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[星福支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}