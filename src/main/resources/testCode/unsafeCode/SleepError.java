/**
 * 无限睡眠（阻塞程序执行）
 */
public class Main {
    public static void main(String[] args) throws InterruptedException {
        long ONE_HOUR = 10 * 1000L;
        Thread.sleep(ONE_HOUR);
        System.out.println("睡完了");
    }
}

