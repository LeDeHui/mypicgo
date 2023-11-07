package com.mp.common.module.videoStory.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mp.common.business.radio.PostDrawUtil;
import com.mp.common.business.radio.model.*;
import com.mp.common.business.user.model.SbrTeacherInfoEx;
import com.mp.common.exception.SpException;
import com.mp.common.module.authority.entity.SbrUser;
import com.mp.common.module.authority.service.ISbrUserService;
import com.mp.common.module.base.util.DateTimeUtil;
import com.mp.common.module.radio.entity.*;
import com.mp.common.module.user.entity.SbrStudentInfo;
import com.mp.common.module.user.entity.SbrTeacherInfo;
import com.mp.common.module.user.service.ISbrStudentInfoService;
import com.mp.common.module.user.service.ISbrTeacherInfoService;
import com.mp.common.module.videoStory.dto.QueryVideoStoryCenter;
import com.mp.common.module.videoStory.dto.SbrVideoStoryEx;
import com.mp.common.module.videoStory.dto.SbrVideoStoryInfoEx;
import com.mp.common.module.videoStory.dto.SbrVideoStoryMainList;
import com.mp.common.module.videoStory.entity.*;
import com.mp.common.module.videoStory.mapper.SbrVideoStoryMainMapper;
import com.mp.common.module.videoStory.service.*;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sun.corba.se.spi.orbutil.threadpool.WorkQueue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author Le_DH
 * @since 2023-10-07
 */
@Service
@Slf4j
public class SbrVideoStoryMainServiceImpl extends ServiceImpl<SbrVideoStoryMainMapper, SbrVideoStoryMain> implements ISbrVideoStoryMainService {

    @Autowired
    ISbrVideoStoryCountService iSbrVideoStoryCountService;
    @Autowired
    ISbrVideoStoryMediaService iSbrVideoStoryMediaService;
    @Autowired
    ISbrStudentInfoService iSbrStudentInfoService;
    @Autowired
    ISbrUserService userService;
    @Autowired
    ISbrVideoStoryMainService iSbrVideoStoryMainService;
    @Autowired
    ISbrVideoStoryDatailService iSbrVideoStoryDatailService;
    @Autowired
    ISbrVideoStoryDetailBackgroundService iSbrVideoStoryDetailBackgroundService;

    @Autowired
    ISbrVideoStoryStationService iSbrVideoStoryStationService;

    @Autowired
    ISbrVideoStoryNameService iSbrVideoStoryNameService;

    @Autowired
    ISbrTeacherInfoService iSbrTeacherInfoService;
    @Value("${sp.teacherUrl}")
    private String webUrl;

