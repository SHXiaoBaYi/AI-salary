package com.base.admin.controller;

import com.base.admin.annotation.Log;
import com.base.admin.annotation.RequiresPermission;
import com.base.admin.common.PageResult;
import com.base.admin.common.Result;
import com.base.admin.domain.dto.PayEmployeeDTO;
import com.base.admin.domain.dto.PayEmployeeQueryDTO;
import com.base.admin.domain.vo.PayDingTalkSyncVO;
import com.base.admin.domain.vo.PayEmployeeVO;
import com.base.admin.service.PayrollDingTalkSyncService;
import com.base.admin.service.PayrollEmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "算薪员工", description = "员工档案")
@RestController
@RequestMapping("/payroll/employee")
@RequiredArgsConstructor
public class PayrollEmployeeController {

    private final PayrollEmployeeService employeeService;
    private final PayrollDingTalkSyncService dingTalkSyncService;

    @Operation(summary = "分页查询员工")
    @PostMapping("/list")
    @RequiresPermission("payroll:employee:list")
    public Result<PageResult<PayEmployeeVO>> list(@RequestBody(required = false) PayEmployeeQueryDTO query) {
        return Result.ok(employeeService.list(query == null ? new PayEmployeeQueryDTO() : query));
    }

    @Operation(summary = "从钉钉企业通讯录同步员工")
    @PostMapping("/sync-dingtalk")
    @RequiresPermission("payroll:employee:edit")
    @Log(title = "算薪员工-钉钉同步", businessType = 2)
    public Result<PayDingTalkSyncVO> syncDingTalk() {
        return Result.ok(dingTalkSyncService.sync());
    }

    @Operation(summary = "保存员工")
    @PostMapping
    @RequiresPermission("payroll:employee:edit")
    @Log(title = "算薪员工", businessType = 1)
    public Result<Void> save(@Valid @RequestBody PayEmployeeDTO dto) {
        employeeService.save(dto);
        return Result.ok();
    }

    @Operation(summary = "删除员工")
    @DeleteMapping("/{id}")
    @RequiresPermission("payroll:employee:edit")
    @Log(title = "算薪员工", businessType = 3)
    public Result<Void> delete(@PathVariable Long id) {
        employeeService.delete(id);
        return Result.ok();
    }
}
