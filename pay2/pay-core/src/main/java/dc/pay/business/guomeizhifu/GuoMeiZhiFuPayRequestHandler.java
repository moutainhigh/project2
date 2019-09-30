package dc.pay.business.guomeizhifu;

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
 * 05 01, 2019
 */
@RequestPayHandler("GUOMEIZHIFU")
public final class GuoMeiZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(GuoMeiZhiFuPayRequestHandler.class);

//    序号	参数名		参数名称			类型			是否必填			说明
//    1.	mer_no		商户号			String		是				商户编号
//    2.	mer_order_no商户订单号		String		是				商户必须保证唯一
//    3.	order_amount交易金额			Number		是				分为单位，整数
//    4.	busi_code	业务编码			String		是				详情见：附件业务编码
//    5.	goods		商品详情			String		是				商品详情
//    6.	notifyUrl	异步通知地址		String		是				支付成功后，平台主动通知商家系统，商家系统必须指定接收通知的地址。
//    7.	bankCode	银行编码 			String						一定场景，必填	网关业务必填
//    8.	pageUrl	支付成功，页面跳转地址	String						一定场景，必填	H5业务必填，
//    9.	sign		数字签名			String		是				详情见：数字签名

  private static final String mer_no                 ="mer_no";
  private static final String mer_order_no           ="mer_order_no";
  private static final String order_amount           ="order_amount";
  private static final String busi_code           	 ="busi_code";
  private static final String goods          		 ="goods";
  private static final String bankCode               ="bankCode";
  private static final String pageUrl            	 ="pageUrl";
  private static final String notifyUrl            	 ="notifyUrl";
  
  private static final String sign                ="sign";
  private static final String key                 ="key";

  @Override
  protected Map<String, String> buildPayParam() throws PayException {
      Map<String, String> payParam = new TreeMap<String, String>() {
          {
              put(mer_no, channelWrapper.getAPI_MEMBERID());
              put(mer_order_no,channelWrapper.getAPI_ORDER_ID());
              put(order_amount,channelWrapper.getAPI_AMOUNT());
              put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
              put(busi_code,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
              put(pageUrl,channelWrapper.getAPI_WEB_URL());
              put(goods,channelWrapper.getAPI_ORDER_ID());
              if(HandlerUtil.isWY(channelWrapper)){
            	  put(bankCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
              }
          }
      };
      log.debug("[国美支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
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
      log.debug("[国美支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
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
	            log.error("[国美支付]]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(EMPTYRESPONSE);
	        }
	        if (!resultStr.contains("{") || !resultStr.contains("}")) {
	            log.error("[国美支付]]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        JSONObject resJson;
	        try {
	            resJson = JSONObject.parseObject(resultStr);
	        } catch (Exception e) {
	            e.printStackTrace();
	            log.error("[国美支付]]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        if (null != resJson && resJson.containsKey("status") && resJson.getString("status").equals("SUCCESS")) {
	        	if(HandlerUtil.isJDSM(channelWrapper)||HandlerUtil.isYLSM(channelWrapper)){
	        		String code_url = resJson.getString("order_data");
		            result.put(HTMLCONTEXT, code_url);
	        	}else{
	        		 String code_url = resJson.getString("order_data");
	 	            result.put(JUMPURL, code_url);
	        	}
	        }else {
	            log.error("[国美支付]]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        payResultList.add(result);
      }
      log.debug("[国美支付]]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
      log.debug("[国美支付]]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
      return requestPayResult;
  }
}