    @Override
    @Transactional
    public String uploadRadioStory(SbrVideoStoryEx sbrVideoStoryEx, SbrTeacherInfoEx currentUser) throws Exception {
        String result = "故事创建成功";
        //判断故事正片是否已上传 只判断正片,其他类型不判断
        QueryWrapper<SbrVideoStoryMain> sbrVideoStoryMainQueryWrapper = new QueryWrapper<>();
        sbrVideoStoryMainQueryWrapper.eq(SbrVideoStoryMain.STORY_TITLE_ID, sbrVideoStoryEx.getAffVideo());
        sbrVideoStoryMainQueryWrapper.eq(SbrVideoStoryMain.STORY_TYPE, "1");
        List<SbrVideoStoryMain> list = this.list(sbrVideoStoryMainQueryWrapper);
        if ("1".equalsIgnoreCase(sbrVideoStoryEx.getVideoType())) {
            if (list.size() > 0) {
                log.error("故事" + sbrVideoStoryEx.getAffVideo() + "正片已经上传,请上传其他类型");
                throw new SpException("故事正片已经上传,请上传其他类型");
            }
        } else {
            if (list.size() <= 0) {
                log.error("故事" + sbrVideoStoryEx.getAffVideo() + "还未上传正片,请确认");
                throw new SpException("故事还未上传正片,请确认");
            }
        }
        //判断故事正片是否已上传
        String videoStoryId = UUID.randomUUID().toString();
        String dateTime = DateTimeUtil.getDateTime();
        SbrVideoStoryMain sbrVideoStoryMain = new SbrVideoStoryMain();
        sbrVideoStoryMain.setStoryUid(videoStoryId);
        sbrVideoStoryMain.setStoryType(sbrVideoStoryEx.getVideoType());
        sbrVideoStoryMain.setSchoolId(currentUser.getSchoolId());
        sbrVideoStoryMain.setDeptId(currentUser.getDeptId());
        sbrVideoStoryMain.setSubTitle(sbrVideoStoryEx.getVideoDesc());
        sbrVideoStoryMain.setStoryTitleId(sbrVideoStoryEx.getAffVideo());
        sbrVideoStoryMain.setExamineStatus("wait");
        sbrVideoStoryMain.setAuthorType("teacher");
        sbrVideoStoryMain.setIsActive("Y");
        sbrVideoStoryMain.setCreateTime(dateTime);
        sbrVideoStoryMain.setCreator(currentUser.getTeacherId());
        boolean save = this.save(sbrVideoStoryMain);
        SbrVideoStoryMedia sbrVideoStoryMedia = new SbrVideoStoryMedia();
        sbrVideoStoryMedia.setMediaUid(UUID.randomUUID().toString());
        sbrVideoStoryMedia.setStoryUid(videoStoryId);
        sbrVideoStoryMedia.setMediaType("video");
        sbrVideoStoryMedia.setMediaFile(sbrVideoStoryEx.getVideoStoryMp4());
        sbrVideoStoryMedia.setListOrder(0);
        sbrVideoStoryMedia.setCreator(currentUser.getTeacherId());
        sbrVideoStoryMedia.setCreateTime(dateTime);
        boolean save1 = iSbrVideoStoryMediaService.save(sbrVideoStoryMedia);
        SbrVideoStoryCount sbrVideoStoryCount = new SbrVideoStoryCount();
        sbrVideoStoryCount.setStoryUid(videoStoryId);
        sbrVideoStoryCount.setInitCount(0);
        sbrVideoStoryCount.setCount(0);
        sbrVideoStoryCount.setCreateTime(dateTime);
        sbrVideoStoryCount.setCreator(currentUser.getTeacherId());
        boolean save2 = iSbrVideoStoryCountService.save(sbrVideoStoryCount);

        //todo-ledehui 保存sbrVideoStoryDetail


        if (!(save && save1 && save2)) {
            throw new SpException("故事创建失败");
        }


//        todo-ledehui 微信推送
        return result;

         /*
        对照保存数据库
        select * from  sbr_story_detail  order by create_time desc limit 10;
        select * from  sbr_story_media  order by create_time desc limit 10;
        select * from  sbr_radio_story  order by create_time desc limit 10;
        select * from  sbr_story_count  order by create_time desc limit 10;

        select * from  sbr_video_story_datail;
        select * from  sbr_video_story_media;
        select * from  sbr_video_story_main;
        select * from  sbr_video_story_count;*/


//        BeanUtil.copyProperties(sbrRadioStoryEx, sbrRadioStory);
//        sbrRadioStory.setStoryUid(UUID.randomUUID().toString());
//        sbrRadioStory.setOpertorExamineStatus("pass");
//        sbrRadioStory.setAuthorType("operate");
//        //自定义发表时间
//        if (ObjectUtil.isEmpty(sbrRadioStoryEx.getReleaseTime())) {
//            sbrRadioStory.setReleaseTime(DateTimeUtil.getDateTime());
//        }
//        sbrRadioStory.setCreator(sbrRadioStoryEx.getAuthorUid());
//        sbrRadioStory.setIsActive("Y");
//        sbrRadioStory.setCreateTime(DateTimeUtil.getDateTime());
//        String audioUrl = sbrRadioStoryEx.getAudioUrl();
//        //插入媒体表
//        SbrVideoStoryMedia sbrStoryMedia = new SbrVideoStoryMedia();
//        sbrStoryMedia.setMediaUid(UUID.randomUUID().toString());
//        sbrStoryMedia.setStoryUid(sbrRadioStory.getStoryUid());
//        sbrStoryMedia.setMediaFile(audioUrl);
//        sbrStoryMedia.setMediaLength(sbrRadioStoryEx.getMediaLength());
//        sbrStoryMedia.setMediaType("video");
//        sbrStoryMedia.setCreator(sbrRadioStoryEx.getAuthorUid());
//        sbrStoryMedia.setCreateTime(DateTimeUtil.getDateTime());
//        //插入阅读数表
//        SbrVideoStoryCount sbrStoryCount = new SbrVideoStoryCount();
//        sbrStoryCount.setStoryUid(sbrRadioStory.getStoryUid());
//        sbrStoryCount.setCreator(sbrRadioStoryEx.getAuthorUid());
//        sbrStoryCount.setCreateTime(DateTimeUtil.getDateTime());
//        sbrStoryCount.setInitCount(sbrRadioStoryEx.getCount());
//        sbrStoryCount.setCount(sbrRadioStoryEx.getCount());
//        boolean mediaSaveResult =  iSbrVideoStoryMediaService.save(sbrStoryMedia);
//        boolean storySaveResult = this.save(sbrRadioStory);
//        boolean countSaveResult = iSbrVideoStoryCountService.save(sbrStoryCount);

    }

