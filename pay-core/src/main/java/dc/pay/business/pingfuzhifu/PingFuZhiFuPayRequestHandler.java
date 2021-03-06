package dc.pay.business.pingfuzhifu;

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
 * 05 06, 2019
 */
@RequestPayHandler("PINGFUZHIFU")
public final class PingFuZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(PingFuZhiFuPayRequestHandler.class);

//    参数			说明			示例			类型		是否为空
//    apiname		API函数名	query_pay	文字		N
//    mchid			接入ID		1			文字		N
//    version		版本号		2.05		数字		N
//    data			函数输入参数	JSON		Y
//    参数			说明			示例			类型		是否为空
//    type			支付类型		0支付宝 1微信	数字		N
//    money			支付金额		只能是100的倍数	数字	N
//    agent_id		商户ID		数字					N
//    remark		备注			文字					Y
//    push_url		接收推送回调url地址		文字		Y

  private static final String apiname               ="apiname";
  private static final String mchid           		="mchid";
  private static final String version           	="version";
  private static final String data           		="data";
  
  private static final String type          		="type";
  private static final String money              	="money";
  private static final String agent_id            	="agent_id";
  private static final String remark           		="remark";
  private static final String push_url           	="push_url";
  private static final String out           		="out";
  private static final String H5get           		="H5get";
  
  private static final String id            		="id";
  private static final String sign                ="sign";
  private static final String key                 ="key";
  
  @Override
  protected Map<String, String> buildPayParam() throws PayException {
      Map<String, String> payParam = new TreeMap<String, String>() {
          {
              put(id, channelWrapper.getAPI_MEMBERID());
              put(remark,channelWrapper.getAPI_ORDER_ID());
              put(money,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
              put(push_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
              put(type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              put(out,"1");
              if(HandlerUtil.isWapOrApp(channelWrapper)){
            	  put(H5get,"1");
              }
              //put(signType,"MD5");
          }
      };
      log.debug("[拼付支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
      return payParam;
  }

   protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
      String signSrc=String.format("%s%s%s", 
    		  api_response_params.get(money),
    		  api_response_params.get(remark),
    		  channelWrapper.getAPI_KEY()
      );
      String paramsStr = signSrc.toString();
      String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
      log.debug("[拼付支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
      return signMD5;
  }

  protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
      payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
      HashMap<String, String> result = Maps.newHashMap();
      ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
      String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.GET);
      if (StringUtils.isBlank(resultStr)) {
          log.error("[拼付支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
          throw new PayException(EMPTYRESPONSE);
      }
      resultStr = UnicodeUtil.unicodeToString(resultStr);
      resultStr=resultStr.replaceAll("\\\\", "");
      if (!resultStr.contains("{") || !resultStr.contains("}")) {
          log.error("[拼付支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
          throw new PayException(resultStr);
      }
      JSONObject resJson;
      try {
          resJson = JSONObject.parseObject(resultStr);
      } catch (Exception e) {
          e.printStackTrace();
          log.error("[拼付支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
          throw new PayException(resultStr);
      }
      if (null != resJson && resJson.containsKey("code") && resJson.getString("code").equals("1")) {
    	  if(HandlerUtil.isWapOrApp(channelWrapper)){
    		  String code_url = resJson.getString("H5url");
              result.put(JUMPURL, code_url);
    	  }else{
    		  String code_url = resJson.getString("qrurl");
              result.put(QRCONTEXT, code_url);
    	  }
      }else {
          log.error("[拼付支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
          throw new PayException(resultStr);
      }
      payResultList.add(result);
      log.debug("[拼付支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
      log.debug("[拼付支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
      return requestPayResult;
  }
}