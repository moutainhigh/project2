package dc.pay.business.xinbazhifu;

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
 * @date 10 Sep 2019
 */
@RequestPayHandler("XINBAZHIFU")
public final class XinBaZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XinBaZhiFuPayRequestHandler.class);

//    参数名				参数					长度定义				参数说明
//    交易流水号			orderId				VARCHAR(32)			交易流水号
//    交易金额			totalAmount			Decimal(8,2)		交易金额，单位：元，最小2元
//    支付通道			payType				VARCHAR(16)			支付通道，
//    异步通知地址		notifyUrl			VARCHAR(64)			异步通知返回地址
//    扩展字段			extendField			VARCHAR(32)			异步通知时回传
//    APP ID			appId				VARCHAR(8)			APP ID（应用ID）
//    签名				sign				VARCHAR(32)			MD5签名（大写）

  private static final String orderId               ="orderId";
  private static final String totalAmount           ="totalAmount";
  private static final String payType           	="payType";
  private static final String notifyUrl           	="notifyUrl";
  private static final String extendField           ="extendField";
  private static final String appId              	="appId";
  
  private static final String signType            ="signType";
  private static final String sign                ="sign";
  private static final String key                 ="key";
  
  @Override
  protected Map<String, String> buildPayParam() throws PayException {
      Map<String, String> payParam = new TreeMap<String, String>() {
          {
              put(appId, channelWrapper.getAPI_MEMBERID());
              put(orderId,channelWrapper.getAPI_ORDER_ID());
              put(totalAmount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
              put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
              put(payType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              put(extendField,channelWrapper.getAPI_ORDER_ID());
          }
      };
      log.debug("[辛巴支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
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
      log.debug("[辛巴支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
      return signMD5;
  }

  protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
      payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
      HashMap<String, String> result = Maps.newHashMap();
      ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
      result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());  //.replace("method='post'","method='get'"));
      payResultList.add(result);
      log.debug("[辛巴支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
      log.debug("[辛巴支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
      return requestPayResult;
  }
}