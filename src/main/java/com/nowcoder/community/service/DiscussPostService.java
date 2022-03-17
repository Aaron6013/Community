package com.nowcoder.community.service;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.util.SensitiveFilter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class DiscussPostService {

    @Value("${caffeine.posts.max-size}")
    private Integer maxSize;

    @Value("${caffeine.posts.max-size}")
    private Integer expireSeconds;

    //帖子列表缓存
    private LoadingCache<String,List<DiscussPost>> postListCache;

    //帖子总数缓存
    private LoadingCache<Integer,Integer> postRowCache;

    @PostConstruct
    public void init(){
        //初始化帖子列表缓存
        postListCache = Caffeine.newBuilder().maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.MICROSECONDS)
                .build(new CacheLoader<String, List<DiscussPost>>() {
                    @Override //缓存没有时，查询数据库
                    public @Nullable List<DiscussPost> load(String key) throws Exception {
                        if(key == null || key.length() == 0){
                            throw  new IllegalArgumentException("参数错误");
                        }
                        String[] params = key.split(":");
                        if(params == null || params.length != 2){
                            throw  new IllegalArgumentException("参数错误");
                        }
                        int offset = Integer.valueOf(params[0]);
                        int limit = Integer.valueOf(params[1]);
                        System.out.println("查询数据库");
                        return discussPostMapper.selectDiscussPosts(0,offset,limit,1);
                    }
                });
        //初始化帖子总数缓存
        postRowCache = Caffeine.newBuilder().maximumSize(maxSize)
                .expireAfterWrite(expireSeconds,TimeUnit.SECONDS)
                .build(new CacheLoader<Integer, Integer>() {
                    @Override
                    public @Nullable Integer load(Integer key) throws Exception {
                        System.out.println("查询数据库");
                        return discussPostMapper.selectDiscussPostRows(key);
                    }
                });
    }

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    public List<DiscussPost> findDiscussPosts(int userId, int offset, int limit, int orderMode) {
        if(userId == 0 && orderMode == 1){
            return postListCache.get(offset + ":" + limit);
        }
        System.out.println("查询数据库");
        return discussPostMapper.selectDiscussPosts(userId, offset, limit, orderMode);
    }

    public int findDiscussPostRows(int userId) {
        if(userId == 0){
            return postRowCache.get(userId);
        }
        System.out.println("查询数据库");
        return discussPostMapper.selectDiscussPostRows(userId);
    }

    public int addDiscussPost(DiscussPost post) {
        if (post == null) {
            throw new IllegalArgumentException("参数不能为空!");
        }

        // 转义HTML标记
        post.setTitle(HtmlUtils.htmlEscape(post.getTitle()));
        post.setContent(HtmlUtils.htmlEscape(post.getContent()));
        // 过滤敏感词
        post.setTitle(sensitiveFilter.filter(post.getTitle()));
        post.setContent(sensitiveFilter.filter(post.getContent()));

        return discussPostMapper.insertDiscussPost(post);
    }

    public DiscussPost findDiscussPostById(int id) {
        return discussPostMapper.selectDiscussPostById(id);
    }

    public int updateCommentCount(int id, int commentCount) {
        return discussPostMapper.updateCommentCount(id, commentCount);
    }

    public Integer updateType(Integer id, Integer type){
        return discussPostMapper.updateType(id,type);
    }

    public Integer updateStatus(Integer id, Integer type){
        return discussPostMapper.updateStatus(id,type);
    }

    public Integer updateScore(Integer id, Double score){
        return discussPostMapper.updateScore(id,score);
    }
}