    @Override
    public SbrRadioStoryList getStoryList(QueryVideoStoryCenter queryRadioStory) {
        SbrRadioStoryList sbrRadioStoryList = new SbrRadioStoryList();
        List<SbrRadioStoryEx> sbrRadioStoryExList = new ArrayList<>();
        QueryWrapper queryWrapper = new QueryWrapper();
        if (ObjectUtil.isNotEmpty(queryRadioStory.getIsShow())) {
            if ("Y".equals(queryRadioStory.getIsShow())) {
                queryWrapper.eq(SbrRadioStory.OPERTOR_EXAMINE_STATUS, "pass");
            } else {
                queryWrapper.ne(SbrRadioStory.OPERTOR_EXAMINE_STATUS, "pass");
            }
        } else {
            queryWrapper.ne(SbrRadioStory.OPERTOR_EXAMINE_STATUS, "del");
        }
        if (ObjectUtil.isNotEmpty(queryRadioStory.getStoryTitle())) {
            queryWrapper.like(SbrRadioStory.STORY_TITLE, queryRadioStory.getStoryTitle());
        }
        queryWrapper.eq(SbrRadioStory.AUTHOR_TYPE, "operate");
        queryWrapper.orderByDesc(SbrRadioStory.RELEASE_TIME);
        //查询故事列表
        queryWrapper.select(SbrRadioStory.STORY_TITLE, SbrRadioStory.SUB_TITLE, SbrRadioStory.STORY_ICON, SbrRadioStory.STORY_UID,
                SbrRadioStory.IS_ACTIVE, SbrRadioStory.AUTHOR_UID, SbrRadioStory.RELEASE_TIME);
        IPage<SbrVideoStoryMain> sbrRadioStoryIPage = this.page(new Page<>(queryRadioStory.getPageNum(), queryRadioStory.getPageSize()), queryWrapper);
        //未查询到故事数据 直接返回
        if (ObjectUtil.isEmpty(sbrRadioStoryIPage.getRecords())) {
            sbrRadioStoryList.setSchoolName("");
            sbrRadioStoryList.setSchoolLogo("");
            sbrRadioStoryList.setBannerBgUrl("");
            sbrRadioStoryList.setSbrRadioStoryExList(new ArrayList<>());
            return sbrRadioStoryList;
        }

        for (SbrVideoStoryMain sbrRadioStory : sbrRadioStoryIPage.getRecords()) {
            SbrRadioStoryEx sbrRadioStoryEx = new SbrRadioStoryEx();
            BeanUtil.copyProperties(sbrRadioStory, sbrRadioStoryEx);
            //查询播放数-redis
            sbrRadioStoryEx.setCount(iSbrVideoStoryCountService.getStoryReadNum(sbrRadioStory.getStoryUid()));
            //查询相关音频
            queryWrapper = new QueryWrapper();
            queryWrapper.eq(SbrStoryMedia.STORY_UID, sbrRadioStory.getStoryUid());
            queryWrapper.eq(SbrStoryMedia.MEDIA_TYPE, "video");
            queryWrapper.select(SbrStoryMedia.MEDIA_FILE, SbrStoryMedia.MEDIA_LENGTH);
            List<SbrVideoStoryMedia> sbrStoryMediaList = iSbrVideoStoryMediaService.page(new Page<>(1, 1), queryWrapper).getRecords();
            if (ObjectUtil.isNotEmpty(sbrStoryMediaList)) {
                sbrRadioStoryEx.setAudioUrl(sbrStoryMediaList.get(0).getMediaFile());
            }
            //查询发表人名称
            queryWrapper = new QueryWrapper();
            queryWrapper.select(SbrUser.REAL_NAME);
            queryWrapper.eq(SbrUser.USER_UID, sbrRadioStory.getAuthorUid());
            List<SbrUser> sbrUserList = userService.list(queryWrapper);
            if (ObjectUtil.isNotEmpty(sbrUserList)) {
                sbrRadioStoryEx.setAuthorName(sbrUserList.get(0).getRealName());
            }
            sbrRadioStoryExList.add(sbrRadioStoryEx);
        }
        sbrRadioStoryList.setSbrRadioStoryExList(sbrRadioStoryExList);
        sbrRadioStoryList.setCount(sbrRadioStoryIPage.getTotal());
        return sbrRadioStoryList;
    }

    @Override
    public String deleteStory(String storyId) {
        String result = "删除失败";
        SbrVideoStoryMain sbrRadioStory = new SbrVideoStoryMain();
        sbrRadioStory.setStoryUid(storyId);
        sbrRadioStory.setOpertorExamineStatus("del");
        sbrRadioStory.setModifyTime(DateTimeUtil.getDateTime());
        boolean updateResult = this.updateById(sbrRadioStory);
        if (updateResult) {
            result = "删除成功";
        }
        return result;
    }

