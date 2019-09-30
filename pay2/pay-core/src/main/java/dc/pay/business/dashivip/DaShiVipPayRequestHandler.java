package dc.pay.business.dashivip;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
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
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Jun 4, 2019
 */
@RequestPayHandler("DASHIVIP")
public final class DaShiVipPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(DaShiVipPayRequestHandler.class);

    //参数名称    参数含义    是否必填    参与签名    参数说明
    //pay_memberid    商户号 是   是   平台分配商户号
    //pay_orderid     订单号 是   是   上送订单号唯一, 字符长度20
    //pay_applydate 提交时间    是   是   时间戳,精确到秒(中国时区)
    //pay_bankcode    银行编码    是   是   参考后续说明
    //pay_notifyurl   服务端通知   是   是   服务端返回地址.（POST返回数据）
    //pay_callbackurl 页面跳转通知  是   是   页面跳转返回地址（POST返回数据）
    //pay_amount      订单金额    是   是   商品金额
    //pay_md5sign     MD5签名   是   否   请看MD5签名字段格式
    //pay_productname   商品名称    是   否   
    private static final String pay_memberid                ="pay_memberid";
    private static final String pay_orderid                 ="pay_orderid";
    private static final String pay_applydate               ="pay_applydate";
    private static final String pay_bankcode                ="pay_bankcode";
    private static final String pay_notifyurl               ="pay_notifyurl";
    private static final String pay_callbackurl             ="pay_callbackurl";
    private static final String pay_amount                  ="pay_amount";
    private static final String pay_productname                  ="pay_productname";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="pay_md5sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(pay_memberid, channelWrapper.getAPI_MEMBERID());
                put(pay_orderid,channelWrapper.getAPI_ORDER_ID());
//                put(pay_applydate, StringToTimestamp(DateUtil.formatDateTimeStrByParam("yyyy-MM-dd HH:mm:ss")).toString());
                put(pay_applydate, DateUtil.formatDateTimeStrByParam("yyyy-MM-dd HH:mm:ss"));
                put(pay_bankcode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
                put(pay_notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(pay_callbackurl,channelWrapper.getAPI_WEB_URL());
                put(pay_amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(pay_productname,  "name");
            }
        };
        log.debug("[大师VIP]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!pay_productname.equals(paramKeys.get(i))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");                
            }
        }
        //最后一个&转换成#
        //signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "#" );
        signSrc.append(key +"="+ channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[大师VIP]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
//        if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
        if (true) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
//            result.put(HTMLCONTEXT, getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
        }
        
//        else{
//            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
////        String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
////        String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
//            if (StringUtils.isBlank(resultStr)) {
//                log.error("[大师VIP]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }
//            if (!resultStr.contains("{") || !resultStr.contains("}")) {
//                log.error("[大师VIP]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }
//            JSONObject jsonObject = null;
//            try {
//                jsonObject = JSONObject.parseObject(resultStr);
//            } catch (Exception e) {
//                log.error("[大师VIP]-[请求支付]-3.3.发送支付请求，及获取支付请求出错，订单号：" + channelWrapper.getAPI_ORDER_ID() + ",通道名称：" + channelWrapper.getAPI_CHANNEL_BANK_NAME() + ",postUrl:" + channelWrapper.getAPI_CHANNEL_BANK_URL() + ",payForManagerCGIResultJsonObj" + e.getMessage(), e);
//                //log.error("[盛盈付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + resultStr + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(e.getMessage(),e);
//            }          
//            //只取正确的值，其他情况抛出异常
//            if (null != jsonObject && jsonObject.containsKey("status") && "success".equalsIgnoreCase(jsonObject.getString("status"))  && jsonObject.containsKey("data") && StringUtils.isNotBlank(jsonObject.getString("data"))) {
//                JSONObject jsonObject2 = jsonObject.getJSONObject("data");
//                if (null != jsonObject2 && jsonObject2.containsKey("url") && StringUtils.isNotBlank(jsonObject2.getString("url"))) {
//                    if (handlerUtil.isWapOrApp(channelWrapper) || handlerUtil.isWebWyKjzf(channelWrapper)) {
//                        result.put(JUMPURL, jsonObject2.getString("url"));
//                    }else {
//                        String qr = QRCodeUtil.decodeByUrl(jsonObject2.getString("url"));
//                        if (StringUtils.isBlank(qr)) {
//                            log.error("[大师VIP]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                            throw new PayException(resultStr);
//                        }
//                        result.put(QRCONTEXT, qr);
//                    }
//                }else {
//                    log.error("[大师VIP]-[请求支付]-3.5.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                    throw new PayException(resultStr);
//                }
//            }else {
//                log.error("[大师VIP]-[请求支付]-3.6.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }
//        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[大师VIP]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[大师VIP]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
    
