package org.mahjong.game.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import org.mahjong.game.domain.WebUser;

import java.util.Optional;

/**
 * Created by zhaoyawen on 2017/3/2.
 */
@Repository
public interface WebUserRepository extends JpaRepository<WebUser, Long>, JpaSpecificationExecutor<WebUser> {

    Optional<WebUser> findOneByUserName(String userName);

}
