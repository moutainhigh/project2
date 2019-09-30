package dc.pay.business.feigezhifu3;

/**
 * ************************
 * @author tony 3556239829
 */

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
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RequestPayHandler("FEIGEZHIFU3")
public final class FeiGeZhiFu3PayRequestHandler extends PayRequestHandler {
    //private static final Logger log = LoggerFactory.getLogger(GeFuPayRequestHandler.class);


    private static final String     user = "user";  //	string(11)	Y	在本平台注册时使用的手机号
    private static final String     record = "record";  //	String(50)	Y	商户订单号或其他附加信息，如：用户网站的用户名，订单号，手机号，等唯一标识(只能包含数字/字母/-/_)
    private static final String     money = "money";  //	decimal(8,2)	Y	金额。请将金额强制转换为2位小数再进行提交
    private static final String     paytype = "paytype";  //	int(1)	Y	支付类型，支付宝:1/微信:2/QQ:3
    private static final String     notify_url = "notify_url";  //	String(150)	Y	异步通知地址,又称回调地址
    private static final String     sign = "sign";  //	string(32)	Y	数据签名 将money record key 拼接后md5


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(user,channelWrapper.getAPI_MEMBERID());
            payParam.put(record,channelWrapper.getAPI_ORDER_ID());
            payParam.put(money,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(paytype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
        }
        log.debug("[飞鸽支付3]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }




    protected String buildPaySign(Map<String,String> params) throws PayException {
        // money record key
        String paramsStr = String.format("%s%s%s",
                params.get(money),
                params.get(record),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[飞鸽支付3]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (1==2 && HandlerUtil.isWY(channelWrapper) &&  HandlerUtil.isYLKJ(channelWrapper)  &&  HandlerUtil.isWapOrApp(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='post'"));
                payResultList.add(result);
            }else{
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
				
				if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){
                    result.put(HTMLCONTEXT,resultStr);
                    payResultList.add(result);
                }else if(StringUtils.isNotBlank(resultStr) ){
                    JSONObject jsonResultStr = JSON.parseObject(resultStr);
                    if(null!=jsonResultStr && jsonResultStr.containsKey("code") && "200".equalsIgnoreCase(jsonResultStr.getString("code"))){
                        if(HandlerUtil.isWapOrApp(channelWrapper) && jsonResultStr.containsKey("payurl") && StringUtils.isNotBlank(jsonResultStr.getString("payurl"))  ){
                            result.put(JUMPURL, jsonResultStr.getString("payurl"));
                        }else if(jsonResultStr.containsKey("image") && StringUtils.isNotBlank(jsonResultStr.getString("image")) ){
                            result.put(QRCONTEXT, jsonResultStr.getString("image"));
                        }else{throw new PayException(resultStr);}
                        payResultList.add(result);
                    }else {throw new PayException(resultStr); }
				}else{ throw new PayException(EMPTYRESPONSE);}
                 
            }
        } catch (Exception e) { 
             log.error("[飞鸽支付3]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[飞鸽支付3]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
        return payResultList;
    }


    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (null != resultListMap && !resultListMap.isEmpty()) {
            if (resultListMap.size() == 1) {
                Map<String, String> resultMap = resultListMap.get(0);
                requestPayResult = buildResult(resultMap,channelWrapper,requestPayResult);
            }
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[飞鸽支付3]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}