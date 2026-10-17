package com.lucky.quota.common.manager.upload;

import cn.hutool.core.io.FileUtil;
import com.lucky.quota.common.enums.ResponseCodeEnum;
import com.lucky.quota.common.exception.BizException;
import com.squareup.okhttp.internal.framed.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
    文件图片上传
 */
@Service
public class FilePictureUpload extends PictureUploadTemplate{


    // 获取输入流（流式上传）
    @Override
    protected InputStream getInputStream(Object inputSource) throws Exception {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        // 直接返回流，不写磁盘！
        return multipartFile.getInputStream();
    }

    // 获取文件大小
    @Override
    protected long getFileSize(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        return multipartFile.getSize();
    }

    @Override
    protected String getOriginFilename(Object inputSource) {
        MultipartFile multipartFile= (MultipartFile) inputSource;
        return multipartFile.getOriginalFilename();
    }

    @Override
    protected void validPicture(Object inputSource) {
       MultipartFile multipartFile= (MultipartFile) inputSource;
       if(multipartFile== null){
           throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID,"文件不能为空");
       }
        //校验文件大小
        long fileSize=multipartFile.getSize();
        final long ONE_M=1024*1024;
        if(fileSize>6 * ONE_M){
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID,"文件不能超过 6MB");
        }
        // 校验文件后缀
        String suffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        // 允许上传的文件后缀列表（或者集合）
        final List<String> ALLOW_FORMAT_LIST= Arrays.asList("jpg","jpeg","png","webp");
        if (!ALLOW_FORMAT_LIST.contains(suffix)){
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID,"文件格式错误");
        }
    }
}
