package io.dataease.controller.panel.server;


import io.dataease.base.domain.PanelLink;
import io.dataease.controller.ResultHolder;
import io.dataease.controller.panel.api.LinkApi;
import io.dataease.controller.request.chart.ChartExtRequest;
import io.dataease.controller.request.panel.link.*;
import io.dataease.dto.panel.link.GenerateDto;
import io.dataease.dto.panel.link.ValidateDto;
import io.dataease.service.chart.ChartViewService;
import io.dataease.service.panel.PanelLinkService;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Map;


@RestController
public class LinkServer implements LinkApi {



    @Autowired
    private PanelLinkService panelLinkService;

    @Resource
    private ChartViewService chartViewService;


    @Override
    public void replacePwd(@RequestBody PasswordRequest request) {
        panelLinkService.password(request);
    }

    @Override
    public void enablePwd(@RequestBody EnablePwdRequest request) {
        panelLinkService.changeEnablePwd(request);
    }

    

    @Override
    public void resetOverTime(@RequestBody OverTimeRequest request) {
        panelLinkService.overTime(request);
        
    }

    @Override
    public void switchLink(@RequestBody LinkRequest request) {
        panelLinkService.changeValid(request);
    }

    @Override
    public GenerateDto currentGenerate(@PathVariable("resourceId") String resourceId) {
        return panelLinkService.currentGenerate(resourceId);
    }

    @Override
    public ValidateDto validate(@RequestBody LinkValidateRequest request)  throws Exception{
        String link = request.getLink();
        String json = panelLinkService.decryptParam(link);

        ValidateDto dto = new ValidateDto();
        String resourceId = json;
       /*  String resourceId = request.getResourceId(); */
        PanelLink one = panelLinkService.findOne(resourceId);
        dto.setResourceId(resourceId);
        if (ObjectUtils.isEmpty(one)){
            dto.setValid(false);
            return dto;
        }
        dto.setValid(one.getValid());
        dto.setEnablePwd(one.getEnablePwd());
        dto.setPassPwd(panelLinkService.validateHeads(one));
        dto.setExpire(panelLinkService.isExpire(one));
        return dto;
    }

    @Override
    public boolean validatePwd(@RequestBody PasswordRequest request) throws Exception {
        return panelLinkService.validatePwd(request);
    }

    @Override
    public Object resourceDetail(@PathVariable String resourceId) {
        return panelLinkService.resourceInfo(resourceId);
    }

    @Override
    public Object viewDetail(String viewId, ChartExtRequest requestList) throws Exception{
        return chartViewService.getData(viewId, requestList);
    }

    /*@Override
    public ResultHolder shortUrl(Map<String,String> param) {
        String url = param.get("url");
        return panelLinkService.getShortUrl(url);
    }*/

    @Override
    public String shortUrl(Map<String,String> param) {
        String resourceId = param.get("resourceId");
        return panelLinkService.getShortUrl(resourceId);
    }
}
