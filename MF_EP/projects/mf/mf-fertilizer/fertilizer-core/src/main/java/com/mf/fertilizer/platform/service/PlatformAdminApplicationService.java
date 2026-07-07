package com.mf.fertilizer.platform.service;

import com.mf.fertilizer.dto.PageDTO;
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
import com.mf.fertilizer.vo.PageVO;

import java.util.Map;

public interface PlatformAdminApplicationService {

    PageVO<ActivityEntity> listActivities(PageDTO page, String keyword, String status, String type);

    ActivityEntity getActivity(Long id);

    void createActivity(ActivitySaveDTO dto);

    void updateActivity(Long id, ActivitySaveDTO dto);

    void deleteActivity(Long id);

    void updateActivityStatus(Long id, String status);

    PageVO<PlatformConfig> listConfigs(PageDTO page, String keyword, String configGroup);

    PlatformConfig getConfig(Long id);

    void createConfig(PlatformConfigSaveDTO dto);

    void updateConfig(Long id, PlatformConfigSaveDTO dto);

    void deleteConfig(Long id);

    PageVO<Faq> listFaqs(PageDTO page, String keyword, String category);

    Faq getFaq(Long id);

    void createFaq(FaqSaveDTO dto);

    void updateFaq(Long id, FaqSaveDTO dto);

    void deleteFaq(Long id);

    PageVO<Message> listMessages(PageDTO page);

    void sendMessage(MessageSendDTO dto);

    PageVO<Feedback> listFeedbacks(PageDTO page, String status, String type);

    void replyFeedback(Long id, Map<String, String> body);

    PageVO<UserUpload> listUploads(PageDTO page, String status, String keyword);

    void reviewUpload(Long id, UploadReviewDTO dto);

    PageVO<SystemLog> listLogs(PageDTO page, String module, String keyword);
}
