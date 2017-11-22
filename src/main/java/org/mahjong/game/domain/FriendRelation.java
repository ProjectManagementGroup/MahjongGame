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
    private WebUser webUser1;

    @ManyToOne
    @JoinColumn(name = "user2_id")
    private WebUser webUser2;


    //flag = 0表示二人是朋友关系
    //flag = 1表示user1向user2发请求，同意或者拒绝后结束后就删掉这条数据
    //数据库不保存同意或者拒绝，同意设置这个flag是0，拒绝就默默失败，不提示
    //发送请求的人总是user1，所以只需要查userName=webUser2.name的就可以知道自己收到的请求
    private int flag;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public WebUser getWebUser1() {
        return webUser1;
    }

    public void setWebUser1(WebUser webUser1) {
        this.webUser1 = webUser1;
    }

    public WebUser getWebUser2() {
        return webUser2;
    }

    public void setWebUser2(WebUser webUser2) {
        this.webUser2 = webUser2;
    }

    public int getFlag() {
        return flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }
}
