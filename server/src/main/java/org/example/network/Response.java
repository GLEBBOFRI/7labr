
package org.example.network;
import java.io.Serializable;

public class Response implements Serializable {
    private static final long serialVersionUID = 82038482L;

    private final String message;
    private final Object data;

    public Response(String message) {
        this(message, null);
    }

    public Response(String message, Object data) {
        this.message = message;
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public Object getData() {
        return data;
    }

    @Override
    public String toString() {
        String dataString = data == null ? "null" : data.toString();
        return "Response{" +
                "message='" + message + '\'' +
                ", data=" + dataString +
                '}';
    }
}
