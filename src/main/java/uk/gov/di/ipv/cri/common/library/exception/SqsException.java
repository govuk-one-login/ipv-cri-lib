package uk.gov.di.ipv.cri.common.library.exception;

import uk.gov.di.ipv.cri.common.library.annotations.ExcludeFromGeneratedCoverageReport;

@ExcludeFromGeneratedCoverageReport
public class SqsException extends Exception {
    public SqsException(Throwable e) {
        super(e);
    }

    public SqsException(String errorMessage) {
        super(errorMessage);
    }
}
