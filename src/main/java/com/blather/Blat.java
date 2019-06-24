package com.blather;

public class Blat {
    private String blatId;
    private String message;
    private String creator;
    private Long timestamp;

    public String getBlatId() {
        return blatId;
    }

    public void setBlatId(String b) {
        blatId = b;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String m) {
        message = m;
    }

    public void setCreator(String c) {
        creator = c;
    }

    public String getCreator() {
        return creator;
    }

    public void setTimestamp(Long t) {
        timestamp = t;
    }

    public Long getTimestamp() {
        return timestamp;
    }
}
