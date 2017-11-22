package org.mahjong.game.service;


import org.apache.commons.codec.binary.Base64;
import org.jadira.usertype.spi.utils.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

/**
 * Created by zhaoyawen on 2017/5/17.
 */
@Service
@Transactional
public class FileService {
    private final Logger log = LoggerFactory.getLogger(FileService.class);


    public static String encodeBase64Str(String plainText) {
        String encodeBase64Str = null;
        if (StringUtils.isNotEmpty(plainText)) {
            byte[] btyeArrayStr = plainText.getBytes();
            Base64 base64 = new Base64();
            btyeArrayStr = base64.encode(btyeArrayStr);
            try {
                encodeBase64Str = new String(btyeArrayStr, "UTF-8");
            } catch (Exception e) {
                e.printStackTrace();
                //log.error("ERROR IN  HotBuyServiceImpl ===> encodeBase64Str method ", e);
            }
        }
        return encodeBase64Str;
    }

    public static String decodeBase64Str(String encryptText) {
        String decodeBase64Str = null;
        if (StringUtils.isNotEmpty(encryptText)) {
            byte[] btyeArrayStr = encryptText.getBytes();
            Base64 base64 = new Base64();
            btyeArrayStr = base64.decode(btyeArrayStr);
            try {
                decodeBase64Str = new String(btyeArrayStr, "UTF-8");
            } catch (Exception e) {
                e.printStackTrace();
                //log.error("ERROR IN  HotBuyServiceImpl ===> encodeBase64Str method ", e);
            }
        }
        return decodeBase64Str;
    }

    public static void main(String args[]) {
        String encryptText = encodeBase64Str("我爱熊猫");
        System.out.println(encryptText);
        System.out.println("------------------------");
        System.out.println(decodeBase64Str(encryptText));
    }
}
