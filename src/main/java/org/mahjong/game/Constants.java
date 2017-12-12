package org.mahjong.game;


import com.google.common.collect.Lists;

import java.util.List;

public class Constants {

    public static final String SPRING_PROFILE_DEVELOPMENT = "dev";
    public static final String SPRING_PROFILE_PRODUCTION = "prod";

    private static List<MahjongTile> mahjongTiles = Lists.newLinkedList();

    public static List<MahjongTile> getMahjongTiles() {
        return mahjongTiles;
    }

    public static void setMahjongTiles(List<MahjongTile> mahjongTiles) {
        Constants.mahjongTiles = mahjongTiles;
    }

    public static final int PERIOD = 5;//秒，为定时器时长

    /**
     * 碰/吃
     */
    public enum RequestType {
        bump("碰", 1),
        eat("吃", 0);

        private String chineseName;
        private int rank;

        RequestType(String chineseName, int rank) {
            this.chineseName = chineseName;
            this.rank = rank;
        }

        public String getChineseName() {
            return chineseName;
        }

        public int getRank() {
            return rank;
        }

        @Override
        public String toString() {
            return this.chineseName;
        }

    }

    /**
     * 麻将花色
     */
    public enum MahjongType {
        dot("饼(筒)"),
        wind("东西南北风"),
        bamboo("条"),
        myriad("万"),
        dragon("中发白");

        private String chineseName;

        MahjongType(String chineseName) {
            this.chineseName = chineseName;
        }

        public String getChineseName() {
            return chineseName;
        }

        @Override
        public String toString() {
            return this.chineseName;
        }

    }

    /**
     * 麻将牌
     */
    public enum MahjongTile {
        oneDot("一饼", MahjongType.dot, 1),
        twoDot("二饼", MahjongType.dot, 2),
        threeDot("三饼", MahjongType.dot, 3),
        fourDot("四饼", MahjongType.dot, 4),
        fiveDot("五饼", MahjongType.dot, 5),
        sixDot("六饼", MahjongType.dot, 6),
        sevenDot("七饼", MahjongType.dot, 7),
        eightDot("八饼", MahjongType.dot, 8),
        nineDot("九饼", MahjongType.dot, 9),

        oneMyriad("一万", MahjongType.myriad, 1),
        twoMyriad("二万", MahjongType.myriad, 2),
        threeMyriad("三万", MahjongType.myriad, 3),
        fourMyriad("四万", MahjongType.myriad, 4),
        fiveMyriad("五万", MahjongType.myriad, 5),
        sixMyriad("六万", MahjongType.myriad, 6),
        sevenMyriad("七万", MahjongType.myriad, 7),
        eightMyriad("八万", MahjongType.myriad, 8),
        nineMyriad("九万", MahjongType.myriad, 9),

        oneBamboo("一条", MahjongType.bamboo, 1),
        twoBamboo("二条", MahjongType.bamboo, 2),
        threeBamboo("三条", MahjongType.bamboo, 3),
        fourBamboo("四条", MahjongType.bamboo, 4),
        fiveBamboo("五条", MahjongType.bamboo, 5),
        sixBamboo("六条", MahjongType.bamboo, 6),
        sevenBamboo("七条", MahjongType.bamboo, 7),
        eightBamboo("八条", MahjongType.bamboo, 8),
        nineBamboo("九条", MahjongType.bamboo, 9),


        eastWind("东风", MahjongType.wind, 1),
        westWind("西风", MahjongType.wind, 2),
        southWind("南风", MahjongType.wind, 3),
        northWind("北风", MahjongType.wind, 4),

        middle("中", MahjongType.dragon, 1),
        fortune("发", MahjongType.dragon, 2),
        white("白", MahjongType.dragon, 3);

        private String chineseName;
        private MahjongType type;
        private int number;


        MahjongTile(String chineseName, MahjongType type, int number) {
            this.chineseName = chineseName;
            this.type = type;
            this.number = number;
        }

        public String getChineseName() {
            return chineseName;
        }

        public MahjongType getType() {
            return type;
        }

        public int getNumber() {
            return number;
        }
    }


}
