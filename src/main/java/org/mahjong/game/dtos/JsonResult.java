package org.mahjong.game.dtos;

public class JsonResult {

    /**
     * 用户请求是否成功
     */
    private boolean status = false;

    /**
     * 正确和或者错误提示
     */
    private String message;

    private String object;

    public JsonResult() {
    }

    public JsonResult(String message) {
        this.message = message;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    @Override
    public String toString() {
        return "{\"ok\": " + status + ", \"message\": \"" + message + "\", \"object\": " + object + "}";
    }
}
