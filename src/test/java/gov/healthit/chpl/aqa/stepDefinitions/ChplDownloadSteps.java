package gov.healthit.chpl.aqa.stepDefinitions;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.events.AbstractWebDriverEventListener;
import org.openqa.selenium.support.events.EventFiringWebDriver;
import org.openqa.selenium.support.events.WebDriverEventListener;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

import cucumber.api.java.After;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import gov.healthit.chpl.aqa.pageObjects.ChplDownloadPage;

/**
 * Class ChplDownloadSteps definition.
 */
public class ChplDownloadSteps extends BaseSteps {

    private EventFiringWebDriver driver;
    private String downloadPath = System.getProperty("downloadPath");
    private File dir;
    private static final long MILLIS_IN_A_DAY = 1000 * 60 * 60 * 24;
    private static final long BYTES_PER_KILOBYTE = 1024;
    private static final double FILE_TOO_LARGE_FACTOR = 1.1;

    /**
     * Constructor creates new driver.
     * Create a sub directory under default temp directory to set downloadPath
     * Print path to get download file location
     */
    public ChplDownloadSteps() {
        super();
        driver = Hooks.getDriver();
        if (StringUtils.isEmpty(downloadPath)) {
            String tempDirectory;
            try {
                tempDirectory = Files.createTempDirectory("download-files").toString();
                // Print the path to the newly created directory
            } catch (final IOException e) {
                // If temp directory creation failed, create new directory in target folder
                // user.dir - User working directory, make new directories in user's working directory
                File file = new File("target", "download-files");
                file.mkdirs();
                tempDirectory = System.getProperty("user.dir") + File.separator + "target" + File.separator + "download-files";
            }
            downloadPath = tempDirectory;
        }
        dir = new File(downloadPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    /**
     * Close browser windows and terminate WebDriver session.
     */
    @After
    public void afterMethod() {
        driver.quit();
    }

    /**
     * Get user to the Download CHPL page. Chrome options are necessary to get past
     * keep/discard pop ups for successful download of a file to directory.
     * @throws Throwable throws exception if there is an issue with Chrome options.
     * @param tEnv test environment in which tests will be run
     */
    @Given("^I am on download the CHPL resources page on \"([^\"]*)\"$")
    public void iAmOnDownloadTheCHPLResourcesPage(final String tEnv) throws Throwable {
        HashMap<String, Object> chromePrefs = new HashMap<String, Object>();
        chromePrefs.put("profile.default_content_settings.popups", 0);
        chromePrefs.put("download.default_directory", downloadPath);
        /**
         * Save Chrome Options
         */
        ChromeOptions options = new ChromeOptions();
        HashMap<String, Object> chromeOptionsMap = new HashMap<String, Object>();
        options.setExperimentalOption("prefs", chromePrefs);
        chromePrefs.put("safebrowsing.enabled", "true");
        options.addArguments("--safebrowsing-disable-download-protection");
        options.addArguments("disable-popup-blocking");

        DesiredCapabilities cap = DesiredCapabilities.chrome();
        cap.setCapability(ChromeOptions.CAPABILITY, chromeOptionsMap);
        cap.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);
        cap.setCapability(ChromeOptions.CAPABILITY, options);

        String url;
        driver = new EventFiringWebDriver(new ChromeDriver(cap));
        WebDriverEventListener errorListener = new AbstractWebDriverEventListener() {
            @Override
            public void onException(final Throwable throwable, final WebDriver activeDriver) {
                try {
                    Hooks.takeScreenshot();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        driver.register(errorListener);
        if (tEnv.equalsIgnoreCase("DEV")) {
            url = "https://chpl.ahrqdev.org";
        } else if (tEnv.equalsIgnoreCase("STG")) {
            url = "https://chpl.ahrqstg.org";
        } else if (tEnv.equalsIgnoreCase("PROD")) {
            url = "https://chpl.healthit.gov";
        } else {
            url = "http://localhost:3000";
        }
        driver.get(url + "#/resources/download");
        getShortWait().until(ExpectedConditions.visibilityOf(ChplDownloadPage.downloadSelectList(driver)));
    }

    /**
     * Activates an item in the download file box.
     */
    @When("^user selects a file in download file box")
    public void userSelectsAFileInDownloadFileBox() {
        WebElement selectBox = ChplDownloadPage.downloadSelectList(driver);
        Select dropdown = new Select(selectBox);
        dropdown.selectByVisibleText("2015 edition products (xml)");
    }

    /**
     * Assert that correct definition file shows.
     */
    @Then("^definition file shows based on download file selection$")
    public void definitionFileShowsBasedOnSelection() {
        String definition = new Select(ChplDownloadPage.definitionSelectList(driver)).getFirstSelectedOption().getText();
        assertTrue(definition.contains("2015 edition products (xml) Definition File"));
    }

    /**
     * Assert that correct number of download files exist.
     * @param expectedLength the expected number of files to find
     */
    @Then("^user sees \"([^\"]*)\" download files")
    public void userSeesDownloadFiles(final String expectedLength) {
        WebElement selectElement = ChplDownloadPage.downloadSelectList(driver);
        Select listBox = new Select(selectElement);
        assertEquals(listBox.getOptions().size(), Integer.parseInt(expectedLength));
    }

    /**
     * Download one of the five product files. Waits to exit until file has been downloaded.
     * @param edition which edition to download
     * @param type whether to get the xml or csv file
     * @throws InterruptedException if thread.sleep is interrupted
     */
    @When("^I download the \"(.*)\" \"(.*)\" products file$")
    public void downloadProductFile(final String edition, final String type) throws InterruptedException {
        switch (edition) {
        case "2011":
            ChplDownloadPage.downloadoption2011editionProductsFile(driver).click();
            break;
        case "2014":
            switch (type) {
            case "csv":
                ChplDownloadPage.downloadoption2014summaryFile(driver).click();
                break;
            case "xml":
                ChplDownloadPage.downloadoption2014editionProductsFile(driver).click();
                break;
            default:
                break;
            }
            break;
        case "2015":
            switch (type) {
            case "csv":
                ChplDownloadPage.downloadoption2015summaryFile(driver).click();
                break;
            case "xml":
                ChplDownloadPage.downloadoption2015editionProductsFile(driver).click();
                break;
            default:
                break;
            }
            break;
        default:
            break;
        }
        ChplDownloadPage.downloadFileButton(driver).click();

        String downloadFileName = null;
        boolean fileFound = false;
        final long sleepTime = 5 * 1000;

        while (!fileFound) {
            File[] files = dir.listFiles();

            for (File file : files) {
                downloadFileName = file.getName();
                if (downloadFileName
                        .replaceAll("(^.........).*(....$)", "$1" + "$2")
                        .equalsIgnoreCase("chpl-" + edition + "." + type)) {
                    fileFound = true;
                }
                Thread.sleep(sleepTime);
            }
        }
    }

    /**
     * Assert that file is not older than expected value. Reads file in download directory
     * then parses filename's date field and compares with "today".
     * @param days maximum number of days old the file may be
     */
    @Then("^the downloaded file is no more than \"(.*)\" days old")
    public void theDownloadedFileIsNotOld(final String days) {
        final int startOfDateInFilename = 10;
        final int endOfDateInFilename = 18;
        String downloadFileName = null;

        File[] files = dir.listFiles();

        for (File file : files) {
            downloadFileName = file.getName();
        }
        String downloadFileDate = downloadFileName.substring(startOfDateInFilename, endOfDateInFilename);
        try {
            Date fileDate = new SimpleDateFormat("yyyyMMdd").parse(downloadFileDate);
            Date currentDate = new Date();
            int numDays = Integer.parseInt(days);
            double age = Math.ceil((currentDate.getTime() - fileDate.getTime()) / MILLIS_IN_A_DAY);
            assertTrue(age <= numDays,
                    "File " + downloadFileName + " is " + age + " days old, should be no more than " + numDays);
        } catch (ParseException e) {
            fail("Could not parse filename: " + downloadFileName + "'s date");
        }
    }

    /**
     * Assert that the file is at least the expected size. Will also give an error if the actual size is more than
     * 10% larger than the minimum size, in order to encourage the file size values to get updated as the files grow.
     * @param sizeParam expected size
     * @param units expected file unit scale (MB or KB)
     */
    @And("^the downloaded file is at least \"(.*)\" \"(.*)\" in size$")
    public void theDownloadedFileIsNotTooSmall(final String sizeParam, final String units) {
        Long size = Long.valueOf(sizeParam);
        File[] files = dir.listFiles();
        for (File f : files) {
            Long rawSize = f.length();
            Long fileSize;
            switch (units) {
            case "MB": fileSize = rawSize / BYTES_PER_KILOBYTE / BYTES_PER_KILOBYTE;
            break;
            case "KB": fileSize = rawSize / BYTES_PER_KILOBYTE;
            break;
            default:
                fileSize = rawSize;
            }
            String fileName = f.getName();
            assertTrue(fileSize >= size,
                    "File " + fileName + " is " + fileSize + " " + units + " in size; should be at least " + size + " " + units);
            assertTrue(fileSize <= (size * FILE_TOO_LARGE_FACTOR),
                    "File " + fileName + " is " + fileSize + " " + units + " in size; should be no more than "
                            + size * FILE_TOO_LARGE_FACTOR + " " + units + ". Please update the AQA test values.");
        }
    }
    /**
     * Select Surveillance Activity file from drop down and download .csv file.
     */
    @When("^I download the Surveillance Activity file$")
    public void downloadSurveillanceActivityFile() {
        ChplDownloadPage.downloadoptionSurveillanceFile(driver).click();
        ChplDownloadPage.downloadFileButton(driver).click();
    }

    /**
     * Assert filename of download file.
     */
    @Then("^the downloaded file shows surveillance-all.csv filename$")
    public void verifySurveillanceActivityFilename() {

        File[] filenames = dir.listFiles();

        for (File file : filenames) {
            String dwldFileName = file.getName();
            String currentfile = "surveillance-all.csv";
            assertEquals(dwldFileName, currentfile, "File is not current");
        }
    }

    /**
     * Select surveillance-with-nonconformities file from drop down and download .csv file.
     */
    @When("^I download the Non-Conformities file$")
    public void downloadNonConformitiesFile() {
        ChplDownloadPage.downloadoptionNonconformitiesFile(driver).click();
        ChplDownloadPage.downloadFileButton(driver).click();
    }

    /**
     * Assert filename of download file.
     */
    @Then("^the downloaded file shows surveillance-with-nonconformities.csv filename$")
    public void verifyNonConformitiesFilename() {

        File[] filenames = dir.listFiles();

        for (File file : filenames) {
            String dwldFileName = file.getName();
            String currentfile = "surveillance-with-nonconformities.csv";
            assertEquals(dwldFileName, currentfile, "File is not current");
        }
    }

    /**
     * Print filenames if directory is not empty.
     * Assert that download directory is empty
     * @throws IOException if unable to clean the directory
     */
    @And("^the download directory is empty$")
    public void checkifDownloadDirectoryisEmpty() throws IOException {
        System.out.println("Clearing directory: " + dir.getAbsolutePath());
        for (String fileName : dir.list()) {
            System.out.println("Removing: " + fileName);
        }
        FileUtils.cleanDirectory(dir);
        assertFalse(dir.list().length > 0, "directory is not empty");
    }
}
