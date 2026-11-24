package com.base.admin.controller;

import com.base.admin.annotation.Log;
import com.base.admin.annotation.RequiresPermission;
import com.base.admin.common.PageResult;
import com.base.admin.common.Result;
import com.base.admin.domain.dto.AncAnchorDTO;
import com.base.admin.domain.dto.AncAnchorQueryDTO;
import com.base.admin.domain.dto.AncSaleLineDTO;
import com.base.admin.domain.dto.AncSaleLineQueryDTO;
import com.base.admin.domain.vo.AncAnchorVO;
import com.base.admin.domain.vo.AncSaleLineVO;
import com.base.admin.service.AnchorRosterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "主播档案与销售明细", description = "主播管理、抖店直播子订单")
@RestController
@RequestMapping("/payroll/anchor")
@RequiredArgsConstructor
public class AnchorRosterController {

    private final AnchorRosterService anchorRosterService;

    @Operation(summary = "主播分页")
    @PostMapping("/hosts/list")
    @RequiresPermission("payroll:anchor:list")
    public Result<PageResult<AncAnchorVO>> pageAnchors(@RequestBody(required = false) AncAnchorQueryDTO query) {
        return Result.ok(anchorRosterService.pageAnchors(query == null ? new AncAnchorQueryDTO() : query));
    }

    @Operation(summary = "在职主播下拉")
    @GetMapping("/hosts/options")
    @RequiresPermission("payroll:anchor:list")
    public Result<List<AncAnchorVO>> anchorOptions() {
        return Result.ok(anchorRosterService.anchorOptions());
    }

    @Operation(summary = "保存主播")
    @PostMapping("/hosts")
    @RequiresPermission("payroll:anchor:edit")
    @Log(title = "主播档案", businessType = 1)
    public Result<Void> saveAnchor(@Valid @RequestBody AncAnchorDTO dto) {
        anchorRosterService.saveAnchor(dto);
        return Result.ok();
    }

    @Operation(summary = "删除主播")
    @DeleteMapping("/hosts/{id}")
    @RequiresPermission("payroll:anchor:edit")
    @Log(title = "主播档案", businessType = 3)
    public Result<Void> deleteAnchor(@PathVariable Long id) {
        anchorRosterService.deleteAnchor(id);
        return Result.ok();
    }

    @Operation(summary = "销售明细分页")
    @PostMapping("/sales/list")
    @RequiresPermission("payroll:anchor:list")
    public Result<PageResult<AncSaleLineVO>> pageSales(@RequestBody(required = false) AncSaleLineQueryDTO query) {
        return Result.ok(anchorRosterService.pageSales(query == null ? new AncSaleLineQueryDTO() : query));
    }

    @Operation(summary = "保存销售明细")
    @PostMapping("/sales")
    @RequiresPermission("payroll:anchor:edit")
    @Log(title = "主播销售明细", businessType = 1)
    public Result<Void> saveSale(@Valid @RequestBody AncSaleLineDTO dto) {
        anchorRosterService.saveSale(dto);
        return Result.ok();
    }

    @Operation(summary = "删除销售明细")
    @DeleteMapping("/sales/{id}")
    @RequiresPermission("payroll:anchor:edit")
    @Log(title = "主播销售明细", businessType = 3)
    public Result<Void> deleteSale(@PathVariable Long id) {
        anchorRosterService.deleteSale(id);
        return Result.ok();
    }
}
