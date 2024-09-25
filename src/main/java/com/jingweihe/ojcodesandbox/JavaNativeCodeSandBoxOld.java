package com.jingweihe.ojcodesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.dfa.FoundWord;
import cn.hutool.dfa.WordTree;
import com.jingweihe.ojcodesandbox.mode.ExecuteCodeRequest;
import com.jingweihe.ojcodesandbox.mode.ExecuteCodeResponse;
import com.jingweihe.ojcodesandbox.mode.JudgeInfo;
import com.jingweihe.ojcodesandbox.utils.ExecuteMessage;
import com.jingweihe.ojcodesandbox.utils.ProcessUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class JavaNativeCodeSandBoxOld implements CodeSandBox {

    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";

    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";

    private static final long TIME_OUT = 5000L;

    private static final List<String> blackList = Arrays.asList("Files","exec");

    private static final WordTree wordTree;

    static {
        // 校验代码初始化
        wordTree = new WordTree();
        wordTree.addWords(blackList);
    }

    public static void main(String[] args) {
        JavaNativeCodeSandBoxOld javaNativeCodeSandBox = new JavaNativeCodeSandBoxOld();
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList("1 2","3 4"));
        // 从main函数的args中读取输入示例的方式
        // String code = ResourceUtil.readStr("testCode/simpleComputeArgs/Main.java", StandardCharsets.UTF_8);
        String code = ResourceUtil.readStr("testCode/unsafeCode/ProcessError.java", StandardCharsets.UTF_8);

        // 采用ACM的交互式输入来读取程序的输入示例
        // String code = ResourceUtil.readStr("testCode/simpleCompute/Main.java", StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("java");
        ExecuteCodeResponse executeCodeResponse = javaNativeCodeSandBox.executeCode(executeCodeRequest);
        System.out.println(executeCodeResponse);
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();

        // 校验代码中是否包含黑名单中的命令
        FoundWord foundWord = wordTree.matchWord(code);
        if (foundWord != null){
            String word = foundWord.getFoundWord();
            if (StrUtil.isNotBlank(word)){
                System.out.println("包含禁止词" + word);
                return null;
            }
        }

        // 1.把用户的代码保存为文件
        String userDir = System.getProperty("user.dir"); // 获取当前工作项目的根目录
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;
        // 判断全局代码目录是否存在,没有则新建
        if (!FileUtil.exist(globalCodePathName)) {
            FileUtil.mkdir(globalCodePathName);
        }
        // 把用户的代码隔离存放
        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();
        String userCodePath = userCodeParentPath + File.separator + GLOBAL_JAVA_CLASS_NAME;
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);

        // 2.编译代码，得到 class 文件
        String compileCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsoluteFile());
        try {
            Process compileProcess = Runtime.getRuntime().exec(compileCmd);
            String opName = "编译";
            ExecuteMessage executeMessage = ProcessUtils.runProcessAndMessage(compileProcess, opName);
            System.out.println(executeMessage);
        } catch (IOException | InterruptedException e) {
            return getErrorResponse(e);
        }

        // 3.执行代码，得到输出结果
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String inputArgs : inputList) {
            // 从main函数的args中读取输入示例的方式
            String runCmd = String.format("java -Xmx128m -Dfile.encoding=utf-8 -cp %s Main %s", userCodeParentPath, inputArgs);

            // 采用ACM的交互式输入来读取程序的输入示例
            // String runCmd = String.format("java -Dfile.encoding=UTF-8 -cp %s Main", userCodeParentPath);

            try {
                Process runProcess = Runtime.getRuntime().exec(runCmd);
                // 超时控制
                new Thread(() -> {
                    try {
                        Thread.sleep(TIME_OUT);
                        runProcess.destroy();
                        System.out.println("运行超时");
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }).start();
                String opName = "运行";
                // 从main函数的args中读取输入示例的方式
                ExecuteMessage executeMessage = ProcessUtils.runProcessAndMessage(runProcess, opName);

                // 采用ACM的交互式输入来读取程序的输入示例
                // ExecuteMessage executeMessage = ProcessUtils.runInterProcessAndMessage(runProcess, opName, inputArgs);
                System.out.println(executeMessage);
                executeMessageList.add(executeMessage);
            } catch (IOException | InterruptedException e) {
                return getErrorResponse(e);
            }
        }

        // 4.收集整理输出结果
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        Long maxExecuteTime = 0L;
        List<String> outputList = new ArrayList<>();
        for (ExecuteMessage executeMessage : executeMessageList) {
            String errorMessage = executeMessage.getErrorMessage();
            if (StrUtil.isNotBlank(errorMessage)){
                executeCodeResponse.setMessage(errorMessage);
                // 执行中存在错误
                executeCodeResponse.setStatus(3);
                break;
            }
            Long time = executeMessage.getTime();
            if (time != null){
                maxExecuteTime = Math.max(maxExecuteTime, time);
            }
            outputList.add(executeMessage.getMessage());
        }
        // 正常运行完成
        if (outputList.size() == executeMessageList.size()) {
            executeCodeResponse.setStatus(1);
        }
        executeCodeResponse.setOutputList(outputList);
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setTime(maxExecuteTime);
        // 要借助第三库来获取内存占用，非常麻烦，此处不做实现
        // judgeInfo.setMemory();
        executeCodeResponse.setJudgeInfo(judgeInfo);

        // 5.文件清理
        if (userCodeFile.getParentFile() != null) {
            boolean del = FileUtil.del(userCodeParentPath);
            System.out.println("删除" + (del ? "成功" : "失败"));
        }
        return executeCodeResponse;
    }

    /**
     * 获取错误响应
     * @param e
     * @return
     */
    private ExecuteCodeResponse getErrorResponse(Throwable e){
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setOutputList(new ArrayList<>());
        executeCodeResponse.setMessage(e.getMessage());
        // 代表代码沙箱错误
        executeCodeResponse.setStatus(2);
        executeCodeResponse.setJudgeInfo(new JudgeInfo());
        return executeCodeResponse;
    }
}
