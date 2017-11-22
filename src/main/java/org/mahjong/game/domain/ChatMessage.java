package org.mahjong.game.domain;

import javax.persistence.*;

/**
 * Created by zhaoyawen on 2017/5/16.
 */
@Entity
@Table(name = "CHAT_MESSAGE")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    //同时也是时间排序的依据，就不存时间了
    private Long id;


    @ManyToOne
    @JoinColumn(name = "user1_id")
    //永远是发送者
    private WebUser webUser1;

    @ManyToOne
    @JoinColumn(name = "user2_id")
    private WebUser webUser2;

    private String message;


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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
