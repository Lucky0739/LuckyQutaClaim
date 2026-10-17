package com.lucky.quota.shop.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.lucky.quota.common.enums.ResponseCodeEnum;
import com.lucky.quota.common.exception.BizException;
import com.lucky.quota.common.model.entity.PageResult;
import com.lucky.quota.common.utils.Response;
import com.lucky.quota.goods.feign.GoodsFeignClient;
import com.lucky.quota.goods.model.vo.GoodsListVO;
import com.lucky.quota.notify.feign.NotifyFeignClient;
import com.lucky.quota.notify.model.entity.Notify;
import com.lucky.quota.shop.model.dto.ShopApplyQueryRequest;
import com.lucky.quota.shop.model.vo.ShopApplyVO;
import com.lucky.quota.user.feign.UserFeignClient;
import com.lucky.quota.user.model.vo.UserProfileVO;
import com.lucky.quota.common.manager.upload.FilePictureUpload;
import com.lucky.quota.shop.mq.MerchantApplyProducer;
import com.lucky.quota.common.model.entity.MerchantApplyMessage;
import com.lucky.quota.common.model.file.UploadPictureResult;
import com.lucky.quota.common.utils.Response;
import com.lucky.quota.shop.mapper.ShopMapper;
import com.lucky.quota.shop.model.dto.CreateShopRequest;
import com.lucky.quota.shop.model.dto.ShopAuditRequest;
import com.lucky.quota.shop.model.entity.Shop;
import com.lucky.quota.shop.service.ShopService;
import com.lucky.quota.shop.model.vo.ShopListVO;
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
 * 发放单位服务实现类
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
    private GoodsFeignClient goodsFeignClient;

    @Resource
    private FilePictureUpload filePictureUpload;

    private static final Long SHOP_OWNER_ROLE_ID = 2L;

    private static final int MAX_SHOP_COUNT_PER_USER = 3;

    /**
     * 申请发放单位
     *
     * @param createShopRequest
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<?> createShop(CreateShopRequest createShopRequest) {
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("==> 用户申请发放单位, userId: {}", userId);

        int shopCount = shopMapper.countByUserId(userId);
        if (shopCount >= MAX_SHOP_COUNT_PER_USER) {
            throw new BizException(ResponseCodeEnum.SHOP_COUNT_LIMIT_EXCEEDED);
        }

        String username = resolveApplicantUsername(userId);

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
                .collectNum(0)
                .isDelete(0)
                .createTime(new java.util.Date())
                .updateTime(new java.util.Date())
                .build();

        int result = shopMapper.insertSelective(shop);
        if (result <= 0) {
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "发放单位创建失败");
        }

        log.info("==> 发放单位创建成功, shopId: {}, userId: {}, shopName: {}", shopId, userId, createShopRequest.getShopName());

        long notifyId = IdUtil.getSnowflakeNextId();
        Notify notify = Notify.builder()
                .id(notifyId)
                .sendUserId(userId)
                .receiveUserId(5L)
                .title("新发放单位申请")
                .content("有新的发放单位申请，请到发放单位审核界面查看详情并处理")
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
            checkFeignResponse(notifyResponse, "发放单位申请消息保存失败");
        } catch (Exception e) {
            log.error("==> 调用消息服务保存发放单位申请通知失败, shopId: {}, error: {}", shopId, e.getMessage(), e);
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "发放单位申请消息保存失败");
        }

        log.info("==> 发放单位申请通知保存成功, notifyId: {}, shopId: {}", notifyId, shopId);

        String messageId = IdUtil.fastSimpleUUID();
        MerchantApplyMessage message = MerchantApplyMessage.builder()
                .shopId(shopId)
                .userId(userId)
                .shopName(createShopRequest.getShopName())
                .shopAvatar(createShopRequest.getShopAvatar())
                .shopDesc(createShopRequest.getShopDesc())
                .applyTime(new Date())
                .status(0)
                .messageId(messageId)
                .build();

        merchantApplyProducer.sendMerchantApplyMessage(message);

        log.info("==> 商家申请消息发送成功, userId: {}, shopId: {}", userId, shopId);

        return Response.success();
    }

    /**
     * 审核发放单位
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
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "发放单位不存在");
        }

        if (shop.getAuditStatus() != 0) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "该发放单位已审核，请勿重复审核");
        }

        shop.setAuditStatus(shopAuditRequest.getAuditStatus());
        shop.setAuditRemark(shopAuditRequest.getAuditRemark());
        shop.setUpdateTime(new java.util.Date());

        int result = shopMapper.updateByPrimaryKeySelective(shop);
        if (result <= 0) {
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "审核状态更新失败");
        }

        log.info("==> 发放单位审核状态更新成功, shopId: {}, auditStatus: {}",
                shopAuditRequest.getShopId(), shopAuditRequest.getAuditStatus());

        try {
            Response<Void> readResponse = notifyFeignClient.markApplyNotifyRead(1, shopAuditRequest.getShopId());
            checkFeignResponse(readResponse, "发放单位申请通知标记已读失败");
            log.info("==> 发放单位申请通知已标记为已读, shopId: {}", shopAuditRequest.getShopId());
        } catch (Exception e) {
            log.error("==> 调用消息服务标记通知已读失败, shopId: {}, error: {}",
                    shopAuditRequest.getShopId(), e.getMessage(), e);
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "发放单位申请通知标记已读失败");
        }
        Long adminId = StpUtil.getLoginIdAsLong();

        if (shopAuditRequest.getAuditStatus().equals(ShopAuditRequest.AUDIT_PASS)) {
            String title = "发放单位申请已通过";
            String content = String.format("恭喜！您的发放单位【%s】发放单位申请已通过审核！", shop.getShopName());

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
            String title = "发放单位申请被拒绝";
            String content = String.format("抱歉，您的发放单位【%s】发放单位申请未通过：%s",
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
     * 查询发放单位申请列表（支持多条件筛选和分页）
     */
    @Override
    public Response<PageResult<ShopApplyVO>> getShopApplyList(ShopApplyQueryRequest queryRequest) {
        log.info("==> 查询发放单位申请列表, 用户名: {}, 发放单位名: {}, 审核状态: {}",
                queryRequest.getUsername(), queryRequest.getShopName(), queryRequest.getAuditStatus());

        Integer pageNum = queryRequest.getPageNum() != null ? queryRequest.getPageNum() : 1;
        Integer pageSize = queryRequest.getPageSize() != null ? queryRequest.getPageSize() : 10;

        PageHelper.startPage(pageNum, pageSize);

        List<Shop> shopList = shopMapper.selectShopApplyList(
                queryRequest.getUsername(),
                queryRequest.getShopName(),
                queryRequest.getAuditStatus()
        );

        Page<Shop> page = (Page<Shop>) shopList;

        List<ShopApplyVO> voList = shopList.stream()
                .map(this::convertToShopApplyVO)
                .collect(Collectors.toList());

        PageResult<ShopApplyVO> pageResult = PageResult.<ShopApplyVO>builder()
                .records(voList)
                .total(page.getTotal())
                .pageNum(pageNum)
                .pageSize(pageSize)
                .totalPages((int) Math.ceil((double) page.getTotal() / pageSize))
                .build();

        log.info("==> 查询发放单位申请列表完成, 总数: {}, 当前页: {}", page.getTotal(), pageNum);

        return Response.success(pageResult);
    }

    /**
     * 禁用/启用发放单位
     */
    @Override
    public Response<?> updateShopStatus(Long shopId, Integer shopStatus) {
        log.info("==> 禁用/启用发放单位, shopId: {}, shopStatus: {}", shopId, shopStatus);

        Shop shop = shopMapper.selectByPrimaryKey(shopId);
        if (Objects.isNull(shop)) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "发放单位不存在");
        }

        if (shop.getAuditStatus() != 1) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "只有审核通过的发放单位才能修改状态");
        }

        int result = shopMapper.updateShopStatus(shopId, shopStatus);
        if (result <= 0) {
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "发放单位状态更新失败");
        }

        log.info("==> 发放单位状态更新成功, shopId: {}, shopStatus: {}", shopId, shopStatus);
        return Response.success();
    }

    /**
     * 获取发放单位列表
     */
    @Override
    public Response<PageResult<ShopApplyVO>> getMyShopList() {
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("==> 商家查看自己的发放单位列表, userId: {}", userId);

        // 查询商家自己的发放单位列表
        List<Shop> shopList = shopMapper.selectListByUserId(userId);

        PageResult<ShopApplyVO> pageResult;
        if (shopList == null || shopList.isEmpty()) {
            // 没有发放单位返回空列表
            pageResult = PageResult.<ShopApplyVO>builder()
                    .records(List.of())
                    .total(0L)
                    .pageNum(1)
                    .pageSize(10)
                    .totalPages(0)
                    .build();
        } else {
            // 转换为 VO 列表
            List<ShopApplyVO> voList = shopList.stream()
                    .map(this::convertToShopApplyVO)
                    .collect(Collectors.toList());
            pageResult = PageResult.<ShopApplyVO>builder()
                    .records(voList)
                    .total((long) voList.size())
                    .pageNum(1)
                    .pageSize(10)
                    .totalPages(1)
                    .build();
        }

        log.info("==> 商家查看自己的发放单位列表完成, userId: {}, 发放单位数量: {}", userId, pageResult.getTotal());
        return Response.success(pageResult);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<?> deleteMyShop(Long shopId) {
        if (shopId == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "发放单位ID不能为空");
        }

        Long userId = StpUtil.getLoginIdAsLong();
        log.info("==> 商家删除自己的发放单位, userId: {}, shopId: {}", userId, shopId);

        Shop shop = shopMapper.selectByPrimaryKey(shopId);
        if (Objects.isNull(shop) || (shop.getIsDelete() != null && shop.getIsDelete() == 1)) {
            throw new BizException(ResponseCodeEnum.SHOP_NOT_EXIST);
        }
        if (!userId.equals(shop.getUserId())) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "无权删除该发放单位");
        }

        try {
            Response<PageResult<GoodsListVO>> goodsResponse = goodsFeignClient.getGoodsPage(
                    shopId, 1, 1, 1, null);
            checkFeignResponse(goodsResponse, "查询发放单位物资失败");
            PageResult<GoodsListVO> pageResult = goodsResponse.getData();
            if (pageResult != null && pageResult.getTotal() != null && pageResult.getTotal() > 0) {
                throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "发放单位存在上架物资，请先下架全部物资后再删除");
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("==> 查询发放单位物资失败, shopId: {}, error: {}", shopId, e.getMessage(), e);
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "查询发放单位物资失败");
        }

        int result = shopMapper.softDeleteById(shopId);
        if (result <= 0) {
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "发放单位删除失败");
        }

        log.info("==> 商家删除发放单位成功, userId: {}, shopId: {}", userId, shopId);
        return Response.success();
    }

    /**
     * 管理员分页查询发放单位列表（支持多条件筛选）
     */
    @Override
    public Response<PageResult<ShopApplyVO>> getShopList(ShopApplyQueryRequest queryRequest) {
        // 设置默认分页参数
        Integer pageNum = queryRequest.getPageNum() != null ? queryRequest.getPageNum() : 1;
        Integer pageSize = queryRequest.getPageSize() != null ? queryRequest.getPageSize() : 10;

        // 开启分页
        PageHelper.startPage(pageNum, pageSize);

        // 查询数据
        List<Shop> shopList = shopMapper.selectShopList(
                queryRequest.getUsername(),
                queryRequest.getShopName(),
                queryRequest.getAuditStatus(),
                queryRequest.getShopStatus()
        );

        // 获取分页信息
        Page<Shop> page = (Page<Shop>) shopList;

        // 转换为 VO
        List<ShopApplyVO> voList = shopList.stream()
                .map(this::convertToShopApplyVO)
                .collect(Collectors.toList());

        // 构建分页结果
        PageResult<ShopApplyVO> pageResult = PageResult.<ShopApplyVO>builder()
                .records(voList)
                .total(page.getTotal())
                .pageNum(pageNum)
                .pageSize(pageSize)
                .totalPages((int) Math.ceil((double) page.getTotal() / pageSize))
                .build();

        log.info("==> 查询发放单位列表完成, 总数: {}, 当前页: {}", page.getTotal(), pageNum);

        return Response.success(pageResult);
    }

    /**
     * 普通用户分页查询发放单位列表（仅返回公开字段）
     */
    @Override
    public Response<PageResult<ShopListVO>> getPublicShopList(ShopApplyQueryRequest queryRequest) {
        log.info("==> 普通用户查询发放单位列表, 发放单位名: {}, 审核状态: {}, 发放单位状态: {}",
                queryRequest.getShopName(), queryRequest.getAuditStatus(), queryRequest.getShopStatus());

        Integer pageNum = queryRequest.getPageNum() != null ? queryRequest.getPageNum() : 1;
        Integer pageSize = queryRequest.getPageSize() != null ? queryRequest.getPageSize() : 10;

        PageHelper.startPage(pageNum, pageSize);
        List<Shop> shopList = shopMapper.selectShopList(
                queryRequest.getUsername(),
                queryRequest.getShopName(),
                queryRequest.getAuditStatus(),
                queryRequest.getShopStatus()
        );
        Page<Shop> page = (Page<Shop>) shopList;

        List<ShopListVO> voList = shopList.stream()
                .map(this::convertToShopListVO)
                .collect(Collectors.toList());

        PageResult<ShopListVO> pageResult = PageResult.<ShopListVO>builder()
                .records(voList)
                .total(page.getTotal())
                .pageNum(pageNum)
                .pageSize(pageSize)
                .totalPages((int) Math.ceil((double) page.getTotal() / pageSize))
                .build();

        log.info("==> 普通用户查询发放单位列表完成, 总数: {}, 当前页: {}", page.getTotal(), pageNum);
        return Response.success(pageResult);
    }

    @Override
    public Response<PageResult<GoodsListVO>> getShopGoods(Long shopId, Integer pageNum, Integer pageSize) {
        if (shopId == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "发放单位ID不能为空");
        }

        Shop shop = shopMapper.selectByPrimaryKey(shopId);
        if (Objects.isNull(shop) || (shop.getIsDelete() != null && shop.getIsDelete() == 1)) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "发放单位不存在");
        }
        if (!Integer.valueOf(1).equals(shop.getAuditStatus())) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "发放单位未通过审核");
        }
        if (!Integer.valueOf(1).equals(shop.getShopStatus())) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "发放单位已关闭");
        }

        pageNum = pageNum == null ? 1 : pageNum;
        pageSize = pageSize == null ? 10 : pageSize;

        try {
            Response<PageResult<GoodsListVO>> goodsResponse = goodsFeignClient.getGoodsPage(
                    shopId, pageNum, pageSize, 1, 1);
            checkFeignResponse(goodsResponse, "查询发放单位物资失败");
            log.info("==> 查询发放单位物资成功, shopId: {}, total: {}", shopId,
                    goodsResponse.getData() != null ? goodsResponse.getData().getTotal() : 0);
            return Response.success(goodsResponse.getData());
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("==> 查询发放单位物资失败, shopId: {}, error: {}", shopId, e.getMessage(), e);
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "查询发放单位物资失败");
        }
    }

    @Override
    public Response<ShopListVO> getShopById(Long shopId) {
        if (shopId == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "发放单位ID不能为空");
        }
        Shop shop = shopMapper.selectByPrimaryKey(shopId);
        if (Objects.isNull(shop) || (shop.getIsDelete() != null && shop.getIsDelete() == 1)) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "发放单位不存在");
        }
        return Response.success(convertToShopListVO(shop));
    }

    /**
     * 将 Shop 转换为 ShopApplyVO
     */
    private ShopApplyVO convertToShopApplyVO(Shop shop) {
        return ShopApplyVO.builder()
                .id(shop.getId())
                .shopName(shop.getShopName())
                .shopAvatar(shop.getShopAvatar())
                .shopDesc(shop.getShopDesc())
                .userId(shop.getUserId())
                .username(shop.getUsername())
                .auditStatus(shop.getAuditStatus())
                .auditStatusDesc(getAuditStatusDesc(shop.getAuditStatus()))
                .auditRemark(shop.getAuditRemark())
                .shopStatus(shop.getShopStatus())
                .createTime(shop.getCreateTime() != null ?
                        LocalDateTime.ofInstant(shop.getCreateTime().toInstant(), ZoneId.systemDefault()) : null)
                .updateTime(shop.getUpdateTime() != null ?
                        LocalDateTime.ofInstant(shop.getUpdateTime().toInstant(), ZoneId.systemDefault()) : null)
                .build();
    }

    private ShopListVO convertToShopListVO(Shop shop) {
        return ShopListVO.builder()
                .id(shop.getId())
                .shopName(shop.getShopName())
                .shopAvatar(shop.getShopAvatar())
                .shopDesc(shop.getShopDesc())
                .shopStatus(shop.getShopStatus())
                .build();
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
     * 上传单位头像
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
            log.info("==> 单位头像上传并更新成功, shopId: {}, avatarUrl: {}",
                    existingShop.getId(), uploadResult.getImageUrl());
        } else {
            log.info("==> 申请发放单位前上传单位头像, userId: {}, avatarUrl: {}",
                    userId, uploadResult.getImageUrl());
        }

        return Response.success(uploadResult);
    }

    /**
     * 发放单位申请写入 t_shop.username：优先昵称，否则用手机号（脱敏）
     */
    private String resolveApplicantUsername(Long userId) {
        try {
            Response<UserProfileVO> userResponse = userFeignClient.getUserById(userId);
            if (userResponse != null && userResponse.isSuccess() && userResponse.getData() != null) {
                UserProfileVO profile = userResponse.getData();
                if (StrUtil.isNotBlank(profile.getNickname())) {
                    return profile.getNickname().trim();
                }
                if (StrUtil.isNotBlank(profile.getMobile())) {
                    return profile.getMobile().trim();
                }
            }
        } catch (Exception e) {
            log.warn("==> 获取申请人用户名失败，userId: {}, error: {}", userId, e.getMessage());
        }
        return "";
    }

    private void checkFeignResponse(Response<?> response, String errorMessage) {
        if (response == null || !response.isSuccess()) {
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, errorMessage);
        }
    }
}
