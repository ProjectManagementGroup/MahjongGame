package org.mahjong.game.domain;

import javax.persistence.*;

/**
 * Created by zhaoyawen on 2017/5/16.
 */
@Entity
@Table(name = "USER")
public class WebUser {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;


    private String userName;
    private String password;

    @Column(length = 500)
    private String publicKeyGetEncoded;

    public Long getId() {
        return id;
    }

    public String getPassword() {
        return password;
    }

    public String getPublicKeyGetEncoded() {
        return publicKeyGetEncoded;
    }

    public void setPublicKeyGetEncoded(String publicKeyGetEncoded) {
        this.publicKeyGetEncoded = publicKeyGetEncoded;
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
}
