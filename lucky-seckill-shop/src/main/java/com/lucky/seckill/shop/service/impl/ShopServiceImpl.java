package com.lucky.seckill.shop.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.lucky.seckill.common.enums.ResponseCodeEnum;
import com.lucky.seckill.common.exception.BizException;
import com.lucky.seckill.common.model.entity.PageResult;
import com.lucky.seckill.notify.feign.NotifyFeignClient;
import com.lucky.seckill.shop.model.dto.ShopApplyQueryRequest;
import com.lucky.seckill.shop.model.vo.ShopApplyVO;
import com.lucky.seckill.user.feign.UserFeignClient;
import com.lucky.seckill.common.manager.upload.FilePictureUpload;
import com.lucky.seckill.common.model.entity.Notify;
import com.lucky.seckill.common.mq.MerchantApplyProducer;
import com.lucky.seckill.common.model.entity.MerchantApplyMessage;
import com.lucky.seckill.common.model.file.UploadPictureResult;
import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.shop.mapper.ShopMapper;
import com.lucky.seckill.shop.model.dto.CreateShopRequest;
import com.lucky.seckill.shop.model.dto.ShopAuditRequest;
import com.lucky.seckill.shop.model.entity.Shop;
import com.lucky.seckill.shop.service.ShopService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 店铺服务实现类
 */
@Service
@Slf4j
public class ShopServiceImpl implements ShopService {

    @Resource
    private ShopMapper shopMapper;

    @Resource
    private MerchantApplyProducer merchantApplyProducer;

    @Resource
    private UserFeignClient userFeignClient;

    @Resource
    private NotifyFeignClient notifyFeignClient;

    @Resource
    private FilePictureUpload filePictureUpload;

    private static final Long SHOP_OWNER_ROLE_ID = 2L;

