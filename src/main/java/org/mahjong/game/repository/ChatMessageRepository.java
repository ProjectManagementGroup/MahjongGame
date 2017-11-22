package org.mahjong.game.repository;

import org.mahjong.game.domain.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by zhaoyawen on 2017/3/2.
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {


    @Query("select cm from ChatMessage cm where (cm.webUser1.userName = :webUser1 and cm.webUser2.userName = :webUser2) or (cm.webUser2.userName = :webUser1 and cm.webUser1.userName = :webUser2) order by cm.id")
    List<ChatMessage> findAllByWebUser1AndWebUser2(@Param("webUser1") String webUser1,@Param("webUser2")String webUser2);



}
