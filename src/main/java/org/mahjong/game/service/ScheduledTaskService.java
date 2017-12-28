package org.mahjong.game.service;

import org.joda.time.DateTime;
import org.mahjong.game.Constants;
import org.mahjong.game.domain.Room;
import org.mahjong.game.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.WebSocketSession;

import javax.inject.Inject;


@Component
@Service
@Configurable
@EnableScheduling
public class ScheduledTaskService {

    private final Logger log = LoggerFactory.getLogger(ScheduledTaskService.class);

    @Inject
    private GameService gameService;

    /**
     * 每50ms检测一下游戏是否开始
     */
    @Scheduled(fixedRate = 50)//单位：ms
    @Transactional
    @Async
    public void checkRoomStart() throws Exception {
        log.info("定时任务：检查游戏是否开始");
        A:
        for (Room room : Constants.roomMap.values()) {
            if (room.getPlayers().size() != 4 || room.isPlaying()) {//如果房间已经在游戏，就不能重复开始
                continue;
            }
            for (User user : room.getPlayers()) {
                if (!user.isReady()) {
                    continue A;
                }
            }
            //如果都准备了，那么就开始游戏
            gameService.gameStart(room);
        }
    }


    /**
     * 本函数用于检测碰牌和吃牌时候到时间
     * 每50ms检测定时器是否过期，如果过期，选出优先级最高的一个
     */
    @Scheduled(fixedRate = 50)//单位：ms
    @Transactional
    @Async
    public void selectPriorRequest() throws Exception {
        log.info("定时任务：检查下一轮是否开始");
        for (Room room : Constants.roomMap.values()) {
            //如果计时器为空，说明已经发完了牌，现在正在等到用户出牌请求
            //如果计时器不为空，说明已经出牌完，不管怎样都等待5秒后才进行下一轮的发牌处理
            if (!room.isPlaying()) {
                continue;
            }
            if (room.getTimerStart() == null) {
                continue;
            }
            if (room.getTimerStart().plusSeconds(Constants.PERIOD).isAfter(DateTime.now())) {
                continue;
            }
            gameService.nextRound(room);//排序后给玩家发牌
        }
    }
}