    /**
     * 申请店铺
     *
     * @param createShopRequest
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<?> createShop(CreateShopRequest createShopRequest) {
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("==> 用户申请开店, userId: {}", userId);

        Shop existingShop = shopMapper.selectByUserId(userId);
        if (Objects.nonNull(existingShop)) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "您已经拥有店铺，无法重复开店");
        }

        // 获取用户信息，设置 username
        String username = null;
        try {
            Object userObj = userFeignClient.getUserById(userId);
            if (userObj != null) {
                // 尝试从用户对象中获取 nickname 或 mobile 作为 username
                if (userObj instanceof java.util.Map) {
                    java.util.Map<?, ?> userMap = (java.util.Map<?, ?>) userObj;
                    Object nickname = userMap.get("nickname");
                    Object mobile = userMap.get("mobile");
                    username = nickname != null ? nickname.toString() : (mobile != null ? mobile.toString() : null);
                }
            }
        } catch (Exception e) {
            log.warn("==> 获取用户信息失败，userId: {}, error: {}", userId, e.getMessage());
        }

        long shopId = IdUtil.getSnowflakeNextId();
        Shop shop = Shop.builder()
                .id(shopId)
                .shopName(createShopRequest.getShopName())
                .shopAvatar(createShopRequest.getShopAvatar())
                .shopDesc(createShopRequest.getShopDesc())
                .userId(userId)
                .username(username)
                .auditStatus(0)
                .shopStatus(1)
                .isDelete(0)
                .createTime(new java.util.Date())
                .updateTime(new java.util.Date())
                .build();

        int result = shopMapper.insertSelective(shop);
        if (result <= 0) {
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "店铺创建失败");
        }

        log.info("==> 店铺创建成功, shopId: {}, userId: {}, shopName: {}", shopId, userId, createShopRequest.getShopName());

        long notifyId = IdUtil.getSnowflakeNextId();
        Notify notify = Notify.builder()
                .id(notifyId)
                .sendUserId(userId)
                .receiveUserId(5L)
                .title("新开店申请")
                .content("有新的开店申请，请到店铺审核界面查看详情并处理")
                .notifyType(1)
                .businessId(shopId)
                .subType(0)
                .isRead(0)
                .isDelete(0)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        try {
            Response<Boolean> notifyResponse = notifyFeignClient.saveNotify(notify);
            checkFeignResponse(notifyResponse, "开店申请消息保存失败");
        } catch (Exception e) {
            log.error("==> 调用消息服务保存开店申请通知失败, shopId: {}, error: {}", shopId, e.getMessage(), e);
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "开店申请消息保存失败");
        }

        log.info("==> 开店申请通知保存成功, notifyId: {}, shopId: {}", notifyId, shopId);

        String messageId = IdUtil.fastSimpleUUID();
        MerchantApplyMessage message = MerchantApplyMessage.builder()
                .shopId(shopId)
                .userId(userId)
                .shopName(createShopRequest.getShopName())
                .shopAvatar(createShopRequest.getShopAvatar())
                .shopDesc(createShopRequest.getShopDesc())
                .applyTime(new java.util.Date())
                .status(0)
                .messageId(messageId)
                .build();

        merchantApplyProducer.sendMerchantApplyMessage(message);

        log.info("==> 商家申请消息发送成功, userId: {}, shopId: {}", userId, shopId);

        return Response.success();
    }

    /**
     * 审核店铺
     *
     * @param shopAuditRequest
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<?> auditShop(ShopAuditRequest shopAuditRequest) {
        log.info("==> 管理员审批商家申请, shopId: {}, auditStatus: {}, auditRemark: {}",
                shopAuditRequest.getShopId(), shopAuditRequest.getAuditStatus(), shopAuditRequest.getAuditRemark());

        Shop shop = shopMapper.selectByPrimaryKey(shopAuditRequest.getShopId());
        if (Objects.isNull(shop)) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "店铺不存在");
        }

        if (shop.getAuditStatus() != 0) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "该店铺已审核，请勿重复审核");
        }

        shop.setAuditStatus(shopAuditRequest.getAuditStatus());
        shop.setAuditRemark(shopAuditRequest.getAuditRemark());
        shop.setUpdateTime(new java.util.Date());

        int result = shopMapper.updateByPrimaryKeySelective(shop);
        if (result <= 0) {
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "审核状态更新失败");
        }

        log.info("==> 店铺审核状态更新成功, shopId: {}, auditStatus: {}",
                shopAuditRequest.getShopId(), shopAuditRequest.getAuditStatus());

        try {
            Response<Void> readResponse = notifyFeignClient.markApplyNotifyRead(1, shopAuditRequest.getShopId());
            checkFeignResponse(readResponse, "开店申请通知标记已读失败");
            log.info("==> 开店申请通知已标记为已读, shopId: {}", shopAuditRequest.getShopId());
        } catch (Exception e) {
            log.error("==> 调用消息服务标记通知已读失败, shopId: {}, error: {}",
                    shopAuditRequest.getShopId(), e.getMessage(), e);
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "开店申请通知标记已读失败");
        }
        Long adminId = StpUtil.getLoginIdAsLong();

        if (shopAuditRequest.getAuditStatus().equals(ShopAuditRequest.AUDIT_PASS)) {
            String title = "开店申请已通过";
            String content = String.format("恭喜！您的店铺【%s】开店申请已通过审核！", shop.getShopName());

            Notify auditNotify = Notify.builder()
                    .id(IdUtil.getSnowflakeNextId())
                    .sendUserId(adminId)
                    .receiveUserId(shop.getUserId())
                    .title(title)
                    .content(content)
                    .notifyType(4)
                    .businessId(shopAuditRequest.getShopId())
                    .subType(0)
                    .isRead(0)
                    .isDelete(0)
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();

            try {
                Response<Boolean> auditNotifyResponse = notifyFeignClient.saveNotify(auditNotify);
                checkFeignResponse(auditNotifyResponse, "审核通过通知发送失败");
            } catch (Exception e) {
                log.error("==> 调用消息服务发送审核通过通知失败, shopId: {}, error: {}",
                        shopAuditRequest.getShopId(), e.getMessage(), e);
                throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "审核通过通知发送失败");
            }
            log.info("==> 审核通过通知已发送, notifyId: {}", auditNotify.getId());

            log.info("==> 审核通过，调用用户服务为用户分配商家角色, userId: {}, roleId: {}",
                    shop.getUserId(), SHOP_OWNER_ROLE_ID);

            try {
                userFeignClient.assignMerchantRole(shop.getUserId(), SHOP_OWNER_ROLE_ID);
                log.info("==> 用户商家角色分配成功, userId: {}", shop.getUserId());
            } catch (Exception e) {
                log.error("==> 调用用户服务分配角色失败, userId: {}, error: {}",
                        shop.getUserId(), e.getMessage(), e);
                throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "商家角色分配失败");
            }
        } else {
            String title = "开店申请被拒绝";
            String content = String.format("抱歉，您的店铺【%s】开店申请未通过：%s",
                    shop.getShopName(), shopAuditRequest.getAuditRemark());

            Notify rejectNotify = Notify.builder()
                    .id(IdUtil.getSnowflakeNextId())
                    .sendUserId(adminId)
                    .receiveUserId(shop.getUserId())
                    .title(title)
                    .content(content)
                    .notifyType(4)
                    .businessId(shopAuditRequest.getShopId())
                    .subType(0)
                    .isRead(0)
                    .isDelete(0)
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();

            try {
                Response<Boolean> rejectNotifyResponse = notifyFeignClient.saveNotify(rejectNotify);
                checkFeignResponse(rejectNotifyResponse, "审核拒绝通知发送失败");
            } catch (Exception e) {
                log.error("==> 调用消息服务发送审核拒绝通知失败, shopId: {}, error: {}",
                        shopAuditRequest.getShopId(), e.getMessage(), e);
                throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "审核拒绝通知发送失败");
            }
            log.info("==> 审核拒绝通知已发送, notifyId: {}", rejectNotify.getId());
        }

        log.info("==> 商家申请审批完成, shopId: {}, auditStatus: {}",
                shopAuditRequest.getShopId(), shopAuditRequest.getAuditStatus());

        return Response.success();
    }


    /**
     * 查询商家申请列表（支持多条件筛选和分页）
     */
    @Override
    public Response<PageResult<ShopApplyVO>> getShopApplyList(ShopApplyQueryRequest queryRequest) {
        log.info("==> 查询商家申请列表, 用户名: {}, 店铺名: {}, 审核状态: {}, 时间范围: {} - {}",
                queryRequest.getUsername(), queryRequest.getShopName(),
                queryRequest.getAuditStatus(), queryRequest.getStartTime(), queryRequest.getEndTime());

        // 设置默认分页参数
        Integer pageNum = queryRequest.getPageNum() != null ? queryRequest.getPageNum() : 1;
        Integer pageSize = queryRequest.getPageSize() != null ? queryRequest.getPageSize() : 10;

        // 开启分页
        PageHelper.startPage(pageNum, pageSize);

        // 查询数据
        List<Shop> shopList = shopMapper.selectShopApplyList(
                queryRequest.getUsername(),
                queryRequest.getShopName(),
                queryRequest.getAuditStatus(),
                queryRequest.getStartTime() != null ? Date.from(queryRequest.getStartTime().atZone(ZoneId.systemDefault()).toInstant()) : null,
                queryRequest.getEndTime() != null ? Date.from(queryRequest.getEndTime().atZone(ZoneId.systemDefault()).toInstant()) : null
        );

        // 获取分页信息
        Page<Shop> page = (Page<Shop>) shopList;

        // 转换为 VO
        List<ShopApplyVO> voList = shopList.stream().map(shop -> {
            ShopApplyVO vo = new ShopApplyVO();
            vo.setId(shop.getId());
            vo.setShopName(shop.getShopName());
            vo.setShopAvatar(shop.getShopAvatar());
            vo.setShopDesc(shop.getShopDesc());
            vo.setUserId(shop.getUserId());
            vo.setUsername(shop.getUsername());
            vo.setAuditStatus(shop.getAuditStatus());
            vo.setAuditStatusDesc(getAuditStatusDesc(shop.getAuditStatus()));
            vo.setAuditRemark(shop.getAuditRemark());
            vo.setShopStatus(shop.getShopStatus());
            vo.setCreateTime(shop.getCreateTime() != null ?
                    LocalDateTime.ofInstant(shop.getCreateTime().toInstant(), ZoneId.systemDefault()) : null);
            vo.setUpdateTime(shop.getUpdateTime() != null ?
                    LocalDateTime.ofInstant(shop.getUpdateTime().toInstant(), ZoneId.systemDefault()) : null);
            return vo;
        }).collect(Collectors.toList());

        // 构建分页结果
        PageResult<ShopApplyVO> pageResult = PageResult.<ShopApplyVO>builder()
                .records(voList)
                .total(page.getTotal())
                .pageNum(pageNum)
                .pageSize(pageSize)
                .totalPages((int) Math.ceil((double) page.getTotal() / pageSize))
                .build();

        log.info("==> 查询商家申请列表完成, 总数: {}, 当前页: {}", page.getTotal(), pageNum);

        return Response.success(pageResult);
    }

