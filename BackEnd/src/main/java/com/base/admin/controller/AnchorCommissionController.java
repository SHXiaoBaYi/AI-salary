package com.base.admin.controller;

import com.base.admin.annotation.Log;
import com.base.admin.annotation.RequiresPermission;
import com.base.admin.common.PageResult;
import com.base.admin.common.Result;
import com.base.admin.domain.dto.AncChannelSaveDTO;
import com.base.admin.domain.dto.AncPreviewDTO;
import com.base.admin.domain.dto.AncProductRateDTO;
import com.base.admin.domain.dto.AncProductRateQueryDTO;
import com.base.admin.domain.vo.AncChannelRuleVO;
import com.base.admin.domain.vo.AncPolicyVO;
import com.base.admin.domain.vo.AncPreviewVO;
import com.base.admin.domain.vo.AncProductRateVO;
import com.base.admin.service.AnchorCommissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "主播提成", description = "直播单品标准、渠道阶梯与试算")
@RestController
@RequestMapping("/payroll/anchor")
@RequiredArgsConstructor
public class AnchorCommissionController {

    private final AnchorCommissionService anchorCommissionService;

    @Operation(summary = "核算口径")
    @GetMapping("/policy")
    @RequiresPermission("payroll:anchor:list")
    public Result<AncPolicyVO> policy() {
        return Result.ok(anchorCommissionService.policy());
    }

    @Operation(summary = "单品提成标准分页")
    @PostMapping("/products/list")
    @RequiresPermission("payroll:anchor:list")
    public Result<PageResult<AncProductRateVO>> pageProducts(@RequestBody(required = false) AncProductRateQueryDTO query) {
        return Result.ok(anchorCommissionService.pageProducts(query == null ? new AncProductRateQueryDTO() : query));
    }

    @Operation(summary = "保存单品提成标准")
    @PostMapping("/products")
    @RequiresPermission("payroll:anchor:edit")
    @Log(title = "主播提成标准", businessType = 1)
    public Result<Void> saveProduct(@Valid @RequestBody AncProductRateDTO dto) {
        anchorCommissionService.saveProduct(dto);
        return Result.ok();
    }

    @Operation(summary = "删除单品提成标准")
    @DeleteMapping("/products/{id}")
    @RequiresPermission("payroll:anchor:edit")
    @Log(title = "主播提成标准", businessType = 3)
    public Result<Void> deleteProduct(@PathVariable Long id) {
        anchorCommissionService.deleteProduct(id);
        return Result.ok();
    }

    @Operation(summary = "渠道阶梯")
    @GetMapping("/channels")
    @RequiresPermission("payroll:anchor:list")
    public Result<List<AncChannelRuleVO>> channels() {
        return Result.ok(anchorCommissionService.channels());
    }

    @Operation(summary = "保存渠道阶梯")
    @PutMapping("/channels")
    @RequiresPermission("payroll:anchor:edit")
    @Log(title = "主播渠道提成", businessType = 2)
    public Result<Void> saveChannel(@Valid @RequestBody AncChannelSaveDTO dto) {
        anchorCommissionService.saveChannel(dto);
        return Result.ok();
    }

    @Operation(summary = "提成试算")
    @PostMapping("/preview")
    @RequiresPermission("payroll:anchor:list")
    public Result<AncPreviewVO> preview(@Valid @RequestBody AncPreviewDTO dto) {
        return Result.ok(anchorCommissionService.preview(dto));
    }
}
