package org.mahjong.game;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class BootstrapService implements InitializingBean {
    private static final Logger log = LoggerFactory.getLogger(BootstrapService.class);

    @Override
    public void afterPropertiesSet() throws Exception {
        initMahjongTiles();
    }

    public void initMahjongTiles() {
        List<Constants.MahjongTile> list = Constants.getMahjongTiles();
        for (Constants.MahjongTile mahjongTile : Constants.MahjongTile.values()) {
            for (int i = 0; i < 4; i++) {
                list.add(mahjongTile);//每张麻将都加四次
            }
        }
    }
}
