package com.jingweihe.ojcodesandbox.utils;

import cn.hutool.core.util.StrUtil;
import org.springframework.util.StopWatch;

import java.io.*;

public class ProcessUtils {

    /**
     * 执行进程并获取信息
     * @param runProcess
     * @return
     * @throws InterruptedException
     * @throws IOException
     */
    public static ExecuteMessage runProcessAndMessage(Process runProcess, String opName) throws InterruptedException, IOException {
        ExecuteMessage executeMessage = new ExecuteMessage();
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        // 等待程序执行，获取错误码
        int exitValue = runProcess.waitFor();
        executeMessage.setExitValue(exitValue);
        // 正常退出
        if (exitValue == 0){
            System.out.println(opName + "成功");
            // 分批获取进程的正常输出
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
            // 逐行读取
            StringBuilder compileOutputStringBuilder = new StringBuilder();
            String compileOutputLine;
            while ((compileOutputLine = bufferedReader.readLine()) != null) {
                compileOutputStringBuilder.append(compileOutputLine).append("\n");
            }
            executeMessage.setMessage(compileOutputStringBuilder.toString());
        }else {
            // 异常退出
            System.out.println(opName + "失败，错误码:" + exitValue);
            // 分批获取进程的正常输出
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
            // 逐行读取
            StringBuilder compileOutputStringBuilder = new StringBuilder();
            String compileOutputLine;
            while ((compileOutputLine = bufferedReader.readLine()) != null) {
                compileOutputStringBuilder.append(compileOutputLine);
            }
            // 分批获取进程的异常输出
            BufferedReader errorBufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
            StringBuilder errorCompileOutputStringBuilder = new StringBuilder();
            // 逐行读取
            String errorCompileOutputLine;
            while ((errorCompileOutputLine = errorBufferedReader.readLine()) != null) {
                errorCompileOutputStringBuilder.append(errorCompileOutputLine).append("\n");
            }
            executeMessage.setMessage(compileOutputStringBuilder.toString());
            executeMessage.setErrorMessage(errorBufferedReader.toString());
        }
        stopWatch.stop();
        long lastTaskTimeMillis = stopWatch.getLastTaskTimeMillis();
        executeMessage.setTime(lastTaskTimeMillis);
        return executeMessage;
    }

    /**
     * 执行交互式进程并获取信息
     * @param runProcess
     * @return
     * @throws InterruptedException
     * @throws IOException
     */
    public static ExecuteMessage runInterProcessAndMessage(Process runProcess, String opName, String args) throws InterruptedException, IOException {
        ExecuteMessage executeMessage = new ExecuteMessage();
        // 向控制台输入程序
        OutputStream outputStream = runProcess.getOutputStream();
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
        String[] s = args.split(" ");
        String join = StrUtil.join("\n", s) + "\n";
        outputStreamWriter.write(join);
        // 相当于按了回车，执行输入的发送
        outputStreamWriter.flush();

        // 分批获取进程的正常输出
        InputStream inputStream = runProcess.getInputStream();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        // 逐行读取
        StringBuilder compileOutputStringBuilder = new StringBuilder();
        String compileOutputLine;
        while ((compileOutputLine = bufferedReader.readLine()) != null) {
            compileOutputStringBuilder.append(compileOutputLine).append("\n");
        }
        executeMessage.setMessage(compileOutputStringBuilder.toString());
        outputStreamWriter.close();
        outputStream.close();
        inputStream.close();
        runProcess.destroy();
        return executeMessage;
    }
}
