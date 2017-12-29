package org.mahjong.game.repository;

import org.mahjong.game.domain.FriendRelation;
import org.mahjong.game.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Created by zhaoyawen on 2017/3/2.
 */
@Repository
public interface FriendRelationRepository extends JpaRepository<FriendRelation, Long>, JpaSpecificationExecutor<FriendRelation> {

    @Query("select from ")
    List<User> findAllByUseId(@Param("uid") Long uid);

}
