package com.travelplatform.contenttrip.dto.share;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public class SharePostCreateRequest {

    @NotBlank(message = "分享标题不能为空")
    @Size(max = 100, message = "分享标题不能超过100个字符")
    private String title;

    @NotBlank(message = "分享摘要不能为空")
    @Size(max = 255, message = "分享摘要不能超过255个字符")
    private String summary;

    @NotBlank(message = "分享内容不能为空")
    @Size(max = 5000, message = "分享内容不能超过5000个字符")
    private String content;

    @NotEmpty(message = "请至少上传一张图片")
    private List<@NotBlank(message = "图片地址不能为空") String> imageUrls;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }
}
