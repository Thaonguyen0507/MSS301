package fu.de190669.user.common;

public class ApiException extends RuntimeException {
    private final int httpStatus;
    private final int status;

    public ApiException(int httpStatus, int status, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.status = status;
    }

    public int getHttpStatus() { return httpStatus; }
    public int getStatus() { return status; }
}
