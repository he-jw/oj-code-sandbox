package com.jingweihe.ojcodesandbox;

import com.jingweihe.ojcodesandbox.mode.ExecuteCodeRequest;
import com.jingweihe.ojcodesandbox.mode.ExecuteCodeResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Java 原生代码沙箱实现（直接复用模版方法）
 */
@Slf4j
public class JavaNativeCodeSandBox extends JavaCodeSandBoxTemplate {

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        return super.executeCode(executeCodeRequest);
    }
}
