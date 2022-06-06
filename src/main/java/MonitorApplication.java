import cn.vorbote.core.time.DateTime;
import lombok.extern.slf4j.Slf4j;

import java.io.*;

/**
 * MonitorApplication<br>
 * Created at Jun 06, 2022 21:9:34 PM
 *
 * @author vorbote
 */
@Slf4j
public class MonitorApplication {

    public static void main(String... args) throws IOException {
        final BufferedWriter writer = new BufferedWriter(new FileWriter("server-report.txt", true));

        var host = "127.0.0.1";
        if (args.length != 0) {
            host = args[0];
        }

        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("cmd.exe", "/c", "ping -t " + host);

        try {

            Process process = processBuilder.start();

            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream()));

            var connect = 0;
            var timeout = 0;
            var timeoutHistory = 0;
            DateTime breakOutTime = null,
                    recoveryTime = null;

            String line;
            while ((line = reader.readLine()) != null) {
                // System.out.println(line);

                // 如果包含 timed out 或 host unreachable ，则视为无法连接
                if (line.contains("timed out") || line.contains("host unreachable")) {
                    timeout++;
                }

                // 如果包含 TTL= 字样则视为连接成功
                if (line.contains("TTL=")) {
                    connect++;
                    timeout = 0;
                }

                // 如果失败超过 10 次，则将连接成功数清零，并在第十次时记录崩溃时间
                if (timeout >= 10) {
                    connect = 0;
                    if (timeout == 10) {
                        breakOutTime = DateTime.now();
                        String result = String.format("服务器 [%s] 出现异常，当前时间：%s\n", host, breakOutTime);
                        writer.write(result);
                    }
                    timeoutHistory = timeout;

                }

                // 成功连接三次并之前断连超过十次，则视为重新连接成功
                if (connect >= 3 && timeoutHistory >= 10) {
                    recoveryTime = DateTime.now();
                    timeoutHistory = 0;
                    String result = String.format("服务器 [%s] 恢复连接，当前时间：%s\n", host, recoveryTime);
                    writer.write(result);
                    String report = String.format("服务器 [%s] 宕机时长：%s\n", host,
                            recoveryTime.minus(breakOutTime));
                    writer.write(report);
                }
                log.info("超时次数：{}，连接成功次数：{}，历史失败次数：{}，连接失败时间：{}，恢复连接时间：{}",
                        timeout, connect, timeoutHistory, breakOutTime, recoveryTime);
                writer.flush();
            }

            int exitCode = process.waitFor();
            System.out.println("\nExited with error code : " + exitCode);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            writer.close();
        }
    }

}