    /**
     * 获取审核状态描述
     */
    private String getAuditStatusDesc(Integer auditStatus) {
        if (auditStatus == null) {
            return "未知";
        }
        switch (auditStatus) {
            case 0:
                return "待审核";
            case 1:
                return "审核通过";
            case 2:
                return "审核拒绝";
            default:
                return "未知";
        }
    }

    /**
     * 上传店铺头像
     */
    @Override
    public Response<UploadPictureResult> uploadShopAvatar(MultipartFile file) {
        Long userId = StpUtil.getLoginIdAsLong();

        UploadPictureResult uploadResult = filePictureUpload.uploadPicture(file, "shop-avatar", true);

        Shop existingShop = shopMapper.selectByUserId(userId);
        if (Objects.nonNull(existingShop)) {
            Shop shop = Shop.builder()
                    .id(existingShop.getId())
                    .shopAvatar(uploadResult.getImageUrl())
                    .updateTime(new java.util.Date())
                    .build();
            shopMapper.updateByPrimaryKeySelective(shop);
            log.info("==> 店铺头像上传并更新成功, shopId: {}, avatarUrl: {}",
                    existingShop.getId(), uploadResult.getImageUrl());
        } else {
            log.info("==> 开店申请前上传店铺头像, userId: {}, avatarUrl: {}",
                    userId, uploadResult.getImageUrl());
        }

        return Response.success(uploadResult);
    }

    private void checkFeignResponse(Response<?> response, String errorMessage) {
        if (response == null || !response.isSuccess()) {
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, errorMessage);
        }
    }
}
