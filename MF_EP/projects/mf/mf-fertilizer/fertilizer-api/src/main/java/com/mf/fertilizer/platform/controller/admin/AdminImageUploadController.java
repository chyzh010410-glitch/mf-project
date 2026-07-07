package com.mf.fertilizer.platform.controller.admin;

import com.mf.fertilizer.context.UserContext;
import com.mf.fertilizer.exception.BusinessException;
import com.mf.fertilizer.platform.entity.FileUpload;
import com.mf.fertilizer.platform.service.FileUploadService;
import com.mf.fertilizer.vo.ResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/admin/uploads")
@RequiredArgsConstructor
public class AdminImageUploadController {

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024L;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");

    private final FileUploadService fileUploadService;

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResultVO<Map<String, Object>> uploadImage(@RequestPart("file") MultipartFile file,
                                                     @RequestParam(defaultValue = "product_image") String purpose) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择图片文件");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new BusinessException("图片不能超过5MB");
        }

        String ext = getExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BusinessException("仅支持 jpg、jpeg、png、webp、gif 图片");
        }

        try {
            Path uploadRoot = Path.of("uploads", "products").toAbsolutePath().normalize();
            Files.createDirectories(uploadRoot);

            String storedName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            Path target = uploadRoot.resolve(storedName);
            file.transferTo(target);

            var image = ImageIO.read(target.toFile());
            FileUpload record = new FileUpload();
            record.setOriginalName(file.getOriginalFilename());
            record.setStoredName(storedName);
            record.setFilePath(target.toString());
            record.setFileUrl("/uploads/products/" + storedName);
            record.setFileSize(file.getSize());
            record.setMimeType(file.getContentType());
            record.setFileExt(ext);
            record.setUploaderId(UserContext.requireUserId());
            record.setUploaderType("admin");
            record.setPurpose(purpose);
            if (image != null) {
                record.setWidth(image.getWidth());
                record.setHeight(image.getHeight());
            }
            fileUploadService.save(record);

            return ResultVO.success(Map.of(
                    "url", record.getFileUrl(),
                    "id", record.getId(),
                    "originalName", record.getOriginalName()
            ));
        } catch (IOException e) {
            throw new BusinessException("图片上传失败");
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
