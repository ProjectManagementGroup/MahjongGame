package org.mahjong.game.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import org.mahjong.game.domain.User;

import java.util.Optional;

/**
 * Created by zhaoyawen on 2017/3/2.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findOneByName(String name);

}
