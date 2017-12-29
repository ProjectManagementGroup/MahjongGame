package org.mahjong.game.domain;


import javax.persistence.*;
import javax.validation.constraints.NotNull;

/**
 * 好友关系
 * user1和user2地位平等
 */
@Entity
@Table(name = "FRIEND_RELATION")
public class FriendRelation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "user1_id")
    private User user1;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "user2_id")
    private User user2;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser1() {
        return user1;
    }

    public void setUser1(User user1) {
        this.user1 = user1;
    }

    public User getUser2() {
        return user2;
    }

    public void setUser2(User user2) {
        this.user2 = user2;
    }
}
