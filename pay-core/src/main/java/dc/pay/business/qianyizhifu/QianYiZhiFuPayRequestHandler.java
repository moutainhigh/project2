package dc.pay.business.qianyizhifu;

import java.util.ArrayList;
import java.util.HashMap;
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
 * @author sunny
 * 05 20, 2019
 */
@RequestPayHandler("QIANYIZHIFU")
public final class QianYiZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(QianYiZhiFuPayRequestHandler.class);

//    参数名称					参数含义				参数说明				必填
//    userid						商户编号				商户在支付平台系统的唯一身份标识。	是
//    innerorderid				商户订单号			商户系统提交的唯一订单号。	是
//    money						订单金额				单位 元				是
//    type							支付类型				固定值 			102是
//    notifyurl						后台通知				用于支付成功后的后台通知	是
//    sign							签名数据				参见签名机制	是

  private static final String userid               		="userid";
  private static final String innerorderid           ="innerorderid";
  private static final String money           			="money";
  private static final String type           				="type";
  private static final String notifyurl         			="notifyurl";
  
  private static final String signType            ="signType";
  private static final String sign                ="sign";
  private static final String key                 ="key";
  
  @Override
  protected Map<String, String> buildPayParam() throws PayException {
      Map<String, String> payParam = new TreeMap<String, String>() {
          {
              put(userid, channelWrapper.getAPI_MEMBERID());
              put(innerorderid,channelWrapper.getAPI_ORDER_ID());
              put(money,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
              put(notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
              put(type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
          }
      };
      log.debug("[仟亿支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
      return payParam;
  }

   protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
      List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
      StringBuilder signSrc = new StringBuilder();
      for (int i = 0; i < paramKeys.size(); i++) {
//          if (!pay_productname.equals(paramKeys.get(i)) && !pay_attach.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
          if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
          	signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
          }
      }
      //最后一个&转换成#
      //signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "#" );
      signSrc.append(key+"="+channelWrapper.getAPI_KEY());
      String paramsStr = signSrc.toString();
      String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr);
      log.debug("[仟亿支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
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
      	String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,null);
	        if (StringUtils.isBlank(resultStr)) {
	            log.error("[仟亿支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(EMPTYRESPONSE);
	        }
	        resultStr = UnicodeUtil.unicodeToString(resultStr);
	        resultStr=resultStr.replaceAll("\\\\", "");
	        if (!resultStr.contains("{") || !resultStr.contains("}")) {
	            log.error("[仟亿支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        JSONObject resJson;
	        try {
	            resJson = JSONObject.parseObject(resultStr);
	        } catch (Exception e) {
	            e.printStackTrace();
	            log.error("[仟亿支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        if (null != resJson && resJson.containsKey("code") && resJson.getString("code").equals("success")) {
	        	String type=resJson.getString("type");
	        	if("1".equals(type)){
	        		String code_url = resJson.getString("data");
		            result.put(QRCONTEXT, code_url);
	        	}else if("2".equals(type)) {
	        		String code_url = resJson.getString("data");
		            result.put(HTMLCONTEXT, code_url);
	        	}else if("3".equals(type)) {
	        		String code_url = resJson.getString("data");
		            result.put(JUMPURL, code_url);
	        	}else {
	        		log.error("[仟亿支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
		            throw new PayException(resultStr);
	        	}
	           
	        }else {
	            log.error("[仟亿支付]-[请求支付]-3.5.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        payResultList.add(result);
      }
      log.debug("[仟亿支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
      log.debug("[仟亿支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
      return requestPayResult;
  }
}