    @Override
    public SbrRadioStoryList getSchoolStoryList(QueryVideoStoryCenter queryRadioStory) {
        SbrRadioStoryList sbrRadioStoryList = new SbrRadioStoryList();
        List<SbrRadioStoryEx> sbrRadioStoryExList = new ArrayList<>();
        QueryWrapper queryWrapper = new QueryWrapper();
        IPage<SbrRadioStoryEx> sbrRadioStoryIPage = baseMapper.getStoryExList(new Page<>(queryRadioStory.getPageNum(), queryRadioStory.getPageSize()), queryRadioStory.getStoryTitle(), queryRadioStory.getCheckStatus(), queryRadioStory.getOrderBy());
        //未查询到故事数据 直接返回
        if (ObjectUtil.isEmpty(sbrRadioStoryIPage.getRecords())) {
            sbrRadioStoryList.setSchoolName("");
            sbrRadioStoryList.setSchoolLogo("");
            sbrRadioStoryList.setBannerBgUrl("");
            sbrRadioStoryList.setSbrRadioStoryExList(new ArrayList<>());
            return sbrRadioStoryList;
        }

        for (SbrRadioStoryEx sbrRadioStory : sbrRadioStoryIPage.getRecords()) {
            SbrRadioStoryEx sbrRadioStoryEx = new SbrRadioStoryEx();
            BeanUtil.copyProperties(sbrRadioStory, sbrRadioStoryEx);
            //查询播放数-redis
//            sbrRadioStoryEx.setCount(iStoryCountService.getStoryReadNum(sbrRadioStory.getStoryUid()));
            sbrRadioStoryEx.setCount(iSbrVideoStoryCountService.getStoryReadNum(sbrRadioStory.getStoryUid()));
            //查询相关音频
            queryWrapper = new QueryWrapper();
            queryWrapper.eq(SbrStoryMedia.STORY_UID, sbrRadioStory.getStoryUid());
            queryWrapper.eq(SbrStoryMedia.MEDIA_TYPE, "audio");
            queryWrapper.select(SbrStoryMedia.MEDIA_FILE, SbrStoryMedia.MEDIA_LENGTH);
//            List<SbrStoryMedia> sbrStoryMediaList = iSbrStoryMediaService.page(new Page<>(1, 1), queryWrapper).getRecords();
            List<SbrStoryMedia> sbrStoryMediaList = iSbrVideoStoryMediaService.page(new Page<>(1, 1), queryWrapper).getRecords();
            if (ObjectUtil.isNotEmpty(sbrStoryMediaList)) {
                sbrRadioStoryEx.setAudioUrl(sbrStoryMediaList.get(0).getMediaFile());
            }
            //查询发表人名称
            queryWrapper = new QueryWrapper();
            queryWrapper.select(SbrStudentInfo.STU_NAME);
            queryWrapper.eq(SbrStudentInfo.STU_UID, sbrRadioStory.getAuthorUid());
            List<SbrStudentInfo> sbrUserList = iSbrStudentInfoService.list(queryWrapper);
            sbrRadioStoryEx.setAuthorName("");
            if (ObjectUtil.isNotEmpty(sbrUserList)) {
                sbrRadioStoryEx.setAuthorName(sbrUserList.get(0).getStuName());
            }
            sbrRadioStoryExList.add(sbrRadioStoryEx);
        }
        sbrRadioStoryList.setSbrRadioStoryExList(sbrRadioStoryExList);
        sbrRadioStoryList.setCount(sbrRadioStoryIPage.getTotal());
        return sbrRadioStoryList;
    }


