package dc.pay.business.suiyinfu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import dc.pay.utils.ValidateUtil;

/**
 * 
 * @author beck Aug 08, 2018
 */
@RequestPayHandler("SUIYINFU")
public final class SuiYinFuPayRequestHandler extends PayRequestHandler {
	private static final Logger log = LoggerFactory.getLogger(SuiYinFuPayRequestHandler.class);

	@Override
	protected Map<String, String> buildPayParam() throws PayException {
		Map<String, String> payParam = Maps.newHashMap();
		payParam.put("service", channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
		payParam.put("version", "2.0");
		payParam.put("agency_id", channelWrapper.getAPI_MEMBERID());
		payParam.put("trade_no", channelWrapper.getAPI_ORDER_ID());
		payParam.put("fee", channelWrapper.getAPI_AMOUNT());
		payParam.put("notify_url", channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
		payParam.put("nonce_str", HandlerUtil.getRandomStr(10));
			
		log.debug("[随银付]-[请求支付]-1.组装请求参数完成：{}", JSON.toJSONString(payParam));
		return payParam;
	}

	protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
		List<String> paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
		StringBuilder signSrc = new StringBuilder();
		for (int i = 0; i < paramKeys.size(); i++) {
		    String keyName = paramKeys.get(i);
		    String value = api_response_params.get(keyName);
		    if(keyName == "sign") continue;
		    
			if (StringUtils.isNotBlank(value)) {
				signSrc.append(paramKeys.get(i)).append("=").append(value).append("&");
			}
		}
		signSrc.append("key=" + channelWrapper.getAPI_KEY());
		String paramsStr = signSrc.toString();
		
		String signMd5 = Sha1Util.getSha1(paramsStr).toUpperCase();
		log.debug("[随银付]-[请求支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5));
		return signMd5;
	}

	protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign)
			throws PayException {
		payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		HashMap<String, String> result = Maps.newHashMap();

		try {

			if (HandlerUtil.isYLKJ(channelWrapper)) {
				result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam).toString());
			} else {
				
			    Map<String, Object> payParam2 = Maps.newHashMap();
			    for(String key:payParam.keySet()){
			        if(key.equalsIgnoreCase("fee")){
			            payParam2.put(key, Integer.parseInt(payParam.get(key)));
			        }else{
			            payParam2.put(key, payParam.get(key));
			        }
			    }
			    
			    String jsonStr = JSON.toJSONString(payParam2);
			    String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), jsonStr, "application/json");
			    
				JSONObject resJson = JSONObject.parseObject(resultStr);

				if (resJson != null && resJson.containsKey("status")
						&& resJson.getString("status").equalsIgnoreCase("PASS")) {
					String payUrl = resJson.getString("qr_data");
					result.put(QRCONTEXT ,payUrl);
					
				} else {
				    log.error("[随银付]-[请求支付]3.1.发送支付请求，及获取支付请求结果出错：{}", resultStr);
					throw new PayException(resultStr);
				}
			}
			
			payResultList.add(result);

		} catch (Exception e) {
			log.error("[随银付]-[请求支付]3.2.发送支付请求，及获取支付请求结果出错：", e);
			throw new PayException(e.getMessage(), e);
		}
		log.debug("[随银付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));

		return payResultList;
	}

	protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
		RequestPayResult requestPayResult = new RequestPayResult();
		if (null != resultListMap && !resultListMap.isEmpty()) {
			if (resultListMap.size() == 1) {
				Map<String, String> resultMap = resultListMap.get(0);
				requestPayResult = buildResult(resultMap, channelWrapper, requestPayResult);
			}
			if (ValidateUtil.requestesultValdata(requestPayResult)) {
				requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
			} else {
				throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
			}
		} else {
			throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
		}
		log.debug("[随银付]-[请求支付]-4.处理请求响应成功：{}", JSON.toJSONString(requestPayResult));
		return requestPayResult;
	}
	
}