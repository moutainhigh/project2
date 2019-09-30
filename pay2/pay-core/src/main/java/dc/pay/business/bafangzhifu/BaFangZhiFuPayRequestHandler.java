package dc.pay.business.bafangzhifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
import dc.pay.utils.ValidateUtil;


/**
 * @author sunny
 * @date 3 Aug 2019
 */
@RequestPayHandler("BAFANGZHIFU")
public final class BaFangZhiFuPayRequestHandler extends PayRequestHandler {
  private static final Logger log = LoggerFactory.getLogger(BaFangZhiFuPayRequestHandler.class);


  private static final String mid               	="mid";
  private static final String order           		="order";
  private static final String amount           		="amount";
  private static final String ver           		="ver";
  private static final String type          		="type";
  private static final String time              	="time";
  private static final String notify            	="notify";
  
  private static final String signType            ="signType";
  private static final String sign                ="sign";
  private static final String key                 ="key";
  


  @Override
  protected Map<String, String> buildPayParam() throws PayException {
      Map<String, String> payParam = new TreeMap<String, String>() {
          {
              put(mid, channelWrapper.getAPI_MEMBERID());
              put(order,channelWrapper.getAPI_ORDER_ID());
              put(amount,channelWrapper.getAPI_AMOUNT());
              put(notify,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
              put(type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              put(time,System.currentTimeMillis()+"");
              put(ver,"2");
          }
      };
      log.debug("[8方支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
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
      //最后一个&转换成#
      signSrc.append(key+"="+channelWrapper.getAPI_KEY());
      String paramsStr = signSrc.toString();
      String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr);
      log.debug("[8方支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
      return signMD5;
  }

  protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
      payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
      HashMap<String, String> result = Maps.newHashMap();
      String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,null);
          if (StringUtils.isBlank(resultStr)) {
              log.error("[8方支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              throw new PayException(EMPTYRESPONSE);
          }
          if (!resultStr.contains("{") || !resultStr.contains("}")) {
              log.error("[8方支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              throw new PayException(resultStr);
          }
          JSONObject resJson;
          try {
              resJson = JSONObject.parseObject(resultStr);
          } catch (Exception e) {
              e.printStackTrace();
              log.error("[8方支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              throw new PayException(resultStr);
          }
          if (null != resJson && resJson.containsKey("msg") && "ok".equals(resJson.getString("msg")) && StringUtils.isNotBlank(resJson.getString("qrcode"))) {
              String code_url = resJson.getString("qrcode");
              result.put(JUMPURL, code_url);
          }else {
              log.error("[8方支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              throw new PayException(resultStr);
          }
          ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
          payResultList.add(result);
    
      log.debug("[8方支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
      log.debug("[8方支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
      return requestPayResult;
  }
}