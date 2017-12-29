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

    @Query("select fr.user2 from FriendRelation fr where fr.user1.id = :uid")
    List<User> findAllByUseId1(@Param("uid") Long uid);

    @Query("select fr.user1 from FriendRelation fr where fr.user2.id = :uid")
    List<User> findAllByUseId2(@Param("uid") Long uid);

    @Query("select fr.id from FriendRelation fr where (fr.user1.id = :uid1 and fr.user2.id = :uid2) or (fr.user2.id = :uid1 and fr.user1.id = :uid2)")
    Long findRelationByUser1AndUser2(@Param("uid1") Long uid1, @Param("uid2") Long uid2);
}
