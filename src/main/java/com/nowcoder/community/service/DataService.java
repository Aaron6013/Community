package com.nowcoder.community.service;

import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @author wensheng
 * @create 2022-03-09 2:54 下午
 **/
@Service
public class DataService {

    @Autowired
    private RedisTemplate redisTemplate;

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");

    //将ip计入uv
    public void recordUV(String ip){
        String uvKey = RedisKeyUtil.getUVKey(simpleDateFormat.format(new Date()));
        redisTemplate.opsForHyperLogLog().add(uvKey,ip);
    }

    //统计指定日期范围内的uv
    public Long calculateUV(Date start, Date end){
        if(start == null || end == null){
            throw new RuntimeException("参数不能为空");
        }
        //整体日期范围内的key
        List<String> keyList = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);
        while (!calendar.getTime().after(end)){
            String uvKey = RedisKeyUtil.getUVKey(simpleDateFormat.format(calendar.getTime()));
            keyList.add(uvKey);
            calendar.add(Calendar.DATE,1); //日期加一天
        }
        //合并数据
        String uvKey = RedisKeyUtil.getUVKey(simpleDateFormat.format(start), simpleDateFormat.format(end));
        redisTemplate.opsForHyperLogLog().union(uvKey,keyList.toArray()); //将keyList.toArray()中的key的值合并后在存入uvKey这个新key

        //返回统计结果
        return redisTemplate.opsForHyperLogLog().size(uvKey);
    }

    //将指定用户计入DAU
    public void recordDAU(Integer userId){
        String dauKey = RedisKeyUtil.getDAUKey(simpleDateFormat.format(new Date()));
        redisTemplate.opsForValue().setBit(dauKey,userId,true);
    }
    //统计日期范围内的dau
    public Long calculateDAU(Date start, Date end){
        if(start == null || end == null){
            throw new RuntimeException("参数不能为空");
        }
        //整体日期范围内的key
        List<byte[]> keyList = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);
        while (!calendar.getTime().after(end)){
            String uvKey = RedisKeyUtil.getDAUKey(simpleDateFormat.format(calendar.getTime()));
            keyList.add(uvKey.getBytes());
            calendar.add(Calendar.DATE,1);
        }
        //合并数据
        return (long)redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                String dauKey = RedisKeyUtil.getDAUKey(simpleDateFormat.format(start), simpleDateFormat.format(end));
                connection.bitOp(RedisStringCommands.BitOperation.OR,dauKey.getBytes(),keyList.toArray(new byte[0][0]));
                return connection.bitCount(dauKey.getBytes());
            }
        });
    }

}
