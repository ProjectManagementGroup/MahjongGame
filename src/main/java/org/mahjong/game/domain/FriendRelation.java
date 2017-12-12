package org.mahjong.game.domain;

import javax.persistence.*;

/**
 * Created by zhaoyawen on 2017/5/16.
 */
@Entity
@Table(name = "FRIEND_RELATION")
public class FriendRelation {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;


    @ManyToOne
    @JoinColumn(name = "user1_id")
    private User webUser1;

    @ManyToOne
    @JoinColumn(name = "user2_id")
    private User webUser2;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getWebUser1() {
        return webUser1;
    }

    public void setWebUser1(User webUser1) {
        this.webUser1 = webUser1;
    }

    public User getWebUser2() {
        return webUser2;
    }

    public void setWebUser2(User webUser2) {
        this.webUser2 = webUser2;
    }
}
