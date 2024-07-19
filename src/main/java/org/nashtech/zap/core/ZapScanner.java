package org.nashtech.zap.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zaproxy.clientapi.core.*;

import java.util.List;
import java.util.stream.Collectors;

public class ZapScanner {
    private static final Logger logger = LogManager.getLogger(ZapScanner.class);
    private ZapClient zapClient;

    public ZapScanner(int zapPort) {
        zapClient = new ZapClient(zapPort);
    }

    public void startScan(String targetUrl) {
        try {
            ApiResponse response = zapClient.getClientApi().ascan.scan(targetUrl, "True", "False", null, null, null);
            System.out.println("Scan started: " + response.toString());
        } catch (ClientApiException e) {
            logger.error("Error starting scan", e);
        }
    }

    public List<String>  getUrlsFromScanTree() throws ClientApiException {
        ClientApi api = zapClient.getClientApi();
        ApiResponse apiResponse = api.core.urls();
        List<ApiResponse> urlList= ((ApiResponseList)apiResponse).getItems();
        return urlList.stream().map(r -> ((ApiResponseElement)r).getValue()).collect(Collectors.toList());
    }

    public void cleanTheScanTree() throws ClientApiException {
        ClientApi api = zapClient.getClientApi();
        List<String> urls=getUrlsFromScanTree();
        for (String url:urls){
            if(getUrlsFromScanTree().stream().anyMatch(s->s.contains(url))){
                api.core.deleteSiteNode(url,"","");
            }
        }
        if(getUrlsFromScanTree().isEmpty())
            System.out.println("scan tree has been cleared successfully");
        else
            throw new RuntimeException("scan tree was not cleared");

    }

    public void activeScan(String webAppUrl) throws ClientApiException {
        String scanId;
        ClientApi api = zapClient.getClientApi();
        ApiResponse apiResponse = api.ascan.scan(webAppUrl, "True", "False", null, null, null);
        System.out.println("apiResponseActive : " + apiResponse.getName());

        scanId = ((ApiResponseElement)apiResponse).getValue();
        ApiResponse activeScanStatus = api.ascan.status(scanId);
        String statusAs = ((ApiResponseElement)activeScanStatus).getValue();
        while(!statusAs.equals("100")){
            activeScanStatus = api.ascan.status(scanId);
            statusAs = ((ApiResponseElement)activeScanStatus).getValue();
        }
    }

    public void waitTillPassiveScanCompleted() throws ClientApiException {
        System.out.println("Starting Passive Scan");
        ClientApi api = zapClient.getClientApi();
        ApiResponse apiResponsePs = api.pscan.recordsToScan();
        String scanRecords = ((ApiResponseElement)apiResponsePs).getValue();
        while(!scanRecords.equals("0")){
            apiResponsePs = api.pscan.recordsToScan();
            scanRecords = ((ApiResponseElement)apiResponsePs).getValue();
        }
        System.out.println("Passive scan completed");
    }

    public void addUrlScanTree(String webAppUrl) throws ClientApiException {
        ClientApi api = zapClient.getClientApi();
        api.core.accessUrl(webAppUrl, "false");
        if(getUrlsFromScanTree().contains(webAppUrl)){
            System.out.println("Sites to test : " + getUrlsFromScanTree());
        }
    }

    public void AllScan(String webAppUrl, String activeScanBool) throws ClientApiException {
        System.out.println("Starting Spider Scan");
        ClientApi api = zapClient.getClientApi();
        ApiResponse apiResponse = api.spider.scan(webAppUrl, null, null, null, null);
        String spiderScanId = ((ApiResponseElement)apiResponse).getValue();
        ApiResponse spiderScanStatus = api.spider.status(spiderScanId);
        String statusAs = ((ApiResponseElement)spiderScanStatus).getValue();
        while(!statusAs.equals("100")){
            spiderScanStatus = api.spider.status(spiderScanId);
            statusAs = ((ApiResponseElement)spiderScanStatus).getValue();

        }
        System.out.println("Spider scan completed");

        waitTillPassiveScanCompleted();

        addUrlScanTree(webAppUrl);

        if(activeScanBool!=null&&activeScanBool.equalsIgnoreCase("true")){
            System.out.println("Starting Active Scan");
            api.ascan.enableAllScanners(null);
            activeScan(webAppUrl);
            System.out.println("Active scan completed");
        }
    }


}
