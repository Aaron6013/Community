package com.nowcoder.community.quartz;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.ElasticsearchService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.RedisKeyUtil;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author wensheng
 * @create 2022-03-14 11:23 上午
 **/

public class PostScoreRefreshJob implements Job, CommunityConstant {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private ElasticsearchService elasticsearchService;

    private static final Date epoch;

    static {
        try {
            epoch = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2014-08-01 00:00:00)");
        } catch (ParseException e) {
            throw new RuntimeException("初始化牛客纪元失败");
        }
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String postScoreKey = RedisKeyUtil.getPostScoreKey();
        BoundSetOperations boundSetOperations = redisTemplate.boundSetOps(postScoreKey);
        if(boundSetOperations != null && boundSetOperations.size() != 0){
            System.out.println("任务开始，正在刷新帖子分数:" + boundSetOperations.size());
            while (boundSetOperations.size() > 0){
                this.refresh((Integer)boundSetOperations.pop());
            }
            System.out.println("帖子分数刷新完毕");
        }else {
            return;
        }
    }

    private void refresh(Integer postId) {
        DiscussPost discussPost = discussPostService.findDiscussPostById(postId);
        if(discussPost == null){
            System.out.println("该帖子不存在: id=" + postId);
            return;
        }
        //是否加精
        boolean wonderful = discussPost.getStatus() == 1;
        //评论数量
        int commentCount = discussPost.getCommentCount();
        //点赞数量
        long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, postId);

        //计算权重
        double w = (wonderful ? 75 : 0) + commentCount * 10 + likeCount * 2;
        //分数
        double score = Math.log10(Math.max(w,1)) + (discussPost.getCreateTime().getTime() - epoch.getTime())/(1000*36000*24);
        //更新帖子分数
        discussPostService.updateScore(postId,score);
        //更新el
        discussPost.setScore(score);
        elasticsearchService.saveDiscussPost(discussPost);
    }
}
