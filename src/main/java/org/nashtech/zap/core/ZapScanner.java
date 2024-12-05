package org.nashtech.zap.core;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.nashtech.zap.config.ZapConfig;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.zaproxy.clientapi.core.*;

import java.util.*;
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

    public WebDriver initializeSeleniumWithZapProxy(WebDriver driver, ChromeOptions options, int zapPort) {
        if (driver != null) {
            throw new IllegalArgumentException("WebDriver must not be already initialized.");
        }
        // Set up the ZAP proxy
        Proxy zapProxy = new Proxy();
        zapProxy.setHttpProxy("localhost:" + zapPort)
                .setSslProxy("localhost:" + zapPort);
        // Apply the proxy to the existing ChromeOptions
        if (options == null) {
            options = new ChromeOptions();
        }
        options.setProxy(zapProxy);
        options.addArguments("--ignore-certificate-errors");
        // Initialize and return a new WebDriver instance
        return new ChromeDriver(options);
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
        api.core.accessUrl(webAppUrl, "true");
        ApiResponse apiResponse = api.spider.scan(webAppUrl, "1", null, null, null);
        String spiderScanId = ((ApiResponseElement)apiResponse).getValue();
        ApiResponse spiderScanStatus = api.spider.status(spiderScanId);
        String statusAs = ((ApiResponseElement)spiderScanStatus).getValue();
        while(!statusAs.equals("100")){
            spiderScanStatus = api.spider.status(spiderScanId);
            statusAs = ((ApiResponseElement)spiderScanStatus).getValue();

        }
        System.out.println("Spider scan completed");
        if(getUrlsFromScanTree().contains(webAppUrl)){
            System.out.println("URL successfully added to Sites tree: " + webAppUrl);
            System.out.println("Sites to test : " + getUrlsFromScanTree());
        }
        else {
            System.err.println("Failed to add URL to Sites tree: " + webAppUrl);
        }
    }

    public void allScan(boolean activeScanEnabled) throws ClientApiException {
        waitTillPassiveScanCompleted();
        ClientApi api = zapClient.getClientApi();
        System.out.println("Checking Sites Tree");
        ApiResponseList sitesList = (ApiResponseList) api.core.sites();
        for (ApiResponse site : sitesList.getItems()) {
            String siteUrl = ((ApiResponseElement) site).getValue();
            // Perform Active Scan if enabled
            if (activeScanEnabled) {
                System.out.println("Starting Active Scan on: " + siteUrl);
                activeScan(siteUrl);
            }
            else{
                System.out.println("URL not enabled for active scan : " + siteUrl);
            }
        }
        System.out.println("All scans completed.");
    }

    public void activeScanImportantUrls( List<String> urlListToScan, boolean activeScanEnabled) throws ClientApiException {
        this.waitTillPassiveScanCompleted();
        ClientApi api = this.zapClient.getClientApi();
        System.out.println("Checking Sites Tree");
        List<String> urlFromSiteTreeList = new ArrayList<>();
        // Check if active scan is enabled for URLs
        if (activeScanEnabled) {
            // Retrieve the full list of URLs from the site tree
            ApiResponse siteTree = api.core.sites();
            ApiResponseList sitesList = (ApiResponseList) api.core.sites();
            for (ApiResponse site : sitesList.getItems()) {
                String siteUrl = ((ApiResponseElement) site).getValue();
                urlFromSiteTreeList.add(siteUrl);
            }
            System.out.println("Extracted URLs: " + urlFromSiteTreeList);
            // Filter URLs based on keywords in urlListToScan
            List<String> filteredUrls = filterUrlsByKeywords(urlFromSiteTreeList, urlListToScan);
            System.out.println("Filtered URLs: " + filteredUrls);
            // Perform active scan on each filtered URL
            for (String url : filteredUrls) {
                System.out.println("Starting Active Scan on: " + url);
                this.activeScan(url);
            }
        } else {
            System.out.println("Active scan is not enabled.");
        }

        System.out.println("All scans completed.");
    }

    /**
     * Filters URLs based on the keywords provided in urlListToScan.
     */
    private List<String> filterUrlsByKeywords(List<String> urlsToScan, List<String> urlListToScan) {
        // Use a Set to automatically handle duplicates
        Set<String> filteredUrlsSet = new HashSet<>();

        // Iterate through each URL and check if it contains any of the keywords
        for (String url : urlsToScan) {
            for (String keyword : urlListToScan) {
                if (url.contains(keyword)) {
                    filteredUrlsSet.add(url);  // Add to Set to avoid duplicates
                    break;  // Once a keyword is matched, no need to check further for this URL
                }
            }
        }

        // Convert the Set back to a List to return
        return new ArrayList<>(filteredUrlsSet);
    }


    public void callZapRestAssured(int zapPort, Map<String, String> headers, String authType, String authValue) {
        ZapConfig config = new ZapConfig();
        String zapAddress = config.getProperty("zap.address");
        String zapApiKey = config.getProperty("zap.apikey");
        requestSpecification = RestAssured.given();
        requestSpecification.baseUri("http://" + zapAddress + ":" + zapPort + "/JSON");
        requestSpecification.queryParam("apikey", zapApiKey);
        // Add Headers
        if (headers != null) {
            requestSpecification.headers(headers);
        }
        // Add Authentication
        if ("Bearer".equalsIgnoreCase(authType) && authValue != null) {
            requestSpecification.header("Authorization", "Bearer " + authValue);
        } else if ("Basic".equalsIgnoreCase(authType) && authValue != null) {
            requestSpecification.header("Authorization", "Basic " + authValue);
        }
    }

    public void addApiUrlToScanTree (String site_to_test){
        requestSpecification.queryParam("url", site_to_test);
        response = requestSpecification.get("/core/action/accessUrl/");
        if (response.getStatusCode() == 200) {
            System.out.println("URL has been added to Scan tree");
        }
        else {
            System.err.println("Failed to add URL to the Scan tree. Response: " + response.getBody().asString());
        }
    }

    public void startApiActiveScan (String site_to_test){
        requestSpecification.queryParam("url", site_to_test);
        response = requestSpecification.get("/ascan/action/scan/");
        if (response.getStatusCode() == 200) {
            System.out.println("Active scan has started");
            waitForApiActiveScanCompletion();
        } else {
        System.err.println("Failed to start Active Scan. Response: " + response.getBody().asString());
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
