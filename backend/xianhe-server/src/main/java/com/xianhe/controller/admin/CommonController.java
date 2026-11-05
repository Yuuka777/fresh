package com.xianhe.controller.admin;

import com.xianhe.exception.FileUploadFailedException;
import com.xianhe.result.Result;
import com.xianhe.utils.AliOssUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 通用模块管理端接口（《接口文档》2.3）
 */
@RestController
@RequestMapping("/admin/common")
@Api(tags = "管理端 · 通用")
@Slf4j
public class CommonController {

    /** 允许的图片扩展名（契约 2.3.1） */
    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "webp");

    //单文件大小上限 2MB（契约 2.3.1）
    private static final long MAX_SIZE = 2L * 1024 * 1024;

    //对象名目录前缀，与契约示例 `xh-goods/2026/09/21/xxx.jpg` 一致
    private static final String OBJECT_PREFIX = "xh-goods";

    /** 按天分目录，避免单个目录下对象过多 */
    private static final DateTimeFormatter DATE_DIR = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    @Autowired
    private AliOssUtil aliOssUtil;

    /**
     * 上传图片，返回公网可访问地址
     * @param file
     * @return
     */
    @PostMapping("/upload")
    @ApiOperation("文件上传")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        // 三重校验：文件非空、体积、扩展名。都不通过时统一返回「文件上传失败」，
        // 不区分具体原因——多余的信息对运营没有帮助，反而方便探测。
        // 对象名只用服务端生成的 UUID 与原扩展名，**不采用客户端文件名**，
        if (file == null || file.isEmpty()) {
            throw new FileUploadFailedException();
        }
        if (file.getSize() > MAX_SIZE) {
            log.warn("上传被拒：文件 {} 字节，超过 {} 字节上限", file.getSize(), MAX_SIZE);
            throw new FileUploadFailedException();
        }

        String ext = extensionOf(file.getOriginalFilename());
        if (!ALLOWED_EXT.contains(ext)) {
            log.warn("上传被拒：扩展名 [{}] 不在允许范围内", ext);
            throw new FileUploadFailedException();
        }

        String objectName = OBJECT_PREFIX
                + "/" + LocalDate.now().format(DATE_DIR)
                + "/" + UUID.randomUUID().toString().replace("-", "")
                + "." + ext;

        try {
            byte[] content = file.getBytes();
            // 扩展名可以随手改，落库前再看一眼文件头：只放真正的那三种图片格式
            if (!looksLikeImage(content, ext)) {
                log.warn("上传被拒：文件头与扩展名 [{}] 不符", ext);
                throw new FileUploadFailedException();
            }
            String url = aliOssUtil.upload(content, objectName);
            return Result.success(url);
        } catch (IOException e) {
            log.error("读取上传文件内容失败", e);
            throw new FileUploadFailedException();
        }
    }

    /**
     * 按文件头判断是否真是图片：扩展名可伪造，必须看内容
     * @param content
     * @param ext
     * @return
     */
    private boolean looksLikeImage(byte[] content, String ext) {
        if (content == null || content.length < 12) {
            return false;
        }
        switch (ext) {
            case "jpg":
            case "jpeg":
                return (content[0] & 0xFF) == 0xFF && (content[1] & 0xFF) == 0xD8 && (content[2] & 0xFF) == 0xFF;
            case "png":
                return (content[0] & 0xFF) == 0x89 && content[1] == 'P' && content[2] == 'N' && content[3] == 'G';
            case "webp":
                return content[0] == 'R' && content[1] == 'I' && content[2] == 'F' && content[3] == 'F'
                        && content[8] == 'W' && content[9] == 'E' && content[10] == 'B' && content[11] == 'P';
            default:
                return false;
        }
    }

    /**
     * 取小写扩展名。文件名不含点（或无扩展名）时返回空串，交由白名单拦截
     * @param originalFilename
     * @return
     */
    private String extensionOf(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        int dot = originalFilename.lastIndexOf('.');
        if (dot < 0 || dot == originalFilename.length() - 1) {
            return "";
        }
        return originalFilename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
