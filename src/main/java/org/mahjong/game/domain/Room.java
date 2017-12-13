package org.mahjong.game.domain;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.joda.time.DateTime;
import org.mahjong.game.Constants;

import java.util.List;
import java.util.Set;

/**
 * 不存在数据库里面，因为会频繁读取
 */
public class Room {

    private Long id;

    /**
     * 是否是好友房间
     */
    private boolean friendly;

    /**
     * 是否正在游戏
     */
    private boolean playing = false;

    /**
     * 需要知道当前最后一张发出去的牌是什么
     */
    private Constants.MahjongTile lastTile;

    /**
     * 某个玩家出牌后，系统要有定时器
     */
    private DateTime timerStart;


    /**
     * 房间内的玩家
     * 这个顺序也是玩家抓牌的顺序
     */
    private List<User> players = Lists.newLinkedList();

    private List<TileRequest> requests = Lists.newLinkedList();

    /**
     * 不做循环队列了，直接判断当前抓牌和出牌的index
     */
    private int index = 0;

    /**
     * 每次游戏开始，都重置麻将牌
     */
    private List<Constants.MahjongTile> mahjongTiles;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<User> getPlayers() {
        return players;
    }

    public void setPlayers(List<User> players) {
        this.players = players;
    }

    public List<Constants.MahjongTile> getMahjongTiles() {
        return mahjongTiles;
    }

    public void setMahjongTiles(List<Constants.MahjongTile> mahjongTiles) {
        this.mahjongTiles = mahjongTiles;
    }

    public boolean isFriendly() {
        return friendly;
    }

    public void setFriendly(boolean friendly) {
        this.friendly = friendly;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Room room = (Room) o;

        return id != null ? id.equals(room.id) : room.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    public Constants.MahjongTile getLastTile() {
        return lastTile;
    }

    public void setLastTile(Constants.MahjongTile lastTile) {
        this.lastTile = lastTile;
    }

    public DateTime getTimerStart() {
        return timerStart;
    }

    public void setTimerStart(DateTime timerStart) {
        this.timerStart = timerStart;
    }

    public boolean isPlaying() {
        return playing;
    }

    public void setPlaying(boolean playing) {
        this.playing = playing;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public List<TileRequest> getRequests() {
        return requests;
    }

    public void setRequests(List<TileRequest> requests) {
        this.requests = requests;
    }

    public int getPlayerIndex(String username) {
        for (int i = 0; i < 4; i++) {
            if (username.equals(players.get(i).getUserName())) {
                return i;
            }
        }
        return -1;
    }
}
