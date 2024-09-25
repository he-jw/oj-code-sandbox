package com.jingweihe.ojcodesandbox.controller;

import com.jingweihe.ojcodesandbox.JavaNativeCodeSandBox;
import com.jingweihe.ojcodesandbox.mode.ExecuteCodeRequest;
import com.jingweihe.ojcodesandbox.mode.ExecuteCodeResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController("/")
public class MainController {

    @Resource
    private JavaNativeCodeSandBox javaNativeCodeSandBox;

    /**
     * 执行代码
     * @param executeCodeRequest
     * @return
     */
    @PostMapping("/executeCode")
    public ExecuteCodeResponse executeCode(@RequestBody ExecuteCodeRequest executeCodeRequest) {
        if (executeCodeRequest == null){
            throw new RuntimeException("请求参数为空");
        }
        return javaNativeCodeSandBox.executeCode(executeCodeRequest);
    }

    @GetMapping("/health")
    public String healthCheck() {
        return "ok";
    }

}
