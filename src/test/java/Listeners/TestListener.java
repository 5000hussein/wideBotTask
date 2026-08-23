package Listeners;

import Util.ScreenshotUtil;
import io.qameta.allure.Allure;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

public class TestListener implements ITestListener {
    @Override
    public void onTestFailure(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        ScreenshotUtil.attachPageContext();
        ScreenshotUtil.captureFailure(testName);

        Throwable cause = result.getThrowable();
        if (cause != null) {
            Allure.addAttachment("Failure detail", "text/plain",
                    cause.getClass().getName() + System.lineSeparator() + cause.getMessage());
        }
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        Throwable cause = result.getThrowable();
        Allure.addAttachment("Skip reason", "text/plain",
                cause == null ? "Skipped by dependency or configuration failure" : String.valueOf(cause.getMessage()));
    }

    @Override
    public void onFinish(ITestContext context) {
        System.out.printf("%nSuite '%s' finished: %d passed, %d failed, %d skipped%n",
                context.getName(),
                context.getPassedTests().size(),
                context.getFailedTests().size(),
                context.getSkippedTests().size());
    }
}
