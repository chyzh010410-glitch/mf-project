package com.mf.fertilizer.ai.controller.client;

import com.mf.fertilizer.ai.service.AiService;
import com.mf.fertilizer.vo.ResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/client/ai")
@RequiredArgsConstructor
public class ClientAiController {

    private final AiService aiService;

    /** 智能客服 —— 用户提问，RAG 检索 FAQ+百科后回答 */
    @PostMapping("/chat")
    public ResultVO<?> chat(@RequestBody Map<String, String> body) {
        String question = body.get("question");
        if (question == null || question.isBlank()) return ResultVO.fail(400, "请输入您的问题");
        try {
            var result = aiService.chat(question);
            return ResultVO.success(result);
        } catch (Exception e) {
            return ResultVO.fail(503, "AI 客服暂不可用，请稍后重试");
        }
    }
}