    @Override
    public SbrVideoStoryMainList getStoryList(QueryRadioStory queryRadioStory, String token) {
        // TODO: 2021/5/8 0008 如果审核状态查询类型是 refuse 则将refuse和preFuse状态的数据都返回
        //置顶数据
        SbrRadioStoryEx sbrRadioStoryEx = null;
        //背景图数据
        SbrRadioBackground sbrRadioBackground = null;
        String bannerUrl = "";
        // TODO: 2021/5/12 0012 添加电台主题颜色
        String themeColor = "";
        //数据库查询结果-带分页
        IPage<SbrRadioStoryEx> sbrRadioStoryIPageEx = null;
        //返回给前端数据实体
        SbrVideoStoryMainList sbrRadioStoryList = new SbrVideoStoryMainList();
        List<SbrRadioStoryEx> sbrRadioStoryExList = new ArrayList<>();
        //数据查询包装类
        QueryWrapper queryWrapper = new QueryWrapper();
        List<SbrTeacherInfo> sbrTeacherInfoList = null;
        switch (queryRadioStory.getAuthorType()) {
//            //教师投稿箱
//            case "teacherDraft":
//                queryRadioStory.setAuthorType("teacher");
//                queryRadioStory.setAuthorUid(queryRadioStory.getTeacherId());
//                sbrRadioStoryIPageEx = iSbrVideoStoryMainService.queryDratStoryExList(new Page<>(queryRadioStory.getPageNum(), queryRadioStory.getPageSize()), queryRadioStory);
//                //设置审核人员名称
//                queryWrapper = new QueryWrapper();
//                queryWrapper.eq(SbrTeacherInfo.SCHOOL_ID, queryRadioStory.getSchoolId());
//                queryWrapper.select(SbrTeacherInfo.TEACHER_NAME, SbrTeacherInfo.TEACHER_ID);
//                sbrTeacherInfoList = iSbrTeacherInfoService.list(queryWrapper);
//                if (ObjectUtil.isEmpty(sbrTeacherInfoList) || ObjectUtil.isEmpty(sbrRadioStoryIPageEx.getRecords())) {
//                    break;
//                }
//                for (SbrRadioStoryEx sbrRadioStoryEx1 : sbrRadioStoryIPageEx.getRecords()) {
//                    String firstCheckorId = sbrRadioStoryEx1.getFirstCheckor();
//                    String secondCheckorId = sbrRadioStoryEx1.getCheckor();
//                    for (SbrTeacherInfo sbrTeacherInfo : sbrTeacherInfoList) {
//                        if (sbrTeacherInfo.getTeacherId().equals(firstCheckorId)) {
//                            sbrRadioStoryEx1.setFirstCheckor(sbrTeacherInfo.getTeacherName());
//                        }
//                        if (sbrTeacherInfo.getTeacherId().equals(secondCheckorId)) {
//                            sbrRadioStoryEx1.setCheckor(sbrTeacherInfo.getTeacherName());
//                        }
//                    }
//                }
//                break;
//            //家长投稿箱
//            case "parentDraft":
//                queryRadioStory.setAuthorType("parent");
//                queryRadioStory.setAuthorUid(queryRadioStory.getStudentUid());
//                sbrRadioStoryIPageEx = iSbrRadioStoryService.queryDratStoryExList(new Page<>(queryRadioStory.getPageNum(), queryRadioStory.getPageSize()), queryRadioStory);
//                //设置审核人员名称
//                queryWrapper = new QueryWrapper();
//                queryWrapper.eq(SbrTeacherInfo.SCHOOL_ID, queryRadioStory.getSchoolId());
//                queryWrapper.select(SbrTeacherInfo.TEACHER_NAME, SbrTeacherInfo.TEACHER_ID);
//                sbrTeacherInfoList = iSbrTeacherInfoService.list(queryWrapper);
//                if (ObjectUtil.isEmpty(sbrTeacherInfoList) || ObjectUtil.isEmpty(sbrRadioStoryIPageEx.getRecords())) {
//                    break;
//                }
//                for (SbrRadioStoryEx sbrRadioStoryEx1 : sbrRadioStoryIPageEx.getRecords()) {
//                    String firstCheckorId = sbrRadioStoryEx1.getFirstCheckor();
//                    String secondCheckorId = sbrRadioStoryEx1.getCheckor();
//                    for (SbrTeacherInfo sbrTeacherInfo : sbrTeacherInfoList) {
//                        if (sbrTeacherInfo.getTeacherId().equals(firstCheckorId)) {
//                            sbrRadioStoryEx1.setFirstCheckor(sbrTeacherInfo.getTeacherName());
//                        }
//                        if (sbrTeacherInfo.getTeacherId().equals(secondCheckorId)) {
//                            sbrRadioStoryEx1.setCheckor(sbrTeacherInfo.getTeacherName());
//                        }
//                    }
//                }
//                break;
            //教师故事审核
            case "storyAudit":
                sbrRadioStoryIPageEx = baseMapper.queryAuditStoryExList(new Page<>(queryRadioStory.getPageNum(), queryRadioStory.getPageSize()), queryRadioStory);
                //设置审核人员名称
                queryWrapper = new QueryWrapper();
                queryWrapper.eq(SbrTeacherInfo.SCHOOL_ID, queryRadioStory.getSchoolId());
                queryWrapper.select(SbrTeacherInfo.TEACHER_NAME, SbrTeacherInfo.TEACHER_ID);
                sbrTeacherInfoList = iSbrTeacherInfoService.list(queryWrapper);
                if (ObjectUtil.isEmpty(sbrTeacherInfoList) || ObjectUtil.isEmpty(sbrRadioStoryIPageEx.getRecords())) {
                    break;
                }
                for (SbrRadioStoryEx sbrRadioStoryEx1 : sbrRadioStoryIPageEx.getRecords()) {
                    String firstCheckorId = sbrRadioStoryEx1.getFirstCheckor();
                    String secondCheckorId = sbrRadioStoryEx1.getCheckor();
                    for (SbrTeacherInfo sbrTeacherInfo : sbrTeacherInfoList) {
                        if (sbrTeacherInfo.getTeacherId().equals(firstCheckorId)) {
                            sbrRadioStoryEx1.setFirstCheckor(sbrTeacherInfo.getTeacherName());
                        }
                        if (sbrTeacherInfo.getTeacherId().equals(secondCheckorId)) {
                            sbrRadioStoryEx1.setCheckor(sbrTeacherInfo.getTeacherName());
                        }
                    }
                }
                break;
//            //小鲸电台
            case "videoToday":
//                sbrRadioStoryIPageEx = iSbrRadioStoryService.queryToDayStoryExList(new Page<>(queryRadioStory.getPageNum(), queryRadioStory.getPageSize()), queryRadioStory);
                QueryWrapper<SbrVideoStoryName> sbrVideoStoryNameQueryWrapper1 = new QueryWrapper<>();
                sbrVideoStoryNameQueryWrapper1.eq(SbrVideoStoryName.SCHOOL_ID, queryRadioStory.getSchoolId());
                sbrVideoStoryNameQueryWrapper1.eq(SbrVideoStoryName.DEPT_ID, queryRadioStory.getDeptId());
                List<SbrVideoStoryName> list2 = iSbrVideoStoryNameService.list(sbrVideoStoryNameQueryWrapper1);
                sbrRadioStoryList.setSbrRadioStoryExList(list2);
                break;
            //校园电台
            case "videoStory":
                QueryWrapper<SbrVideoStoryName> sbrVideoStoryNameQueryWrapper = new QueryWrapper<>();
                sbrVideoStoryNameQueryWrapper.eq(SbrVideoStoryName.SCHOOL_ID, queryRadioStory.getSchoolId());
                sbrVideoStoryNameQueryWrapper.eq(SbrVideoStoryName.DEPT_ID, queryRadioStory.getDeptId());
                List<SbrVideoStoryName> list = iSbrVideoStoryNameService.list(sbrVideoStoryNameQueryWrapper);
                list.stream().map(item->{
                    List<SbrVideoStoryInfoEx> sbrVideoStoryInfoExs = baseMapper.getStoryInfo(item.getStoryTitleId());
                    Map<String, List<SbrVideoStoryInfoEx>> map = new HashMap<>();
                    List<SbrVideoStoryInfoEx> collect1 = sbrVideoStoryInfoExs.stream().filter(i1 -> {
                        return "1".equalsIgnoreCase(i1.getStoryType());
                    }).collect(Collectors.toList());
                    map.put("1", collect1);
                    List<SbrVideoStoryInfoEx> collect2 = sbrVideoStoryInfoExs.stream().filter(i2 -> {
                        return "2".equalsIgnoreCase(i2.getStoryType());
                    }).collect(Collectors.toList());
                    map.put("2", collect2);
                    List<SbrVideoStoryInfoEx> collect3 = sbrVideoStoryInfoExs.stream().filter(i3 -> {
                        return "3".equalsIgnoreCase(i3.getStoryType());
                    }).collect(Collectors.toList());
                    map.put("3", collect3);
                    return sbrVideoStoryInfoExs;

                }).collect(Collectors.toList());



                sbrRadioStoryList.setSbrRadioStoryExList(list);
//                //查询置顶数据
//                sbrRadioStoryEx = iSbrVideoStoryMediaService.querySchoolTopStoryEx(queryRadioStory.getSchoolId());
//                if (ObjectUtil.isNotEmpty(sbrRadioStoryEx)) {
//                    //排除置顶数据
//                    queryRadioStory.setTopStoryUid(sbrRadioStoryEx.getStoryUid());
//                }
//                //查询故事数据
//                sbrRadioStoryIPageEx = iSbrVideoStoryMediaService.querySchoolStoryExList(new Page<>(queryRadioStory.getPageNum(), queryRadioStory.getPageSize()), queryRadioStory);
                break;
            default:
                throw new SpException(queryRadioStory.getAuthorType() + " 该电台范围类型不存在");
        }
//        todo-ledehui
//        for (int i = 0; i < sbrRadioStoryIPageEx.getRecords().size(); i++) {
//            SbrRadioStoryEx sbrRadioStoryEx1 = sbrRadioStoryIPageEx.getRecords().get(i);
//            QueryWrapper<SbrVideoStoryDatail> sbrStoryDetailQueryWrapper = new QueryWrapper<>();
//            sbrStoryDetailQueryWrapper.likeRight(SbrStoryDetail.STORY_UID,sbrRadioStoryEx1.getStoryUid());
//            List<SbrVideoStoryDatail> list = iSbrVideoStoryDatailService.list(sbrStoryDetailQueryWrapper);
//            List<SbrRadioStoryNewListEx> collect = list.stream().filter(item2 ->{
//                return item2.getRemarks().length()!=0;
//            }).map(item -> {
//                SbrRadioStoryNewListEx sbrRadioStoryNewListEx = new SbrRadioStoryNewListEx();
//                sbrRadioStoryNewListEx.setDetailPhoto(item.getDetailPhoto());
//                sbrRadioStoryNewListEx.setDetailContent(item.getDetailContent());
//
//                return sbrRadioStoryNewListEx;
//            }).collect(Collectors.toList());
//            sbrRadioStoryEx1.setListimagesDesc(collect);
//        }
//        sbrRadioStoryExList = sbrRadioStoryIPageEx.getRecords();
//        //故事总数
//        long storyCount = sbrRadioStoryIPageEx.getTotal();
//        //添加置顶数据---第一页查询则添加置顶数据
//        if (ObjectUtil.isNotEmpty(sbrRadioStoryEx) && queryRadioStory.getPageNum() == 1) {
//            sbrRadioStoryExList.add(0, sbrRadioStoryEx);
//            storyCount += 1;
//        }
//        sbrRadioStoryList.setCount(storyCount);
//        //未查询到故事数据
//        if (ObjectUtil.isEmpty(sbrRadioStoryExList)) {
//            sbrRadioStoryList.setSchoolName("");
//            sbrRadioStoryList.setSchoolLogo("");
//            sbrRadioStoryList.setBannerBgUrl("");
//            sbrRadioStoryExList = new ArrayList<>();
//            sbrRadioStoryList.setSbrRadioStoryExList(sbrRadioStoryExList);
//        }
//        //查询播放数-redis
//        for (SbrRadioStoryEx sbrRadioStory : sbrRadioStoryExList) {
//            sbrRadioStory.setCount(iSbrVideoStoryCountService.getStoryReadNum(sbrRadioStory.getStoryUid()));
//        }
//        //查询故事的banner图
//        if ("today".equals(queryRadioStory.getAuthorType())) {
//            queryWrapper = new QueryWrapper();
//            queryWrapper.eq(SbrRadioBackground.TYPE, "today");
//            queryWrapper.select(SbrRadioBackground.BANNER_BG_URL);
//            queryWrapper.orderByDesc(SbrRadioBackground.CREATE_TIME);
//            List<SbrVideoStoryDetailBackground> sbrRadioBackgroundList = iSbrVideoStoryDetailBackgroundService.page(new Page(1, 1), queryWrapper).getRecords();
//            if (ObjectUtil.isNotEmpty(sbrRadioBackgroundList)) {
//                bannerUrl = sbrRadioBackgroundList.get(0).getBannerBgUrl();
//                themeColor = sbrRadioBackgroundList.get(0).getThemeColor();
//            }
//        } else {
//            //根据学校ID联表查询查询学校电台banner背景图
//            sbrRadioBackground = iSbrVideoStoryDetailBackgroundService.getRadioBgBySchoolId(queryRadioStory.getSchoolId());
//            if (ObjectUtil.isNotEmpty(sbrRadioBackground)) {
//                bannerUrl = sbrRadioBackground.getBannerBgUrl();
//                themeColor = sbrRadioBackground.getThemeColor();
//            }
//        }
//        sbrRadioStoryList.setBannerBgUrl(bannerUrl);
//        //查询学校logo、名称、故事海报
//        queryWrapper = new QueryWrapper();
//        queryWrapper.eq(SbrRadioStation.SCHOOL_ID, queryRadioStory.getSchoolId());
//        queryWrapper.select(SbrRadioStation.RADIO_UID, SbrRadioStation.SCHOOL_LOGO, SbrRadioStation.SCHOOL_NAME, SbrRadioStation.STORY_URL, SbrRadioStation.POST_URL, SbrRadioStation.SCHOOL_ADDRESS, SbrRadioStation.SCHOOL_SLOGAN, SbrRadioStation.SCHOOL_ID, SbrRadioStation.CREATOR);
//        List<SbrVideoStoryStation> sbrRadioStationList = iSbrVideoStoryStationService.page(new Page<>(1, 1), queryWrapper).getRecords();
//        if (ObjectUtil.isEmpty(sbrRadioStationList)) {
//            sbrRadioStoryList.setSbrRadioStoryExList(sbrRadioStoryExList);
//            return sbrRadioStoryList;
//        }
        QueryWrapper<SbrVideoStoryStation> sbrVideoStoryStationQueryWrapper = new QueryWrapper<>();
        sbrVideoStoryStationQueryWrapper.eq(SbrVideoStoryStation.SCHOOL_ID,queryRadioStory.getSchoolId());
        sbrVideoStoryStationQueryWrapper.select(SbrRadioStation.RADIO_UID, SbrRadioStation.SCHOOL_LOGO, SbrRadioStation.SCHOOL_NAME, SbrRadioStation.STORY_URL, SbrRadioStation.POST_URL, SbrRadioStation.SCHOOL_ADDRESS, SbrRadioStation.SCHOOL_SLOGAN, SbrRadioStation.SCHOOL_ID, SbrRadioStation.CREATOR);
        SbrVideoStoryStation sbrVideoStoryStationServiceOne = iSbrVideoStoryStationService.getOne(sbrVideoStoryStationQueryWrapper);
        if (sbrVideoStoryStationServiceOne==null) {
            return  sbrRadioStoryList;
        }
        sbrRadioStoryList.setSchoolLogo(sbrVideoStoryStationServiceOne.getSchoolLogo());
        sbrRadioStoryList.setSchoolName(sbrVideoStoryStationServiceOne.getSchoolName());
        //如果海报未生成 则主动生成
        if (ObjectUtil.isEmpty(sbrVideoStoryStationServiceOne.getStoryUrl()) || ObjectUtil.isEmpty(sbrVideoStoryStationServiceOne.getPostUrl()) ) {
//        if ((ObjectUtil.isEmpty(sbrVideoStoryStationServiceOne.getStoryUrl()) || ObjectUtil.isEmpty(sbrVideoStoryStationServiceOne.getPostUrl())) && ObjectUtil.isNotEmpty(sbrRadioBackground)) {
            SbrRadioStationEx sbrRadioStationEx = new SbrRadioStationEx();
            BeanUtil.copyProperties(sbrVideoStoryStationServiceOne, sbrRadioStationEx);
//            sbrRadioStationEx.setPosterBgUrl(sbrRadioBackground.getPosterBgUrl());
//            sbrRadioStationEx.setInvitationUrl(sbrRadioBackground.getInvitationUrl());
//            sbrRadioStationEx.setTeacherId(sbrRadioStation.getCreator());
//            //生成电台海报
            String postUrl = PostDrawUtil.generateRadioPost(sbrRadioStationEx, token, webUrl);
            sbrVideoStoryStationServiceOne.setPostUrl(postUrl);
//            //生成故事海报
            String storyUrl = PostDrawUtil.generateStoryPost(sbrRadioStationEx, token, webUrl);
            sbrVideoStoryStationServiceOne.setStoryUrl(storyUrl);
            sbrVideoStoryStationServiceOne.setModifyTime(DateTimeUtil.getDateTime());
            sbrVideoStoryStationServiceOne.setRemark("自动生成海报");
            iSbrVideoStoryStationService.updateById(sbrVideoStoryStationServiceOne);
        }
        sbrRadioStoryList.setStoryUrl(sbrVideoStoryStationServiceOne.getStoryUrl());
        sbrRadioStoryList.setPostUrl(sbrVideoStoryStationServiceOne.getPostUrl());
//        sbrRadioStoryList.setSbrRadioStoryExList(sbrRadioStoryExList);
        sbrRadioStoryList.setThemeColor(themeColor);
        return sbrRadioStoryList;
    }

