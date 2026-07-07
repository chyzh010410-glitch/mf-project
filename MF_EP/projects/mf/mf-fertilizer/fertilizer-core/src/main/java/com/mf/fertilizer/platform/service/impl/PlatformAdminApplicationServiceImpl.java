package com.mf.fertilizer.platform.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mf.fertilizer.context.UserContext;
import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.exception.BusinessException;
import com.mf.fertilizer.platform.dto.admin.ActivitySaveDTO;
import com.mf.fertilizer.platform.dto.admin.FaqSaveDTO;
import com.mf.fertilizer.platform.dto.admin.MessageSendDTO;
import com.mf.fertilizer.platform.dto.admin.PlatformConfigSaveDTO;
import com.mf.fertilizer.platform.dto.admin.UploadReviewDTO;
import com.mf.fertilizer.platform.entity.ActivityEntity;
import com.mf.fertilizer.platform.entity.Faq;
import com.mf.fertilizer.platform.entity.Feedback;
import com.mf.fertilizer.platform.entity.Message;
import com.mf.fertilizer.platform.entity.PlatformConfig;
import com.mf.fertilizer.platform.entity.SystemLog;
import com.mf.fertilizer.platform.entity.UserUpload;
import com.mf.fertilizer.platform.service.ActivityEntityService;
import com.mf.fertilizer.platform.service.FaqService;
import com.mf.fertilizer.platform.service.FeedbackService;
import com.mf.fertilizer.platform.service.MessageService;
import com.mf.fertilizer.platform.service.PlatformAdminApplicationService;
import com.mf.fertilizer.platform.service.PlatformConfigService;
import com.mf.fertilizer.platform.service.SystemLogService;
import com.mf.fertilizer.platform.service.UserUploadService;
import com.mf.fertilizer.user.service.UserService;
import com.mf.fertilizer.vo.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PlatformAdminApplicationServiceImpl implements PlatformAdminApplicationService {

    private final ActivityEntityService activityService;
    private final PlatformConfigService configService;
    private final FaqService faqService;
    private final MessageService messageService;
    private final UserService userService;
    private final FeedbackService feedbackService;
    private final UserUploadService uploadService;
    private final SystemLogService logService;

    @Override
    public PageVO<ActivityEntity> listActivities(PageDTO page, String keyword, String status, String type) {
        var wrapper = new LambdaQueryWrapper<ActivityEntity>()
                .like(StrUtil.isNotBlank(keyword), ActivityEntity::getTitle, keyword)
                .eq(StrUtil.isNotBlank(status), ActivityEntity::getStatus, status)
                .eq(StrUtil.isNotBlank(type), ActivityEntity::getType, type)
                .orderByDesc(ActivityEntity::getSortOrder)
                .orderByDesc(ActivityEntity::getCreateTime);
        var result = activityService.page(new Page<>(page.getPage(), page.getSize()), wrapper);
        return PageVO.of(page, result);
    }

    @Override
    public ActivityEntity getActivity(Long id) {
        return requireActivity(id);
    }

    @Override
    public void createActivity(ActivitySaveDTO dto) {
        ActivityEntity activity = new ActivityEntity();
        BeanUtils.copyProperties(dto, activity);
        if (activity.getSortOrder() == null) {
            activity.setSortOrder(0);
        }
        if (activity.getIsBanner() == null) {
            activity.setIsBanner(0);
        }
        activity.setStatus("active");
        activityService.save(activity);
    }

    @Override
    public void updateActivity(Long id, ActivitySaveDTO dto) {
        ActivityEntity activity = requireActivity(id);
        BeanUtils.copyProperties(dto, activity);
        activity.setId(id);
        activityService.updateById(activity);
    }

    @Override
    public void deleteActivity(Long id) {
        activityService.removeById(id);
    }

    @Override
    public void updateActivityStatus(Long id, String status) {
        ActivityEntity activity = requireActivity(id);
        activity.setStatus(status);
        activityService.updateById(activity);
    }

    @Override
    public PageVO<PlatformConfig> listConfigs(PageDTO page, String keyword, String configGroup) {
        var wrapper = new LambdaQueryWrapper<PlatformConfig>()
                .like(StrUtil.isNotBlank(keyword), PlatformConfig::getConfigKey, keyword)
                .eq(StrUtil.isNotBlank(configGroup), PlatformConfig::getConfigGroup, configGroup)
                .orderByAsc(PlatformConfig::getConfigGroup)
                .orderByAsc(PlatformConfig::getConfigKey);
        var result = configService.page(new Page<>(page.getPage(), page.getSize()), wrapper);
        return PageVO.of(page, result);
    }

    @Override
    public PlatformConfig getConfig(Long id) {
        return requireConfig(id);
    }

    @Override
    public void createConfig(PlatformConfigSaveDTO dto) {
        PlatformConfig config = new PlatformConfig();
        BeanUtils.copyProperties(dto, config);
        configService.save(config);
    }

    @Override
    public void updateConfig(Long id, PlatformConfigSaveDTO dto) {
        PlatformConfig config = requireConfig(id);
        BeanUtils.copyProperties(dto, config);
        config.setId(id);
        configService.updateById(config);
    }

    @Override
    public void deleteConfig(Long id) {
        configService.removeById(id);
    }

    @Override
    public PageVO<Faq> listFaqs(PageDTO page, String keyword, String category) {
        var wrapper = new LambdaQueryWrapper<Faq>()
                .like(StrUtil.isNotBlank(keyword), Faq::getQuestion, keyword)
                .eq(StrUtil.isNotBlank(category), Faq::getCategory, category)
                .orderByAsc(Faq::getSortOrder)
                .orderByDesc(Faq::getCreateTime);
        var result = faqService.page(new Page<>(page.getPage(), page.getSize()), wrapper);
        return PageVO.of(page, result);
    }

    @Override
    public Faq getFaq(Long id) {
        return requireFaq(id);
    }

    @Override
    public void createFaq(FaqSaveDTO dto) {
        Faq faq = new Faq();
        BeanUtils.copyProperties(dto, faq);
        if (faq.getIsPublished() == null) {
            faq.setIsPublished(1);
        }
        if (faq.getSortOrder() == null) {
            faq.setSortOrder(0);
        }
        faqService.save(faq);
    }

    @Override
    public void updateFaq(Long id, FaqSaveDTO dto) {
        Faq faq = requireFaq(id);
        BeanUtils.copyProperties(dto, faq);
        faq.setId(id);
        faqService.updateById(faq);
    }

    @Override
    public void deleteFaq(Long id) {
        faqService.removeById(id);
    }

    @Override
    public PageVO<Message> listMessages(PageDTO page) {
        var wrapper = new LambdaQueryWrapper<Message>().orderByDesc(Message::getCreateTime);
        var result = messageService.page(new Page<>(page.getPage(), page.getSize()), wrapper);
        return PageVO.of(page, result);
    }

    @Override
    public void sendMessage(MessageSendDTO dto) {
        if (dto.getUserIds() == null || dto.getUserIds().isEmpty()) {
            throw new BusinessException(400, "请选择消息接收用户");
        }
        var notFound = new ArrayList<Long>();
        for (Long userId : dto.getUserIds()) {
            if (userService.getById(userId) == null) {
                notFound.add(userId);
                continue;
            }
            Message message = new Message();
            message.setUserId(userId);
            message.setTitle(dto.getTitle());
            message.setContent(dto.getContent());
            message.setType(dto.getType());
            message.setPushChannel(dto.getPushChannel());
            message.setIsRead(0);
            messageService.save(message);
        }
        if (!notFound.isEmpty()) {
            throw new BusinessException(400, "以下用户不存在: " + notFound);
        }
    }

    @Override
    public PageVO<Feedback> listFeedbacks(PageDTO page, String status, String type) {
        var wrapper = new LambdaQueryWrapper<Feedback>()
                .eq(StrUtil.isNotBlank(status), Feedback::getStatus, status)
                .eq(StrUtil.isNotBlank(type), Feedback::getType, type)
                .orderByDesc(Feedback::getCreateTime);
        var result = feedbackService.page(new Page<>(page.getPage(), page.getSize()), wrapper);
        return PageVO.of(page, result);
    }

    @Override
    public void replyFeedback(Long id, Map<String, String> body) {
        Feedback feedback = requireFeedback(id);
        feedback.setHandlerReply(body.get("reply"));
        feedback.setHandlerId(UserContext.getUserId());
        feedback.setHandleTime(LocalDateTime.now());
        feedback.setStatus("handled");
        feedbackService.updateById(feedback);
    }

    @Override
    public PageVO<UserUpload> listUploads(PageDTO page, String status, String keyword) {
        var wrapper = new LambdaQueryWrapper<UserUpload>()
                .eq(StrUtil.isNotBlank(status), UserUpload::getStatus, status)
                .like(StrUtil.isNotBlank(keyword), UserUpload::getName, keyword)
                .orderByDesc(UserUpload::getCreateTime);
        var result = uploadService.page(new Page<>(page.getPage(), page.getSize()), wrapper);
        return PageVO.of(page, result);
    }

    @Override
    public void reviewUpload(Long id, UploadReviewDTO dto) {
        UserUpload upload = requireUpload(id);
        upload.setStatus(dto.getStatus());
        upload.setReviewComment(dto.getReviewComment());
        upload.setReviewerId(UserContext.getUserId());
        upload.setReviewTime(LocalDateTime.now());
        uploadService.updateById(upload);
    }

    @Override
    public PageVO<SystemLog> listLogs(PageDTO page, String module, String keyword) {
        var wrapper = new LambdaQueryWrapper<SystemLog>()
                .eq(StrUtil.isNotBlank(module), SystemLog::getModule, module)
                .and(StrUtil.isNotBlank(keyword), q -> q
                        .like(SystemLog::getOperatorName, keyword).or()
                        .like(SystemLog::getAction, keyword).or()
                        .like(SystemLog::getTarget, keyword))
                .orderByDesc(SystemLog::getCreateTime);
        var result = logService.page(new Page<>(page.getPage(), page.getSize()), wrapper);
        return PageVO.of(page, result);
    }

    private ActivityEntity requireActivity(Long id) {
        ActivityEntity activity = activityService.getById(id);
        if (activity == null) {
            throw new BusinessException(404, "活动不存在");
        }
        return activity;
    }

    private PlatformConfig requireConfig(Long id) {
        PlatformConfig config = configService.getById(id);
        if (config == null) {
            throw new BusinessException(404, "配置不存在");
        }
        return config;
    }

    private Faq requireFaq(Long id) {
        Faq faq = faqService.getById(id);
        if (faq == null) {
            throw new BusinessException(404, "FAQ不存在");
        }
        return faq;
    }

    private Feedback requireFeedback(Long id) {
        Feedback feedback = feedbackService.getById(id);
        if (feedback == null) {
            throw new BusinessException(404, "反馈不存在");
        }
        return feedback;
    }

    private UserUpload requireUpload(Long id) {
        UserUpload upload = uploadService.getById(id);
        if (upload == null) {
            throw new BusinessException(404, "上传记录不存在");
        }
        return upload;
    }
}
