package com.base.admin.controller;

import com.base.admin.annotation.Log;
import com.base.admin.annotation.RequiresPermission;
import com.base.admin.common.PageResult;
import com.base.admin.common.Result;
import com.base.admin.domain.dto.AncFanDTO;
import com.base.admin.domain.dto.AncFanQueryDTO;
import com.base.admin.domain.dto.AncRefundDTO;
import com.base.admin.domain.dto.AncRefundQueryDTO;
import com.base.admin.domain.dto.AncScheduleDTO;
import com.base.admin.domain.dto.AncScheduleQueryDTO;
import com.base.admin.domain.dto.AncWeekQueryDTO;
import com.base.admin.domain.vo.AncFanVO;
import com.base.admin.domain.vo.AncRefundVO;
import com.base.admin.domain.vo.AncScheduleVO;
import com.base.admin.domain.vo.AncWeekResultVO;
import com.base.admin.service.AnchorLiveService;
import com.base.admin.service.AnchorWeekService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "主播周核算", description = "售后退款、直播排班、新增粉丝、一键按周核算")
@RestController
@RequestMapping("/payroll/anchor")
@RequiredArgsConstructor
public class AnchorWeekController {

    private final AnchorLiveService anchorLiveService;
    private final AnchorWeekService anchorWeekService;

    @Operation(summary = "售后退款分页")
    @PostMapping("/refunds/list")
    @RequiresPermission("payroll:anchor:list")
    public Result<PageResult<AncRefundVO>> pageRefunds(@RequestBody(required = false) AncRefundQueryDTO query) {
        return Result.ok(anchorLiveService.pageRefunds(query == null ? new AncRefundQueryDTO() : query));
    }

    @Operation(summary = "保存售后退款")
    @PostMapping("/refunds")
    @RequiresPermission("payroll:anchor:edit")
    @Log(title = "主播售后退款", businessType = 1)
    public Result<Void> saveRefund(@Valid @RequestBody AncRefundDTO dto) {
        anchorLiveService.saveRefund(dto);
        return Result.ok();
    }

    @Operation(summary = "删除售后退款")
    @DeleteMapping("/refunds/{id}")
    @RequiresPermission("payroll:anchor:edit")
    @Log(title = "主播售后退款", businessType = 3)
    public Result<Void> deleteRefund(@PathVariable Long id) {
        anchorLiveService.deleteRefund(id);
        return Result.ok();
    }

    @Operation(summary = "直播排班分页")
    @PostMapping("/schedules/list")
    @RequiresPermission("payroll:anchor:list")
    public Result<PageResult<AncScheduleVO>> pageSchedules(@RequestBody(required = false) AncScheduleQueryDTO query) {
        return Result.ok(anchorLiveService.pageSchedules(query == null ? new AncScheduleQueryDTO() : query));
    }

    @Operation(summary = "保存直播排班")
    @PostMapping("/schedules")
    @RequiresPermission("payroll:anchor:edit")
    @Log(title = "主播直播排班", businessType = 1)
    public Result<Void> saveSchedule(@Valid @RequestBody AncScheduleDTO dto) {
        anchorLiveService.saveSchedule(dto);
        return Result.ok();
    }

    @Operation(summary = "删除直播排班")
    @DeleteMapping("/schedules/{id}")
    @RequiresPermission("payroll:anchor:edit")
    @Log(title = "主播直播排班", businessType = 3)
    public Result<Void> deleteSchedule(@PathVariable Long id) {
        anchorLiveService.deleteSchedule(id);
        return Result.ok();
    }

    @Operation(summary = "新增粉丝分页")
    @PostMapping("/fans/list")
    @RequiresPermission("payroll:anchor:list")
    public Result<PageResult<AncFanVO>> pageFans(@RequestBody(required = false) AncFanQueryDTO query) {
        return Result.ok(anchorLiveService.pageFans(query == null ? new AncFanQueryDTO() : query));
    }

    @Operation(summary = "保存新增粉丝")
    @PostMapping("/fans")
    @RequiresPermission("payroll:anchor:edit")
    @Log(title = "主播新增粉丝", businessType = 1)
    public Result<Void> saveFan(@Valid @RequestBody AncFanDTO dto) {
        anchorLiveService.saveFan(dto);
        return Result.ok();
    }

    @Operation(summary = "删除新增粉丝")
    @DeleteMapping("/fans/{id}")
    @RequiresPermission("payroll:anchor:edit")
    @Log(title = "主播新增粉丝", businessType = 3)
    public Result<Void> deleteFan(@PathVariable Long id) {
        anchorLiveService.deleteFan(id);
        return Result.ok();
    }

    @Operation(summary = "查看已核算的周结果")
    @PostMapping("/weeks/list")
    @RequiresPermission("payroll:anchor:list")
    public Result<AncWeekResultVO> listWeeks(@RequestBody(required = false) AncWeekQueryDTO query) {
        return Result.ok(anchorWeekService.list(query == null ? new AncWeekQueryDTO() : query));
    }

    @Operation(summary = "一键按周核算")
    @PostMapping("/weeks/settle")
    @RequiresPermission("payroll:anchor:edit")
    @Log(title = "主播周核算", businessType = 1)
    public Result<AncWeekResultVO> settle(@RequestBody(required = false) AncWeekQueryDTO query) {
        return Result.ok(anchorWeekService.settle(query == null ? new AncWeekQueryDTO() : query));
    }
}
