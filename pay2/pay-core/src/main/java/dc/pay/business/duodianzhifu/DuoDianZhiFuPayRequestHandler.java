package dc.pay.business.duodianzhifu;

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
 * @date 10 Jul 2019
 */
@RequestPayHandler("DUODIANZHIFU")
public final class DuoDianZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(DuoDianZhiFuPayRequestHandler.class);

//  参数			类型			可空			说明
//  pid			String		N			商户ID
//  amount		float		N			金额,单位元，范围（1~2000）
//  trade_no		String		N			订单号
//  order_source	String		N			订单来源,推荐设置为区别用户的标识，比如设置为用户的ID，方便在商户在后台按用户筛选订单
//  pay_type		String		N			支付方式,可用值: alih5 #支付宝H5 wxh5 #微信H5
//  return_url	String		N			服务器同步返回地址
//  notify_url	String		N				服务器异步通知地址
//  sign			String		N			32位小写MD5签名值，参数字典序排列 md5(amount={}&notify_url={}&order_source={}&pay_type={}&pid={}

    private static final String pid                  	  ="pid";
    private static final String amount                    ="amount";
    private static final String trade_no                  ="trade_no";
    private static final String order_source              ="order_source";
    private static final String pay_type             	  ="pay_type";
    private static final String return_url                ="return_url";
    private static final String notify_url                ="notify_url";
    private static final String apikey                	  ="apikey";
  
    private static final String signType            ="signType";
    private static final String sign                ="sign";
    private static final String key                 ="key";

  @Override
  protected Map<String, String> buildPayParam() throws PayException {
      Map<String, String> payParam = new TreeMap<String, String>() {
          {
              put(pid, channelWrapper.getAPI_MEMBERID());
              put(trade_no,channelWrapper.getAPI_ORDER_ID());
              put(amount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
              put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
              put(pay_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              put(return_url,channelWrapper.getAPI_WEB_URL());
              put(order_source,channelWrapper.getAPI_ORDER_ID());
              //put(signType,"MD5");
          }
      };
      log.debug("[多点支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
      return payParam;
  }

   protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
      String signSrc=String.format("%s%s%s%s%s%s%s%s", 
    		  amount+"="+  api_response_params.get(amount)+"&",
    		  notify_url+"="+  api_response_params.get(notify_url)+"&",
    		  order_source+"="+  api_response_params.get(order_source)+"&",
    		  pay_type+"="+  api_response_params.get(pay_type)+"&",
    		  pid+"="+  api_response_params.get(pid)+"&",
    		  return_url+"="+  api_response_params.get(return_url)+"&",
    		  trade_no+"="+  api_response_params.get(trade_no)+"&",
    		  apikey+"="+  channelWrapper.getAPI_KEY()
      );
      String paramsStr = signSrc.toString();
      String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
      log.debug("[多点支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
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
	            log.error("[多点支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(EMPTYRESPONSE);
	        }
	        if (!resultStr.contains("{") || !resultStr.contains("}")) {
	            log.error("[多点支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        JSONObject resJson;
	        try {
	            resJson = JSONObject.parseObject(resultStr);
	        } catch (Exception e) {
	            e.printStackTrace();
	            log.error("[多点支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        if (null != resJson && !resJson.containsKey("code")) {
	            String code_url = resJson.getString("rurl");
	            result.put(JUMPURL, code_url);
	        }else {
	            log.error("[多点支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        payResultList.add(result);
      }
      log.debug("[多点支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
      log.debug("[多点支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
      return requestPayResult;
  }
}