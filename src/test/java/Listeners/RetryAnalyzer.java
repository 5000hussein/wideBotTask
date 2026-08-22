package Listeners;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * Retries a failed test once.
 *
 * A deliberately small allowance, aimed at the shared public demo environment:
 * it drops out under load often enough that a single network blip would
 * otherwise be reported as a product defect. Capped at one retry on purpose --
 * a higher number hides real flakiness, which is worse than a red result.
 *
 * IMPORTANT -- this is applied per test, NOT suite-wide, and only to tests that
 * do not create data. Attaching it to everything was tried and actively made
 * things worse: re-running a test that had already created an employee hit the
 * duplicate Employee Id validation, so the form never navigated and a transient
 * blip turned into a confusing second failure with an unrelated message. A
 * retry is only safe where re-running the test is genuinely idempotent.
 */
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
