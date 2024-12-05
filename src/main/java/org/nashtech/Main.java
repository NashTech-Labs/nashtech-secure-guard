package org.nashtech;

import org.nashtech.zap.core.ZapManager;
import org.nashtech.zap.core.ZapReport;
import org.nashtech.zap.core.ZapScanner;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        ZapManager zapManager = new ZapManager();
       zapManager.runTrivyCommand("nginx:latest redis:6.2 postgres alpine:3.14", "json");
    }
}