//    public static void main(String[] args) {  
//        
//        Date date = new Date(System.currentTimeMillis());  // 对应的北京时间是2017-08-24 11:17:10
//         
//        SimpleDateFormat bjSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");     // 北京
//        bjSdf.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));  // 设置北京时区
//         
//        SimpleDateFormat tokyoSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");  // 东京
//        tokyoSdf.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));  // 设置东京时区
//         
//        SimpleDateFormat londonSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // 伦敦
//        londonSdf.setTimeZone(TimeZone.getTimeZone("Europe/London"));  // 设置伦敦时区
//         
//        System.out.println("毫秒数:" + date.getTime() + ", 北京时间:" + bjSdf.format(date));
//        System.out.println("毫秒数:" + date.getTime() + ", 东京时间:" + tokyoSdf.format(date));
//        
//        String timeStr =  DateUtil.formatDateTimeStrByParam("yyyy-MM-dd HH:mm:ss"); // 字面时间
//        SimpleDateFormat bjSdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        bjSdf2.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
//        Date bjDate = null;
//        try {
//            bjDate = bjSdf2.parse(timeStr);
//        } catch (ParseException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }  // 解析
//        System.out.println("字面时间: " + timeStr +",按北京时间来解释:" + bjSdf2.format(bjDate) + ", " + bjDate.getTime());
//         
//        SimpleDateFormat tokyoSdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");  // 东京
//        tokyoSdf2.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));  // 设置东京时区
//        Date tokyoDate = null;
//        try {
//            tokyoDate = tokyoSdf2.parse(timeStr);
//        } catch (ParseException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        } // 解析
//        System.out.println("字面时间: " + timeStr +",按东京时间来解释:"  + tokyoSdf2.format(tokyoDate) + ", " + tokyoDate.getTime());
//        System.out.println(tokyoDate.getTime());
//        System.out.println(bjDate.getSeconds());
//
//    }  
    
    /**
     * String(yyyy-MM-dd HH:mm:ss)转10位时间戳
     * @param time
     * @return
     */
    public static Integer StringToTimestamp(String time){
        int times = 0;
        try {  
            times = (int) ((Timestamp.valueOf(time).getTime())/1000);  
        } catch (Exception e) {  
            e.printStackTrace();  
        }
        if(times==0){
            System.out.println("String转10位时间戳失败");
        }
        return times; 
        
    }
    
    public static void main(String[] args) {    
        Date date = new Date(1391174450000L); // 2014-1-31 21:20:50    
        System.out.println(date);    
        Calendar calendar = Calendar.getInstance();    
        calendar.setTimeZone(TimeZone.getTimeZone("GMT"));    
        // 或者可以 Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));    
        calendar.setTime(date);    
        System.out.println(calendar.get(Calendar.HOUR_OF_DAY) + ":" + calendar.get(Calendar.MINUTE));    
        
        System.out.println(StringToTimestamp(DateUtil.formatDateTimeStrByParam("yyyy-MM-dd HH:mm:ss")).toString());    
    }    
}