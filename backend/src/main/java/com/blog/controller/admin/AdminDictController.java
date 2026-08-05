package com.blog.controller.admin;

import com.blog.common.Result;
import com.blog.entity.DataDict;
import com.blog.service.DictService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/api/dict")
public class AdminDictController {

    private final DictService dictService;

    public AdminDictController(DictService dictService) {
        this.dictService = dictService;
    }

    @GetMapping("/list")
    public Result<List<DataDict>> list(@RequestParam(required = false) String type) {
        return Result.ok(dictService.list(type));
    }

    @PostMapping("/save")
    public Result<DataDict> save(@RequestBody DataDict dict) {
        return Result.ok(dictService.save(dict));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        dictService.delete(id);
        return Result.ok();
    }
}
