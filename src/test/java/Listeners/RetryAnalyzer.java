package Listeners;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

public class RetryAnalyzer implements IRetryAnalyzer {
    private static final int MAX_RETRIES = 1;

    private int attempts = 0;

    @Override
    public boolean retry(ITestResult result) {
        if (attempts < MAX_RETRIES) {
            attempts++;
            System.out.printf("Retrying %s.%s (attempt %d of %d)%n",
                    result.getTestClass().getName(),
                    result.getMethod().getMethodName(),
                    attempts + 1,
                    MAX_RETRIES + 1);
            return true;
        }
        return false;
    }
}
