package dc.pay.business.yingfu;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

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
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Jan 26, 2018
 */
@RequestPayHandler("YINGFU")
public final class YingFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YingFuPayRequestHandler.class);
	
	//pay_memberid			商户号				是			是			平台分配商户号
	//pay_orderid			订单号				是			是			上送订单号唯一, 字符长度20
	//pay_applydate			提交时间				是			是			时间格式：2016-12-26 18:18:18
	//pay_bankcode			银行编码				是			是			参考后续说明
	//pay_notifyurl			服务端通知				是			是			服务端返回地址.（POST返回数据）
	//pay_callbackurl		页面跳转通知			是			是			页面跳转返回地址（POST返回数据）
	//pay_amount			订单金额				是			是			商品金额
	//pay_md5sign			MD5签名				是			否			请看MD5签名字段格式
	private static final String pay_memberid	="pay_memberid";
	private static final String pay_orderid		="pay_orderid";
	private static final String pay_applydate	="pay_applydate";
	private static final String pay_bankcode	="pay_bankcode";
	private static final String pay_notifyurl	="pay_notifyurl";
	private static final String pay_callbackurl	="pay_callbackurl";
	private static final String pay_amount		="pay_amount";
//	private static final String pay_md5sign		="pay_md5sign";
	private static final String pay_productname	="pay_productname";
	
    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
            	//pay_md5sign			MD5签名				是			否			请看MD5签名字段格式
            	put(pay_memberid, channelWrapper.getAPI_MEMBERID());
            	put(pay_orderid,channelWrapper.getAPI_ORDER_ID());
            	put(pay_applydate,HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyy-MM-dd HH:mm:ss"));
            	put(pay_bankcode ,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            	put(pay_notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            	put(pay_callbackurl, channelWrapper.getAPI_WEB_URL());
            	//元还是分？ TODO andrew
            	put(pay_amount ,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            	put(pay_productname ,"name");
            }
        };
        log.debug("[盈付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

	protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
		List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
		StringBuilder signSrc = new StringBuilder();
		for (int i = 0; i < paramKeys.size(); i++) {
		    if (!"pay_productname".equalsIgnoreCase(paramKeys.get(i)+"") && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
				signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
			}
		}
		signSrc.append("key=" + channelWrapper.getAPI_KEY());
		String paramsStr = signSrc.toString();
		String pay_md5sign = HandlerUtil.getMD5UpperCase(paramsStr).toUpperCase();
		log.debug("[盈付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
		return pay_md5sign;
	}
	 
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
    	payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        List<Map<String, String>> payResultList = Lists.newArrayList();
        String api_CHANNEL_BANK_URL = channelWrapper.getAPI_CHANNEL_BANK_URL();
        Map<String,String> result = Maps.newHashMap();
        if(HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || handlerUtil.isWebYlKjzf(channelWrapper)){
            StringBuffer htmlContent = HandlerUtil.getHtmlContent(api_CHANNEL_BANK_URL, payParam);
            //保存第三方返回值
            result.put(HTMLCONTEXT,htmlContent.toString());
        }else {
        	String resultStr = RestTemplateUtil.sendByRestTemplate(api_CHANNEL_BANK_URL, payParam, String.class, HttpMethod.POST).trim();
        	if (StringUtils.isBlank(resultStr)) {
        		log.error("[盈付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空"+",参数："+JSON.toJSONString(payParam));
        		throw new PayException("返回空"+",参数："+JSON.toJSONString(payParam));
        	}
        	if (!resultStr.contains("form")) {
        		log.error("[盈付]-[请求支付]-3.2.发送支付请求，获取支付请求返回值异常:"+resultStr);
                throw new PayException(resultStr);
			}
        	result.put("第三方返回1", resultStr);
        	Document document1 = Jsoup.parse(resultStr);  //Jsoup.parseBodyFragment(html)
        	Element bodyEl1 = document1.getElementsByTag("body").first();
        	Element formEl1 = bodyEl1.getElementsByTag("form").first();
        	Map<String, String> secondPayParam = HandlerUtil.parseFormElement(formEl1);
        	resultStr = RestTemplateUtil.sendByRestTemplateRedirect(secondPayParam.get("action"), secondPayParam, String.class, HttpMethod.POST).trim();
        	if (StringUtils.isBlank(resultStr)) {
        		log.error("[盈付]-[请求支付]-3.3.发送支付请求，获取支付请求返回值异常:返回空"+",参数："+JSON.toJSONString(payParam));
        		throw new PayException("返回空"+",参数："+JSON.toJSONString(payParam));
        	}
        	result.put("第三方返回2",resultStr); //保存全部第三方信息，上面的拆开没必要
        	if (channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("_ZFB_SM")) {
        		result.put(HTMLCONTEXT, resultStr);
			}else {
				Document document2 = Jsoup.parse(resultStr);  //Jsoup.parseBodyFragment(html)
				if (channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("YINGFU_BANK_WEBWAPAPP_WX_SM")) {
					String attr = document2.getElementsByTag("body").select("iframe").first().attr("src");
					String resultStr2 = RestTemplateUtil.sendByRestTemplate(attr, payParam, String.class, HttpMethod.GET).trim();
					if (StringUtils.isBlank(resultStr2)) {
						log.error("[盈付]-[请求支付]--[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr2) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
		        		throw new PayException(resultStr2);
					}
					result.put("第三方返回3",resultStr2); //保存全部第三方信息，上面的拆开没必要
					String replaceBlank = HandlerUtil.replaceBlank(resultStr2);
					String[] weixins = replaceBlank.split("weixin:");
					if (null == weixins || weixins.length < 1 ) {
						log.error("[盈付]-[请求支付]--[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr2) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
		        		throw new PayException(resultStr2);
					}
					String tmp = weixins[1];
					result.put(QRCONTEXT, tmp.contains("?") ?  "weixin:"+tmp.split("\'")[0] : null);
				}else {
					Elements el2 = document2.getElementsByTag("body").select("form img");
					if (null == el2 || el2.size() < 1) {
						log.error("[盈付]-[请求支付]-3.4.发送支付请求，获取支付请求返回值异常:"+resultStr);
						throw new PayException(resultStr);
					}
					String attr = el2.get(1).attr("src");
					result.put(QRCONTEXT, attr.contains("=") ? attr.split("=")[1] : attr);
				}
			}
        	result.put("第三方返回2",resultStr); //保存全部第三方信息，上面的拆开没必要
		}
        payResultList.add(result);
        log.debug("[盈付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
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
        log.debug("[盈付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}