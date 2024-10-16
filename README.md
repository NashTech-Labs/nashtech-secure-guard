# Secure Guard
Secure Guard is a reusable library designed for integration with Selenium projects. It serves as a wrapper around the OWASP ZAP API, enabling seamless interaction with OWASP ZAP for security testing. This guide provides instructions on integrating Secure Guard into your Selenium project.

### Prerequisites
1. Maven
2. JDK 11 or higher

### Step 1 : Build the JAR File
To build the JAR file, follow these steps:

#### 1. Clone the Repository

Clone the repository to your local machine:

    git clone https://github.com/yourusername/secure-guard.git
    cd secure-guard

#### 2. Build the JAR

Run the following Maven command to build the JAR file:

    mvn clean package
The JAR file will be created in the target directory, named secure-guard-1.0.jar.

#### 3. Integrate the Secure Guard JAR into Your Selenium Project
##### Add the JAR File to Your Selenium Project

Copy the secure-guard-1.0.jar file from the target directory of your Secure Guard project into your Selenium project’s lib directory or any other location you use for external JARs.

##### Update Your Selenium Project’s pom.xml
Add the JAR file to the classpath by updating the pom.xml file of your Selenium project. If you are using a local JAR file, configure it as follows:

    <dependency>
        <groupId>org.example</groupId>
        <artifactId>secure-guard</artifactId>
        <version>1.0</version>
        <scope>system</scope>
        <systemPath>${project.basedir}/lib/secure-guard-1.0.jar</systemPath> <!-- Adjust path as needed -->
    </dependency>

#### 4. Use the Secure Guard Methods
#### a. Import and use the methods in your Selenium test code as shown below:

    import com.example.ZapScanner;
    import com.example.ZapManager;
    import com.example.ZapReport;
    import org.openqa.selenium.WebDriver;
    import org.openqa.selenium.chrome.ChromeDriver;
    
    public class SeleniumTest {
    public static void main(String[] args) throws IOException {
    // Initialize WebDriver
    WebDriver driver = new ChromeDriver();
    
            // Create instances for Secure Guard components
            ZapScanner scanner = new ZapScanner(8080); // Port number for OWASP ZAP
            ZapManager zapManager = new ZapManager();
            
            // Start OWASP ZAP in a Docker container
            zapManager.startZap(8080);
            
            // Perform login and add URLs to the scan tree
            String currentUrl = SignIn.loginToWebApp(driver); 
            scanner.addUrlScanTree(currentUrl);
            
            String anotherPageUrl = Navigate.anotherPage(driver);
            scanner.addUrlScanTree(anotherPageUrl);
            
            // Perform active scanning
            scanner.allScan(anotherPageUrl, true); // Set false if active scanning is not required
            
            // Generate a security report
            ZapReport report = new ZapReport(8080);
            report.generateReport("security-test-report.html", "Security Testing Report");
            
            // Stop OWASP ZAP container and quit WebDriver
            zapManager.stopZap();
            driver.quit();
        }
    }

#### b. Import and use the methods in your API code as shown below
    ZapScanner scanner = new ZapScanner(8080);
    ZapManager zapManager = new ZapManager();
    zapManager.startZap(8080);
    String currentUrl = "your_application_url";
    System.out.println("currentUrl : " + currentUrl);
    scanner.callZapRestAssured(8080);
    scanner.addApiUrlToScanTree(currentUrl);
    scanner.startApiActiveScan(currentUrl);
    ZapReport report = new ZapReport(8080);
    report.generateReport("api-test-report.html", "Security Testing Report");
    zapManager.stopZap();
