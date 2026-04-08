package cn.dolphinmind.glossary.java.analyze;

import cn.dolphinmind.glossary.java.analyze.map.FileUtil;
import cn.dolphinmind.glossary.java.analyze.map.HashCode;
import cn.dolphinmind.glossary.java.analyze.map.RateInfo;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

public class ApiTest {

    private Set<String> words;

    @Before
    public void before() {
        "abc".hashCode();
        // 读取文件，103976个英语单词库.txt
        words = FileUtil.readWordList("/Users/mingxilv/WebDevelopment/gitcode/dev-proj/s-pay-mall/s-pay-mall-ddd/source-proj/glossary-redisson/src/main/resources/103976个英语单词库.txt");
    }

    @Test
    public void test_collisionRate() {
        System.out.println("单词数量：" + words.size());
        List<RateInfo> rateInfoList = HashCode.collisionRateList(words, 2, 3, 5, 7, 17, 31, 32, 33, 39, 41, 199);
        for (RateInfo rate : rateInfoList) {
            System.out.println(String.format("乘数 = %4d, 最小Hash = %11d, 最大Hash = %10d, 碰撞数量 =%6d, 碰撞概率 = %.4f%%", rate.getMultiplier(), rate.getMinHash(), rate.getMaxHash(), rate.getCollisionCount(), rate.getCollisionRate() * 100));
        }
    }

    @Test
    public void test_hashArea() {
        System.out.println(HashCode.hashArea(words, 2).values());
        System.out.println(HashCode.hashArea(words, 7).values());
        System.out.println(HashCode.hashArea(words, 31).values());
        System.out.println(HashCode.hashArea(words, 32).values());
        System.out.println(HashCode.hashArea(words, 199).values());
    }

}
