package org.mahjong.game.domain;

import com.google.common.collect.Lists;
import org.mahjong.game.Constants;

import javax.persistence.*;
import java.util.List;
import java.util.Map;

/**
 * Created by zhaoyawen on 2017/5/16.
 */
@Entity
@Table(name = "USER")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private String password;

    /**
     * 积分
     */
    private Integer point;

    /**
     * 用户排序
     */
    @Transient
    private int index;

    /**
     * 玩家手牌，游戏开始前清空
     */
    @Transient
    private List<Constants.MahjongTile> ownTiles = Lists.newLinkedList();

    /**
     * 玩家出过的牌，游戏开始前清空
     */
    @Transient
    private List<Constants.MahjongTile> thrownTiles = Lists.newLinkedList();

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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;

        if (!id.equals(user.id)) return false;
        if (!username.equals(user.username)) return false;
        return password.equals(user.password);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + username.hashCode();
        result = 31 * result + password.hashCode();
        return result;
    }

}
