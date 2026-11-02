package dev.kien.shopcart.error;

public class ResourceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " was not found: " + id);
    }
}
