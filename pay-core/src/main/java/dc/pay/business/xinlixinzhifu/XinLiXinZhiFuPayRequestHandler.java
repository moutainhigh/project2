package dc.pay.business.xinlixinzhifu;

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
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Cobby
 * July 27, 2019
 */
@RequestPayHandler("XINLIXINZHIFU")
public final class XinLiXinZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XinLiXinZhiFuPayRequestHandler.class);

    private static final String merchant   = "merchant";  //    N    Y    下发的商户号
    private static final String amount     = "amount";    //    N    Y    单位元（人民币），2位小数
    private static final String pay_code   = "pay_code";  //    N    Y    填写相应的支付方式编码
    private static final String order_no   = "order_no";  //    N    Y    订单号，max(50),该值需在商户系统内唯一
    private static final String notify_url = "notify_url";//    N    Y    异步通知地址，需要以http://开头且没有任何参数
    private static final String return_url = "return_url";//    N    Y    同步跳转地址，支付成功后跳回
    private static final String order_time = "order_time";//    Y    Y    格式YYYY-MM-DD hh:ii:ss，回调时原样返回
//  private static final String json                   ="json";      //    Y    N    固定值：json; 注意：只适用于扫码付款
//  private static final String attach                 ="attach";    //    Y    有值加入    回调时原样返回
//  private static final String cuid                   ="cuid";      //    Y    有值加入    商户名下的能表示用户的标识，方便对账，回调时原样返回

    private static final String key = "key";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merchant, channelWrapper.getAPI_MEMBERID());
                put(amount, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(pay_code, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(order_no, channelWrapper.getAPI_ORDER_ID());
                put(notify_url, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(return_url, channelWrapper.getAPI_WEB_URL());
                put(order_time, DateUtil.formatDateTimeStrByParam("yyyy-MM-dd HH:mm:ss"));
            }
        };
        log.debug("[新立信支付]-[请求支付]-1.组装请求参数完成：{}", JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
        List          paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc   = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        signSrc.append(key + "=" + channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5   = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[新立信支付]-[请求支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);

        HashMap<String, String> result = Maps.newHashMap();
        try {

            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam).toString());

        } catch (Exception e) {
            log.error("[新立信支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null", e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[新立信支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));
        return payResultList;
    }

    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (null != resultListMap && !resultListMap.isEmpty()) {
            if (resultListMap.size() == 1) {
                Map<String, String> resultMap = resultListMap.get(0);
                requestPayResult = buildResult(resultMap, channelWrapper, requestPayResult);
            }
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[新立信支付]-[请求支付]-4.处理请求响应成功：{}", JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}