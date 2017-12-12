package org.mahjong.game.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.mahjong.game.domain.FriendRelation;
import org.mahjong.game.domain.User;

import java.util.List;
import java.util.Optional;

/**
 * Created by zhaoyawen on 2017/3/2.
 */
@Repository
public interface FriendRelationRepository extends JpaRepository<FriendRelation, Long> {

    @Query("select fr.webUser2.userName from FriendRelation fr where fr.webUser1.userName=:userName and flag = 0")
    List<String> findAllFriendsByUserName1(@Param("userName") String userName);

    @Query("select fr.webUser1.userName from FriendRelation fr where fr.webUser2.userName=:userName and flag = 0")
    List<String> findAllFriendsByUserName2(@Param("userName") String userName);

    @Query("select fr from FriendRelation fr where fr.webUser2.userName=:userName and flag = 1")
    List<FriendRelation> findAllAppliesByUserName(@Param("userName") String userName);

    @Query("select fr.webUser1 from FriendRelation fr where fr.webUser2.userName = :userName")
    Optional<User> findOneByFriendName1(@Param("userName") String userName);

    @Query("select fr.webUser2 from FriendRelation fr where fr.webUser1.userName = :userName")
    Optional<User> findOneByFriendName2(@Param("userName") String userName);

    @Query("select fr from FriendRelation fr where (fr.webUser1.userName = :userName1 and fr.webUser2.userName = :userName2) or (fr.webUser2.userName = :userName1 and fr.webUser1.userName = :userName2)")
    Optional<FriendRelation> findOneByWebUser1AndWebUser2(@Param("userName1") String userName1, @Param("userName2") String userName2);


    @Query("select fr from FriendRelation fr where fr.webUser1.userName = :userName1 and fr.webUser2.userName = :userName2 and fr.flag=:flag")
    Optional<FriendRelation> findOneByWebUser1AndWebUser2AndFlag(@Param("userName1") String userName1, @Param("userName2") String userName2, @Param("flag") int flag);


    @Query("select fr from FriendRelation fr where (fr.webUser1.userName = :userName1 and fr.webUser2.userName = :userName2) or (fr.webUser2.userName = :userName1 and fr.webUser1.userName = :userName2)")
    List<FriendRelation> findAllApplies(@Param("userName1") String userName1, @Param("userName2") String userName2);

}
