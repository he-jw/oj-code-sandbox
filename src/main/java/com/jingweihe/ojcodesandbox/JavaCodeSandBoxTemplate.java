package com.jingweihe.ojcodesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.jingweihe.ojcodesandbox.mode.ExecuteCodeRequest;
import com.jingweihe.ojcodesandbox.mode.ExecuteCodeResponse;
import com.jingweihe.ojcodesandbox.mode.JudgeInfo;
import com.jingweihe.ojcodesandbox.utils.ExecuteMessage;
import com.jingweihe.ojcodesandbox.utils.ProcessUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * java代码沙箱模版方法实现
 */
@Slf4j
public abstract class JavaCodeSandBoxTemplate implements CodeSandBox {

    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";

    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";

    private static final long TIME_OUT = 5000L;


    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();

        // 1.把用户的代码保存为文件
        File userCodeFile = saveCodeToFile(code);

        // 2.编译代码，得到 class 文件
        ExecuteMessage compileFileExecuteMessage = compileFile(userCodeFile);
        System.out.println(compileFileExecuteMessage);

        // 3.执行代码，得到输出结果
        List<ExecuteMessage> executeMessageList = runFile(userCodeFile.getParentFile().getAbsolutePath(), inputList);

        // 4.收集整理输出结果
        ExecuteCodeResponse outputResponse = getOutputResponse(executeMessageList);

        // 5.文件清理
        Boolean deleteFile = deleteFile(userCodeFile);
        if (!deleteFile) {
            log.info("清理文件失败，userCodeFile = {}", userCodeFile.getAbsolutePath());
        }
        return outputResponse;
    }

    /**
     * 1.把用户的代码保存为文件
     * @param code
     * @return
     */
    public File saveCodeToFile(String code) {
        // 获取当前工作项目的根目录
        String userDir = System.getProperty("user.dir");
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;
        // 判断全局代码目录是否存在,没有则新建
        if (!FileUtil.exist(globalCodePathName)) {
            FileUtil.mkdir(globalCodePathName);
        }
        // 把用户的代码隔离存放
        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();
        String userCodePath = userCodeParentPath + File.separator + GLOBAL_JAVA_CLASS_NAME;
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
        return userCodeFile;
    }

    /**
     * 2.编译代码，得到 class 文件
     * @param userCodeFile
     * @return
     */
    public ExecuteMessage compileFile(File userCodeFile) {
        String compileCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsoluteFile());
        try {
            Process compileProcess = Runtime.getRuntime().exec(compileCmd);
            String opName = "编译";
            ExecuteMessage executeMessage = ProcessUtils.runProcessAndMessage(compileProcess, opName);
            if (executeMessage.getExitValue() != 0){
                throw new RuntimeException("编译错误");
            }
            return executeMessage;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException();
        }
    }

    /**
     * 3.执行代码，得到结果执行列表
     * @param userCodeParentPath
     * @param inputList
     * @return
     */
    public List<ExecuteMessage> runFile(String userCodeParentPath, List<String> inputList) {
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
                throw new RuntimeException("执行错误", e);
            }
        }
        return executeMessageList;
    }

    /**
     * 4.获取输出结果
     * @param executeMessageList
     * @return
     */
    public ExecuteCodeResponse getOutputResponse(List<ExecuteMessage> executeMessageList) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        // 统计最大执行时间
        Long maxExecuteTime = 0L;
        // 收集输出结果
        List<String> outputList = new ArrayList<>();
        for (ExecuteMessage executeMessage : executeMessageList) {
            String errorMessage = executeMessage.getErrorMessage();
            if (StrUtil.isNotBlank(errorMessage)){
                // 执行中存在错误
                executeCodeResponse.setMessage(errorMessage);
                executeCodeResponse.setStatus(3);
                break;
            }
            outputList.add(executeMessage.getMessage());
            Long time = executeMessage.getTime();
            if (time != null){
                maxExecuteTime = Math.max(maxExecuteTime, time);
            }
        }
        if (outputList.size() == executeMessageList.size()) {
            // 正常运行完成
            executeCodeResponse.setStatus(1);
        }
        executeCodeResponse.setOutputList(outputList);
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setTime(maxExecuteTime);
        // 要借助第三库来获取内存占用，非常麻烦，此处不做实现
        // judgeInfo.setMemory();
        executeCodeResponse.setJudgeInfo(judgeInfo);
        return executeCodeResponse;
    }

    /**
     * 5.文件清理
     * @param userCodeFile
     * @return
     */
    public Boolean deleteFile(File userCodeFile) {
        boolean del = true;
        if (userCodeFile.getParentFile() != null) {
            String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
            del = FileUtil.del(userCodeParentPath);
            System.out.println("删除" + (del ? "成功" : "失败"));
        }
        return del;
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
