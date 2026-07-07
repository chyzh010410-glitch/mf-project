<template>
  <div class="page-container">
    <h2 class="section-title">意见反馈</h2>
    <div class="feedback-wrap">
      <div class="feedback-card">
        <h3>告诉我们你的问题或建议</h3>
        <p>提交后平台运营人员会在后台处理，必要时会通过你留下的联系方式回复。</p>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
          <el-form-item label="反馈类型" prop="type">
            <el-select v-model="form.type" style="width:100%">
              <el-option label="功能建议" value="suggestion" />
              <el-option label="使用问题" value="bug" />
              <el-option label="订单咨询" value="order" />
              <el-option label="其他" value="other" />
            </el-select>
          </el-form-item>
          <el-form-item label="反馈内容" prop="content">
            <el-input
              v-model="form.content"
              type="textarea"
              :rows="6"
              maxlength="500"
              show-word-limit
              placeholder="请描述你遇到的问题、期望的改进或其他建议"
            />
          </el-form-item>
          <el-form-item label="联系方式">
            <el-input v-model="form.contact" placeholder="手机号、邮箱或微信号，选填" />
          </el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">提交反馈</el-button>
        </el-form>
      </div>

      <div class="feedback-aside">
        <div class="aside-block">
          <strong>处理说明</strong>
          <span>建议类反馈会进入产品优化池，问题类反馈会优先排查复现。</span>
        </div>
        <div class="aside-block">
          <strong>写得更清楚</strong>
          <span>请尽量包含操作路径、页面名称和异常现象，能更快定位问题。</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { submitFeedback } from '@/api/feedback'

const formRef = ref()
const submitting = ref(false)
const form = reactive({
  type: 'suggestion',
  content: '',
  contact: ''
})

const rules = {
  type: [{ required: true, message: '请选择反馈类型', trigger: 'change' }],
  content: [{ required: true, message: '请输入反馈内容', trigger: 'blur' }]
}

const handleSubmit = async () => {
  await formRef.value?.validate()
  submitting.value = true
  try {
    await submitFeedback({ ...form })
    ElMessage.success('反馈已提交，感谢你的建议')
    form.type = 'suggestion'
    form.content = ''
    form.contact = ''
  } catch {
    // request interceptor already shows the message
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.feedback-wrap { display: grid; grid-template-columns: minmax(0, 1fr) 300px; gap: 22px; align-items: start; }
.feedback-card { background: #fff; border: 1px solid #ebeef5; border-radius: 14px; padding: 24px; }
.feedback-card h3 { margin: 0 0 8px; color: #25352b; font-size: 22px; }
.feedback-card p { color: #7a7f87; margin: 0 0 22px; line-height: 1.7; }
.feedback-aside { display: grid; gap: 14px; }
.aside-block { background: linear-gradient(135deg, #f2faf4, #fffaf0); border: 1px solid #e1eddf; border-radius: 14px; padding: 18px; }
.aside-block strong { display: block; color: #25352b; margin-bottom: 8px; }
.aside-block span { color: #66706a; font-size: 14px; line-height: 1.7; }
@media (max-width: 860px) {
  .feedback-wrap { grid-template-columns: 1fr; }
}
</style>
