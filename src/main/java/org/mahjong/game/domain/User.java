package org.mahjong.game.domain;

import jdk.nashorn.internal.ir.annotations.Ignore;
import org.mahjong.game.Constants;

import javax.persistence.*;
import java.util.List;

/**
 * Created by zhaoyawen on 2017/5/16.
 */
@Entity
@Table(name = "USER")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userName;

    private String password;

    /**
     * 积分
     */
    private Integer point;

    /**
     * 玩家手牌，游戏开始前清空
     */
    @Transient
    private List<Constants.MahjongTile> ownTiles;

    /**
     * 玩家出过的牌，游戏开始前清空
     */
    @Transient
    private List<Constants.MahjongTile> thrownTiles;

    /**
     * 用户所属房间，可以为空
     */
    @Transient
    private Room room;

    /**
     * 玩家是否准备,游戏结束后都清空
     */
    @Transient
    private boolean ready;

    public Long getId() {
        return id;
    }

    public String getPassword() {
        return password;
    }


    public void setPassword(String password) {
        this.password = password;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Integer getPoint() {
        return point;
    }

    public void setPoint(Integer point) {
        this.point = point;
    }

    public List<Constants.MahjongTile> getOwnTiles() {
        return ownTiles;
    }

    public void setOwnTiles(List<Constants.MahjongTile> ownTiles) {
        this.ownTiles = ownTiles;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public List<Constants.MahjongTile> getThrownTiles() {
        return thrownTiles;
    }

    public void setThrownTiles(List<Constants.MahjongTile> thrownTiles) {
        this.thrownTiles = thrownTiles;
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;

        if (!id.equals(user.id)) return false;
        if (!userName.equals(user.userName)) return false;
        return password.equals(user.password);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + userName.hashCode();
        result = 31 * result + password.hashCode();
        return result;
    }

    public String getInfo() {
        return "{\"username\": \"" + userName + "\", \"point\": " + point + ", \"ready\": " + ready + "}";
    }
}
