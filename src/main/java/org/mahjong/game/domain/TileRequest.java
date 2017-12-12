package org.mahjong.game.domain;

import org.mahjong.game.Constants;

public class TileRequest {

    /**
     * 哪位用户的请求
     */
    private User user;

    /**
     * 碰/吃
     */
    private Constants.RequestType type;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Constants.RequestType getType() {
        return type;
    }

    public void setType(Constants.RequestType type) {
        this.type = type;
    }
}
