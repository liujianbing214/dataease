package io.dataease.controller.panel;

import com.github.xiaoymin.knife4j.annotations.ApiSupport;
import io.dataease.base.domain.DatasetTableField;
import io.dataease.commons.model.BaseRspModel;
import io.dataease.controller.request.panel.PanelLinkageRequest;
import io.dataease.controller.request.panel.PanelSubjectRequest;
import io.dataease.dto.panel.linkJump.PanelLinkJumpBaseRequest;
import io.dataease.dto.panel.linkJump.PanelLinkJumpBaseResponse;
import io.dataease.dto.panel.linkJump.PanelLinkJumpDTO;
import io.dataease.dto.panel.linkJump.PanelLinkJumpInfoDTO;
import io.dataease.service.panel.PanelLinkJumpService;
import io.dataease.service.panel.PanelViewLinkageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * Author: wangjiahao
 * Date: 8/4/21
 * Description:
 */
@Api(tags = "仪表板：仪表板跳转")
@ApiSupport(order = 171)
@RestController
@RequestMapping("linkJump")
public class PanelLinkJumpController {

    @Resource
    private PanelLinkJumpService panelLinkJumpService;

    @ApiOperation("根据视图ID获取对应表字段信息")
    @GetMapping("/getTableFieldWithViewId/{viewId}")
    public List<DatasetTableField>  getTableFieldWithViewId(@PathVariable String viewId){
        return panelLinkJumpService.getViewFields(viewId);
    }


    @ApiOperation("根据仪表板ID和视图ID获取跳转信息")
    @GetMapping("/queryWithViewId/{panelId}/{viewId}")
    public PanelLinkJumpDTO queryWithViewId(@PathVariable String panelId, @PathVariable String viewId){
        return panelLinkJumpService.queryWithView(panelId,viewId);
    }

    @ApiOperation("根据仪表板ID获取跳转信息")
    @GetMapping("/queryPanelJumpInfo/{panelId}")
    public PanelLinkJumpBaseResponse queryPanelJumpInfo(@PathVariable String panelId){
        return panelLinkJumpService.queryPanelJumpInfo(panelId);
    }

    @ApiOperation("更新跳转信息")
    @PostMapping("/updateJumpSet")
    public void updateJumpSet(@RequestBody PanelLinkJumpDTO jumpDTO){
        panelLinkJumpService.updateJumpSet(jumpDTO);
    }

    @ApiOperation("获取仪表板目标仪表板跳转联动信息")
    @PostMapping("/queryTargetPanelJumpInfo")
    public PanelLinkJumpBaseResponse queryTargetPanelJumpInfo(@RequestBody PanelLinkJumpBaseRequest request){
        return panelLinkJumpService.queryTargetPanelJumpInfo(request);
    }
}
