package org.nashtech;

import org.nashtech.zap.core.ZapManager;
import org.nashtech.zap.core.ZapReport;
import org.nashtech.zap.core.ZapScanner;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        ZapManager zapManager = new ZapManager();
        /*ZapScanner scanner = new ZapScanner(8080);
        ZapManager zapManager = new ZapManager();
        zapManager.startZap(8080);
        String currentUrl = "https://reqres.in/api/users?page=2";
        System.out.println("currentUrl : " + currentUrl);
        scanner.callZapRestAssured(8080, null, null, null);
        scanner.addApiUrlToScanTree(currentUrl);
        scanner.startApiActiveScan(currentUrl);
        ZapReport report = new ZapReport(8080);
        report.generateReport("api-test-report.html", "Security Testing Report");
        zapManager.stopZap();*/
        zapManager.runTrivyCommand("./trivy-execute.sh nginx:latest redis:6.2 postgres alpine:3.14");
    }
}