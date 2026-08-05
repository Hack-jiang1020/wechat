package com.blog.controller.admin;

import com.blog.common.PageResult;
import com.blog.common.Result;
import com.blog.entity.SystemLog;
import com.blog.repository.SystemLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/api/log")
public class AdminLogController {

    private final SystemLogRepository systemLogRepository;

    public AdminLogController(SystemLogRepository systemLogRepository) {
        this.systemLogRepository = systemLogRepository;
    }

    @GetMapping("/list")
    public Result<PageResult<SystemLog>> list(@RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(100, Math.max(1, size));
        Page<SystemLog> result = systemLogRepository.findAll(
                PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "createTime")));
        return Result.ok(new PageResult<>(result.getTotalElements(), result.getContent()));
    }

    @DeleteMapping("/clear")
    public Result<Void> clear() {
        systemLogRepository.deleteAll();
        return Result.ok();
    }
}
