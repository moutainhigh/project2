package dc.pay.business.shenghuizhifu2;

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
 * June 28, 2019
 */
@RequestPayHandler("SHENGHUIZHIFU2")
public final class ShengHuiZhiFu2PayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ShengHuiZhiFu2PayRequestHandler.class);

    private static final String pay_memberid    = "pay_memberid";   // String  商户号    N   Y    商户号由微云分配
    private static final String pay_orderid     = "pay_orderid";    // String  订单号    Y   Y    订单ID，须唯一，如果商户不提供，则有微云分配订单ID
    private static final String pay_amount      = "pay_amount";     // String  金额     N   Y    付款金额，单位为元
    private static final String pay_applydate   = "pay_applydate";  // String  订单时间  N   Y    订单生成日期
    private static final String pay_bankcode    = "pay_bankcode";   // String  银行编号  N   Y    银行编码，见附录
    private static final String pay_notifyurl   = "pay_notifyurl";  // String  通知地址  N   Y    订单支付成功后，微云会将支付结果通知到该地址，具体通知信息见下文
    private static final String pay_callbackurl = "pay_callbackurl";// String  返回地址  Y   Y    页面跳转返回地址，支付成功后，微云会跳转到商户提供的该地址处。
    private static final String pay_tongdao     = "pay_tongdao";    // String  通道名    N   N    支付通道名，见附录。商户须严格按照以上通道名称传递该参数
    private static final String pay_productname = "pay_productname";// String  商品名称   N   N    商品描述名称
//  private static final String pay_md5sign      ="pay_md5sign";     // String  签名      N   N    参数签名，具体签名算法见下文

    private static final String key = "key";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(pay_memberid, channelWrapper.getAPI_MEMBERID());
                put(pay_orderid, channelWrapper.getAPI_ORDER_ID());
                put(pay_amount, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(pay_applydate, DateUtil.formatDateTimeStrByParam("yyyy-MM-dd HH:mm:ss"));
                put(pay_bankcode, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
                put(pay_notifyurl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(pay_callbackurl, channelWrapper.getAPI_WEB_URL());
                put(pay_tongdao, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
                put(pay_productname, channelWrapper.getAPI_ORDER_ID());
            }
        };
        log.debug("[新胜辉支付]-[请求支付]-1.组装请求参数完成：{}", JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
        List          paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc   = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!pay_productname.equals(paramKeys.get(i)) && !pay_tongdao.equals(paramKeys.get(i))
                    && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
//      删除最后一个字符
//      signSrc.deleteCharAt(paramsStr.length()-1);
        signSrc.append(key + "=" + channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5   = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[新胜辉支付]-[请求支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);

        HashMap<String, String> result = Maps.newHashMap();
        try {

            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam).toString());

        } catch (Exception e) {
            log.error("[新胜辉支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null", e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[新胜辉支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));
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
        log.debug("[新胜辉支付]-[请求支付]-4.处理请求响应成功：{}", JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}