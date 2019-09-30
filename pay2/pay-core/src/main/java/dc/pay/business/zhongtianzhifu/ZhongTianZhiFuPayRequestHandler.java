package dc.pay.business.zhongtianzhifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

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
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Dec 5, 2017
 */
@RequestPayHandler("ZHONGTIANZHIFU")
public final class ZhongTianZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ZhongTianZhiFuPayRequestHandler.class);

    //发起付款
    //跳转支付页接口URL：http://120.79.243.161/pay
    //传参方式： Post
    //使用方法： 用表单post的方式，post参数并跳转到此网址，显示我们的支付页
    //#   参数名 含义  类型  说明  参与加密    必填
    //1.  merchant    商户号 string(50)  您的商户唯一标识，注册后在基本资料里获得        
    private static final String merchant                ="merchant";
    //2.  amount  金额  float   单位：元。精确小数点后2位       
    private static final String amount                ="amount";
    //3.  pay_code    支付渠道    String  填写相应的支付方式编码 alipay：支付宝；wechat：微信支付      
    private static final String pay_code                ="pay_code";
    //4.  order_no    商户订单号   string(50)  订单号，max(50),该值需在商户系统内唯一     
    private static final String order_no                ="order_no";
    //5.  notify_url  异步回调地址  string(255) 异步通知地址，需要以http://开头且没有任何参数用户。支付成功后，我们服务器会主动发送一个post消息到这个网址。     
    private static final String notify_url                ="notify_url";
    //6.  return_url  同步跳转地址  string(255) 同步跳转地址，支付成功后跳回用户。支付成功后，我们会让用户浏览器自动跳转到这个网址。      
    private static final String return_url                ="return_url";
    //7.  json    请求返回方式  string(10)  固定值：json; 注意：只适用于扫码付款       
    private static final String json                ="json";
    //8.  attach  备注消息    string(1000)    回调时将会根据传入内容原样返回（为防止乱码情况，请尽量不填写中文）   有值时加入   
//    private static final String attach                ="attach";
    //9.  order_time  请求时间    string(50)  格式YYYY-MM-DD hh:ii:ss，回调时原样返回       
    private static final String order_time                ="order_time";
    //10. cuid    商户的用户id string(50)  商户名下的能表示用户的标识，方便对账，回调时原样返回  有值时加入   
//    private static final String cuid                ="cuid";
    //11. sign    MD5签名   string(32)  Md5签名(签名规则详见下面签名规则)     
//    private static final String sign                ="sign";
    
    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 2) {
            log.error("[中天支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&通道类型编码值（向第三方获取当前使用通道编码值）" );
            throw new PayException("[中天支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&通道类型编码值（向第三方获取当前使用通道编码值）" );
        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merchant, channelWrapper.getAPI_MEMBERID().split("&")[0]);
                put(amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
//                put(pay_code,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(pay_code,channelWrapper.getAPI_MEMBERID().split("&")[1]);
                put(order_no,channelWrapper.getAPI_ORDER_ID());
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(return_url,channelWrapper.getAPI_WEB_URL());
                if (handlerUtil.isWEBWAPAPP_SM(channelWrapper)) {                    
                    put(json,"json");
                }
                put(order_time,DateUtil.formatDateTimeStrByParam("yyyy-MM-dd HH:mm:ss"));
            }
        };
        log.debug("[中天支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!json.equals(paramKeys.get(i))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //最后一个&转换成#
        //signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "#" );
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        signSrc.append(key + "="+channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[中天支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();
        
//      if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
      if (true) {
          result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
          //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
      }else{
          String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParam),MediaType.APPLICATION_JSON_VALUE).trim();
//          String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
          //String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
          //if (StringUtils.isBlank(resultStr)) {
          //    log.error("[中天支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
          //    throw new PayException(resultStr);
          //    //log.error("[中天支付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
          //    //throw new PayException("返回空,参数："+JSON.toJSONString(map));
          //}
//          System.out.println("请求返回=========>"+resultStr);
          //if (!resultStr.contains("{") || !resultStr.contains("}")) {
          //   log.error("[中天支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
          //   throw new PayException(resultStr);
          //}
          //JSONObject jsonObject = JSONObject.parseObject(resultStr);
          JSONObject jsonObject;
          try {
              jsonObject = JSONObject.parseObject(resultStr);
          } catch (Exception e) {
              e.printStackTrace();
              log.error("[中天支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              throw new PayException(resultStr);
          }
          //只取正确的值，其他情况抛出异常
          //if (null != jsonObject && jsonObject.containsKey("result_code") && "SUCCESS".equalsIgnoreCase(jsonObject.getString("result_code"))  && 
          //(jsonObject.containsKey("qrcode") && StringUtils.isNotBlank(jsonObject.getString("qrcode")) || 
          // jsonObject.containsKey("pay_url") && StringUtils.isNotBlank(jsonObject.getString("pay_url")))
          //){
          if (null != jsonObject && jsonObject.containsKey("status") && "1".equalsIgnoreCase(jsonObject.getString("status"))  && jsonObject.containsKey("codeimg") && StringUtils.isNotBlank(jsonObject.getString("codeimg"))) {
              String code_url = jsonObject.getString("codeimg");
              result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
              //if (handlerUtil.isWapOrApp(channelWrapper)) {
              //    result.put(JUMPURL, code_url);
              //}else{
              //    result.put(QRCONTEXT, code_url);
              //}
          }else {
              log.error("[中天支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              throw new PayException(resultStr);
          }
      }
      
  
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[中天支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[中天支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}