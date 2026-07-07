package com.mf.fertilizer.platform.service;

import com.mf.fertilizer.platform.dto.client.FeedbackSubmitDTO;
import com.mf.fertilizer.platform.entity.ActivityEntity;
import com.mf.fertilizer.platform.entity.Faq;
import com.mf.fertilizer.platform.vo.client.HomePageVO;

import java.util.List;

public interface PlatformPortalApplicationService {

    List<ActivityEntity> listActiveActivities();

    ActivityEntity getActivityDetail(Long id);

    List<Faq> listPublishedFaqs(String category);

    void submitFeedback(Long userId, FeedbackSubmitDTO dto);

    HomePageVO getHomePage();
}
