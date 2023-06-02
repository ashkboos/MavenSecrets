package nl.tudelft.mavensecrets;

public class PackageException extends Exception {

    private static final long serialVersionUID = -3351058050694810249L;

    private final PackageId id;
    private final String error;

    public PackageException(PackageId id, String message) {
        this(id, message, null);
    }

    public PackageException(PackageId id, String message, Exception inner) {
        super(message + " (package: " + id + ")", inner);
        this.id = id;
        this.error = message;
    }

    public PackageId getPackageId() {
        return id;
    }

    public String getError() {
        return error;
    }
}
