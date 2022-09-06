package uk.gov.di.ipv.cri.common.library.validation;

public class ValidationResult<T> {
    private final boolean valid;
    private final T error;

    public ValidationResult(boolean valid, T error) {
        this.valid = valid;
        this.error = error;
    }

    public boolean isValid() {
        return valid;
    }

    public T getError() {
        return error;
    }

    public static <U> ValidationResult<U> createValidResult() {
        return new ValidationResult<>(true, null);
    }
}
