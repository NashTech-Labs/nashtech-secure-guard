package org.nashtech.zap.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nashtech.zap.config.ZapConfig;
import org.zaproxy.clientapi.core.ApiResponse;
import org.zaproxy.clientapi.core.ClientApi;
import org.zaproxy.clientapi.core.ClientApiException;

import java.io.IOException;

public class ZapReport {
    private static final Logger logger = LogManager.getLogger(ZapReport.class);
    private ZapClient zapClient;

    public ZapReport(int zapPort) {
        zapClient = new ZapClient(zapPort);
    }

    public void generateReport(String reportName, String reportHeading) {
        ClientApi api = zapClient.getClientApi();
        ZapManager zapManager = new ZapManager();
        ZapConfig config = new ZapConfig();
        String projectRoot = System.getProperty("user.dir");
        if (api != null) {
            try {
                String description = "";
                String template = "traditional-html";
                System.out.println("title : " + reportHeading);
                System.out.println("fileName : " + reportName);
                System.out.println("projectRoot : " + projectRoot);
                ApiResponse response = api.reports.generate(
                        reportHeading,
                        template,
                        null,
                        description,
                        null,
                        null,
                        null,
                        null,
                        null,
                        reportName,
                        null,
                        null,
                        null);

                System.out.println("ZAP report generated: " + response.toString());
                zapManager.runCommand("docker cp secure-guard-container:/home/zap/security-test-report.html " + projectRoot);
            } catch (ClientApiException e) {
                System.out.println("Error generating ZAP report " + e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
