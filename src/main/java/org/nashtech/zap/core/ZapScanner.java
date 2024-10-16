package org.nashtech.zap.core;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.nashtech.zap.config.ZapConfig;
import org.zaproxy.clientapi.core.*;

import java.util.List;
import java.util.stream.Collectors;

public class ZapScanner {
    private ZapClient zapClient;
    private RequestSpecification requestSpecification;
    private Response response;

    public ZapScanner(int zapPort) {
        zapClient = new ZapClient(zapPort);
    }

    public void startScan(String targetUrl) {
        try {
            ApiResponse response = zapClient.getClientApi().ascan.scan(targetUrl, "True", "False", null, null, null);
            System.out.println("Scan started: " + response.toString());
        } catch (ClientApiException e) {
            System.out.println("Error starting scan" + e);
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

    public void callZapRestAssured(int zapPort) {
        ZapConfig config = new ZapConfig();
        String zapAddress = config.getProperty("zap.address");
        String zapApiKey = config.getProperty("zap.apikey");
        requestSpecification = RestAssured.given();
        requestSpecification.baseUri("http://" + zapAddress + ":" + zapPort + "/JSON");
        requestSpecification.queryParam("apikey", zapApiKey);

        //proxy = new Proxy().setSslProxy(zapAddress + ":" + zapPort).setHttpProxy(zapAddress + ":" + zapPort);
    }

    public void addApiUrlToScanTree (String site_to_test){
        requestSpecification.queryParam("url", site_to_test);
        response = requestSpecification.get("/core/action/accessUrl/");
        if (response.getStatusCode() == 200)
            System.out.println("URL has been added to Scan tree");
    }

    public void startApiActiveScan (String site_to_test){
        requestSpecification.queryParam("url", site_to_test);
        response = requestSpecification.get("/ascan/action/scan/");
        if (response.getStatusCode() == 200) {
            System.out.println("Active scan has started");
            waitForApiActiveScanCompletion();
        }
    }

    public void waitForApiActiveScanCompletion () {
        response = requestSpecification.get("/ascan/view/status/");
        String status = response.jsonPath().get("status");
        int previousPercentage = 0; // Variable to track the previous percentage
        while (!status.equals("100")) {
            // Get the current status as an integer percentage
            int currentPercentage = Integer.parseInt(status);
            // Print status only if 10% more has been completed compared to the previous
            if (currentPercentage >= previousPercentage + 10) {
                System.out.println("Active scan is " + currentPercentage + "% complete");
                previousPercentage = currentPercentage; // Update previous percentage
            }
            // Sleep for a while to avoid too many requests
            try {
                Thread.sleep(5000); // Wait for 5 seconds before checking again
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // Update the status
            response = requestSpecification.get("/ascan/view/status/");
            status = response.jsonPath().get("status");
        }

        System.out.println("Active scan has completed");
    }


}
