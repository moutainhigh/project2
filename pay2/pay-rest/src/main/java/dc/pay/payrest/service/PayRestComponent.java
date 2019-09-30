package dc.pay.payrest.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import dc.pay.payrest.util.RestTemplateUtil;
import dc.pay.payrest.util.XmlUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * ************************
 *
 * @author tony 3556239829
 */
@Component
@Qualifier("PayRestComponent")
public class PayRestComponent {

    private static final Logger log =  LoggerFactory.getLogger(PayRestComponent.class);
    private static final String ResPayRemoteIp = "ResPayRemoteIp";
    private static final String http = "http";
    private static final String PathEnd = "/";

    /**
     * 直链PayServIP地址,低优先级
     */
    @Value("${payProps.forweb.payServUrl:}")
    private String PAY_SERV_URL;


    /**
     * 非直链PayServIp地址，使用Eureka实例名，高优先级
     */
    @Value("${payProps.forweb.payServEurekaInstances:}")
    private String PAY_SERV_EUREKA_INSTANCES;


    @Autowired
    @LoadBalanced
    @Qualifier("payForWebLoadBalanced")
    RestTemplate payLoadBalanced;


    @Autowired
    @Qualifier("payForWebRestTemplate")
    RestTemplate payRestTemplate;





    /**
     * 转发内容至支付网关
     */
    public String postForPayServ(String channelName,Map<String,String> mapParams,HttpServletRequest request,String RESPAY_SERV_PATH){
        if(null==mapParams ||mapParams.isEmpty()) mapParams= Maps.newHashMap();
        mapParams.put(ResPayRemoteIp, getIpAdrress(request));//添加第三方远程服务器ip
        String payServUrl = null;
        RestTemplate restTemplate = null;
        if(StringUtils.isNotBlank(PAY_SERV_EUREKA_INSTANCES) && PAY_SERV_EUREKA_INSTANCES.startsWith(http)){
            payServUrl = PAY_SERV_EUREKA_INSTANCES+RESPAY_SERV_PATH+channelName+PathEnd;
            restTemplate=payLoadBalanced;
        }else{
            payServUrl = PAY_SERV_URL+RESPAY_SERV_PATH+channelName+PathEnd;
            restTemplate=payRestTemplate;
        }
        return RestTemplateUtil.postToPayServ(restTemplate,payServUrl,mapParams);
    }





    /**
     * 处理request参数
     */
    public static Map<String, String> processRequestParam(HttpServletRequest request) {
        Enumeration enu=request.getParameterNames();
        Map<String,String> paramMap = new TreeMap<String,String>();
        while(enu.hasMoreElements()){
            String paraName=(String)enu.nextElement();
            paramMap.put(paraName,request.getParameter(paraName));
        }
        if(null!=paramMap && !paramMap.isEmpty())
            return paramMap;
        return null;
    }



    /**
     * JSON转Map
     */
    public static  Map<String,String>  jsonToMap(String str) {
        String sb = str.substring(2, str.length() - 2);
        String[] name = sb.split("\\\",\\\"");
        String[] nn = null;
        Map<String, String> map = new HashMap<>();
        for (String aName : name) {
            nn = aName.split("\\\":\\\"");
            map.put(nn[0], nn.length==2?nn[1]:null);
        }
        return map;
    }


    public static boolean valMap(Map<String,String> params){
        if(!CollectionUtils.isEmpty(params)){
            Iterator<Map.Entry<String,String>> entries = params.entrySet().iterator();
            while (entries.hasNext()) {
                Map.Entry<String,String>  entry = entries.next();
                if(!RequestPayJumpService.valInput(entry.getValue())   || !RequestPayJumpService.valInput(entry.getKey())  ) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }


    public static String getIpAdrress(HttpServletRequest request) {
        String Xip = request.getHeader("X-Real-IP");
        String XFor = request.getHeader("X-Forwarded-For");
        if(StringUtils.isNotEmpty(XFor) && !"unKnown".equalsIgnoreCase(XFor)){
            //多次反向代理后会有多个ip值，第一个ip才是真实ip
            int index = XFor.indexOf(",");
            if(index != -1){
                return XFor.substring(0,index);
            }else{
                return XFor;
            }
        }
        XFor = Xip;
        if(StringUtils.isNotEmpty(XFor) && !"unKnown".equalsIgnoreCase(XFor)){
            return XFor;
        }
        if (StringUtils.isBlank(XFor) || "unknown".equalsIgnoreCase(XFor)) {
            XFor = request.getHeader("Proxy-Client-IP");
        }
        if (StringUtils.isBlank(XFor) || "unknown".equalsIgnoreCase(XFor)) {
            XFor = request.getHeader("WL-Proxy-Client-IP");
        }
        if (StringUtils.isBlank(XFor) || "unknown".equalsIgnoreCase(XFor)) {
            XFor = request.getHeader("HTTP_CLIENT_IP");
        }
        if (StringUtils.isBlank(XFor) || "unknown".equalsIgnoreCase(XFor)) {
            XFor = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (StringUtils.isBlank(XFor) || "unknown".equalsIgnoreCase(XFor)) {
            XFor = request.getRemoteAddr();
        }
        // XFor.concat(":").concat(request.getRemotePort()+"");
        return XFor;
    }





    //目前只能处理,request.getInputStream内容是  {"A":"B"},不能处理request.getInputStream内容是  data= {"A":"B"}
    public Map<String, String> getMapUseJsonStr(HttpServletRequest request) {
        Map<String, String> mapParams= new HashMap<>();
        String requstStr = XmlUtil.parseRequst(request);
        if(StringUtils.isNotBlank(requstStr) && requstStr.contains("{") && requstStr.contains("}")){
            mapParams =  JSONObject.toJavaObject(JSON.parseObject(requstStr), Map.class);
        }else {
            mapParams.put("pay-core-respData",requstStr);
        }
        return mapParams;
    }



    /**
     * 转发回复内容至第三方支付(无论验证是否成功，总返回第三方支付验证成功,目的：使第三方不再继续发送通知，具体错误由人工处理)
     */
    public  void writeResponse(HttpServletResponse response, String result,String RESPONSE_MSG){
        PrintWriter out = null;

        try {
            out = response.getWriter();
            String responseMsg = getResponseMsg(result,RESPONSE_MSG);
            if(StringUtils.isNotBlank(responseMsg)) responseMsg = responseMsg.replaceAll("\\\\","");
            if(StringUtils.isNotBlank(responseMsg) && responseMsg.contains("|") && responseMsg.split("\\|").length==2){
                response.setContentType(responseMsg.split("\\|")[0]);
                responseMsg = responseMsg.split("\\|")[1];
            }
            if(StringUtils.isBlank(responseMsg) )   responseMsg = "ERROR";
            out.print(responseMsg);
            out.flush();
        } catch (IOException e) {
            log.error(result,e);
        }finally {
            if(null!=out){
                out.close();
            }
        }
    }


    /**
     * 查询返回第三方内容
     */
    public  String getResponseMsg(String result,String RESPONSE_MSG){
        if(null!=result && !"".equalsIgnoreCase(result)){
            Map<String,String> map = jsonToMap(result);
            if(map!=null && !map.isEmpty()){
                String responsePayMsg = map.get(RESPONSE_MSG);
                return responsePayMsg;
            }
        }
        return null;
    }


}
