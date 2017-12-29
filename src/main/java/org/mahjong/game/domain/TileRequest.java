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

    /**
     * 两位两张牌都是什么
     */
    private Constants.MahjongTile tile1;

    /**
     * 两位两张牌都是什么
     */
    private Constants.MahjongTile tile2;

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

    public Constants.MahjongTile getTile1() {
        return tile1;
    }

    public void setTile1(Constants.MahjongTile tile1) {
        this.tile1 = tile1;
    }

    public Constants.MahjongTile getTile2() {
        return tile2;
    }

    public void setTile2(Constants.MahjongTile tile2) {
        this.tile2 = tile2;
    }
}