    @Override
    public IPage<SbrRadioStoryEx> queryToDayStoryExList(Page iPage, QueryRadioStory queryRadioStory) {
        return baseMapper.queryToDayStoryExList(iPage, queryRadioStory, DateTimeUtil.getDateTime());

    }

    @Override
    public SbrRadioStoryEx querySchoolTopStoryEx(String schoolId) {
        return baseMapper.querySchoolTopStoryEx(schoolId);
    }

    @Override
    public List<SbrVideoStoryInfoEx> getStoryInfo(String storyTitleId, String token) {
        List<SbrVideoStoryInfoEx> sbrVideoStoryInfoExs = baseMapper.getStoryInfo(storyTitleId);
        Map<String, List<SbrVideoStoryInfoEx>> map = new HashMap<>();
        List<SbrVideoStoryInfoEx> collect1 = sbrVideoStoryInfoExs.stream().filter(item -> {
            return "1".equalsIgnoreCase(item.getStoryType());
        }).collect(Collectors.toList());
        map.put("1", collect1);
        List<SbrVideoStoryInfoEx> collect2 = sbrVideoStoryInfoExs.stream().filter(item -> {
            return "2".equalsIgnoreCase(item.getStoryType());
        }).collect(Collectors.toList());
        map.put("2", collect2);
        List<SbrVideoStoryInfoEx> collect3 = sbrVideoStoryInfoExs.stream().filter(item -> {
            return "3".equalsIgnoreCase(item.getStoryType());
        }).collect(Collectors.toList());
        map.put("3", collect3);


        return sbrVideoStoryInfoExs;

    }

