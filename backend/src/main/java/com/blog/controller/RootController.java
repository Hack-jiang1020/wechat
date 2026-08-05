package com.blog.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
public class RootController {

    /**
     * 根路径相对跳转到管理后台，保持当前协议（https 不会被降级为 http）
     */
    @GetMapping("/")
    public ResponseEntity<Void> root() {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create("/admin/"));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}