    @Override
    public SbrVideoStoryMainList getStoryListAudit(QueryRadioStory queryRadioStory, String token) {
        IPage<SbrRadioStoryEx> sbrRadioStoryIPageEx = null;
        sbrRadioStoryIPageEx = baseMapper.queryAuditStoryExList(new Page<>(queryRadioStory.getPageNum(), queryRadioStory.getPageSize()), queryRadioStory);
        SbrVideoStoryMainList sbrVideoStoryMainList = new SbrVideoStoryMainList();
        sbrVideoStoryMainList.setCount(sbrRadioStoryIPageEx.getSize());

        List<SbrRadioStoryEx> sbrRadioStoryExList = new ArrayList<>();
        sbrRadioStoryExList = sbrRadioStoryIPageEx.getRecords();
        sbrVideoStoryMainList.setSbrRadioStoryExs(sbrRadioStoryExList);
        return sbrVideoStoryMainList;
    }

    @Override
    public String checkStory(SbrRadioStoryEx sbrRadioStoryEx, SbrTeacherInfoEx currentUser) {
        String result = "故事审核失败";
        String resultOK = "故事审核成功";
        log.info(sbrRadioStoryEx.toString());
        log.info(currentUser.toString());
        SbrVideoStoryMain sbrVideoStoryMain = this.getById(sbrRadioStoryEx.getStoryUid());
        String dateTime = DateTimeUtil.getDateTime();
        sbrVideoStoryMain.setFirstCheckor(sbrRadioStoryEx.getCheckor());
        sbrVideoStoryMain.setCheckor(sbrRadioStoryEx.getCheckor());
        sbrVideoStoryMain.setModifyTime(dateTime);
        if ("pass".equalsIgnoreCase(sbrRadioStoryEx.getExamineStatus())) {
            sbrVideoStoryMain.setExamineStatus("pass");
            log.info("pass");
        } else {
            sbrVideoStoryMain.setExamineStatus("refuse");
            sbrVideoStoryMain.setCheckAdvise(sbrRadioStoryEx.getCheckAdvise());
            log.info("refuse");
        }
        boolean b = this.saveOrUpdate(sbrVideoStoryMain);

        return b ?    resultOK:result;
    }

    @Override
    public IPage<SbrRadioStoryEx> querySchoolStoryExList(Page iPage, QueryRadioStory queryRadioStory) {
        return baseMapper.querySchoolStoryExList(iPage, queryRadioStory);
    }


}
