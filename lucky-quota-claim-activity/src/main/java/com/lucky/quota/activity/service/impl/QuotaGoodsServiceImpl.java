package com.lucky.quota.activity.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.lucky.quota.activity.enums.QuotaActivityStatusEnum;
import com.lucky.quota.activity.enums.QuotaGoodsAuditStatusEnum;
import com.lucky.quota.activity.mapper.QuotaActivityMapper;
import com.lucky.quota.activity.mapper.QuotaGoodsMapper;
import com.lucky.quota.activity.model.dto.QuotaGoodsApplyQueryRequest;
import com.lucky.quota.activity.model.dto.QuotaGoodsApplyRequest;
import com.lucky.quota.activity.model.dto.QuotaGoodsAuditRequest;
import com.lucky.quota.activity.model.entity.QuotaActivity;
import com.lucky.quota.activity.model.entity.QuotaGoods;
import com.lucky.quota.activity.model.vo.QuotaGoodsApplyVO;
import com.lucky.quota.activity.support.QuotaActivityPurchaseValidator;
import com.lucky.quota.activity.service.QuotaGoodsService;
import com.lucky.quota.activity.service.QuotaRedisService;
import com.lucky.quota.common.constant.NotifyTypeConstants;
import com.lucky.quota.common.enums.ResponseCodeEnum;
import com.lucky.quota.common.exception.BizException;
import com.lucky.quota.common.model.entity.PageResult;
import com.lucky.quota.common.utils.Response;
import com.lucky.quota.goods.feign.GoodsFeignClient;
import com.lucky.quota.goods.model.entity.Goods;
import com.lucky.quota.goods.model.entity.GoodsSku;
import com.lucky.quota.notify.feign.NotifyFeignClient;
import com.lucky.quota.notify.model.entity.Notify;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class QuotaGoodsServiceImpl implements QuotaGoodsService {

    private static final Long ADMIN_USER_ID = 5L;

    @Resource
    private QuotaGoodsMapper seckillGoodsMapper;

    @Resource
    private QuotaActivityMapper seckillActivityMapper;

    @Resource
    private GoodsFeignClient goodsFeignClient;

    @Resource
    private NotifyFeignClient notifyFeignClient;

    @Resource
    private QuotaRedisService seckillRedisService;

    @Resource
    private QuotaActivityPurchaseValidator seckillActivityPurchaseValidator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<Long> applyQuotaGoods(QuotaGoodsApplyRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();

        QuotaActivity activity = seckillActivityMapper.selectByPrimaryKey(request.getActivityId());
        if (activity == null) {
            throw new BizException(ResponseCodeEnum.QUOTA_ACTIVITY_NOT_EXIST);
        }
        if (!QuotaActivityStatusEnum.NOT_STARTED.getCode().equals(activity.getStatus())) {
            throw new BizException(ResponseCodeEnum.QUOTA_ACTIVITY_ALREADY_STARTED, "仅未开始的活动可申请加入限量物资");
        }

        Goods goods = requireGoods(request.getGoodsId());
        if (!userId.equals(goods.getUserId())) {
            throw new BizException(ResponseCodeEnum.USER_PERMISSION_DENIED, "只能为自己的物资申请限量申领");
        }
        if (goods.getStatus() == null || goods.getStatus() != 1) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "物资未上架，无法申请限量申领");
        }
        if (goods.getAuditStatus() == null || goods.getAuditStatus() != 2) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "物资未通过上架审核，无法申请限量申领");
        }

        GoodsSku sku = requireSku(request.getSkuId());
        if (!request.getGoodsId().equals(sku.getGoodsId())) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "所选规格不属于该物资");
        }

        QuotaGoods existing = seckillGoodsMapper.selectByActivityAndGoodsAndSku(
                request.getActivityId(), request.getGoodsId(), request.getSkuId());
        if (existing != null) {
            Integer auditStatus = existing.getAuditStatus();
            if (QuotaGoodsAuditStatusEnum.PENDING.getCode().equals(auditStatus)) {
                throw new BizException(ResponseCodeEnum.QUOTA_GOODS_APPLY_PENDING);
            }
            if (QuotaGoodsAuditStatusEnum.APPROVED.getCode().equals(auditStatus)) {
                throw new BizException(ResponseCodeEnum.QUOTA_GOODS_ALREADY_EXISTS);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        long seckillGoodsId = IdUtil.getSnowflakeNextId();
        String title = request.getSeckillTitle();
        if (title == null || title.isBlank()) {
            title = goods.getGoodsName();
        }
        String img = request.getSeckillImg();
        if (img == null || img.isBlank()) {
            img = sku.getSkuImg() != null && !sku.getSkuImg().isBlank()
                    ? sku.getSkuImg() : goods.getGoodsImg();
        }

        QuotaGoods seckillGoods = QuotaGoods.builder()
                .id(seckillGoodsId)
                .activityId(request.getActivityId())
                .goodsId(request.getGoodsId())
                .skuId(request.getSkuId())
                .skuSpecName(sku.getSkuSpecName())
                .seckillTitle(title)
                .seckillImg(img)
                .seckillPrice(request.getSeckillPrice())
                .seckillTotal(request.getSeckillTotal())
                .seckillStock(request.getSeckillTotal())
                .seckillLimit(request.getSeckillLimit() == null ? 1 : request.getSeckillLimit())
                .sort(request.getSort() == null ? 0 : request.getSort())
                .auditStatus(QuotaGoodsAuditStatusEnum.PENDING.getCode())
                .version(0)
                .isDeleted(0)
                .createTime(now)
                .updateTime(now)
                .build();

        int rows = seckillGoodsMapper.insertSelective(seckillGoods);
        if (rows == 0) {
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "限量投放申请提交失败");
        }

        sendApplyNotify(userId, seckillGoodsId, title, activity.getActivityName());
        log.info("==> 限量投放申请提交成功, seckillGoodsId={}, goodsId={}, skuId={}, activityId={}",
                seckillGoodsId, request.getGoodsId(), request.getSkuId(), request.getActivityId());
        return Response.success(seckillGoodsId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<Void> auditQuotaGoods(QuotaGoodsAuditRequest request) {
        Long adminId = StpUtil.getLoginIdAsLong();

        QuotaGoods seckillGoods = seckillGoodsMapper.selectByPrimaryKey(request.getSeckillGoodsId());
        if (seckillGoods == null) {
            throw new BizException(ResponseCodeEnum.QUOTA_GOODS_NOT_EXIST);
        }
        if (!QuotaGoodsAuditStatusEnum.PENDING.getCode().equals(seckillGoods.getAuditStatus())) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "该限量物资已审核，请勿重复操作");
        }

        if (!QuotaGoodsAuditRequest.AUDIT_PASS.equals(request.getAuditStatus())
                && !QuotaGoodsAuditRequest.AUDIT_REJECT.equals(request.getAuditStatus())) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "审核状态无效");
        }
        if (QuotaGoodsAuditRequest.AUDIT_REJECT.equals(request.getAuditStatus())
                && (request.getAuditRemark() == null || request.getAuditRemark().isBlank())) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "拒绝时请填写审核备注");
        }

        int rows = seckillGoodsMapper.updateAuditStatus(
                request.getSeckillGoodsId(),
                request.getAuditStatus(),
                request.getAuditRemark(),
                LocalDateTime.now());
        if (rows == 0) {
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "限量物资审核状态更新失败");
        }

        markApplyNotifyRead(request.getSeckillGoodsId());
        sendAuditResultNotify(adminId, seckillGoods, request);

        if (QuotaGoodsAuditRequest.AUDIT_PASS.equals(request.getAuditStatus())) {
            syncApprovedGoodsToRedis(seckillGoods.getActivityId(), request.getSeckillGoodsId());
        }

        log.info("==> 限量物资审核完成, seckillGoodsId={}, auditStatus={}",
                request.getSeckillGoodsId(), request.getAuditStatus());
        return Response.success();
    }

    @Override
    public Response<PageResult<QuotaGoodsApplyVO>> listApplyPage(QuotaGoodsApplyQueryRequest request) {
        int pageNum = request.getPageNum() == null || request.getPageNum() < 1 ? 1 : request.getPageNum();
        int pageSize = request.getPageSize() == null || request.getPageSize() < 1 ? 10 : request.getPageSize();

        try (Page<QuotaGoods> page = PageHelper.startPage(pageNum, pageSize)) {
            List<QuotaGoods> list = seckillGoodsMapper.selectApplyPage(request);
            List<QuotaGoodsApplyVO> voList = list.stream()
                    .map(item -> {
                        QuotaGoodsApplyVO vo = QuotaGoodsApplyVO.fromEntity(item);
                        QuotaActivity activity = seckillActivityMapper.selectByPrimaryKey(item.getActivityId());
                        if (activity != null) {
                            vo.setActivityName(activity.getActivityName());
                        }
                        Goods ownerGoods = requireGoods(item.getGoodsId());
                        vo.setUserId(ownerGoods.getUserId());
                        return vo;
                    })
                    .collect(Collectors.toList());

            PageResult<QuotaGoodsApplyVO> pageResult = PageResult.<QuotaGoodsApplyVO>builder()
                    .records(voList)
                    .total(page.getTotal())
                    .pageNum(pageNum)
                    .pageSize(pageSize)
                    .totalPages((int) Math.ceil((double) page.getTotal() / pageSize))
                    .build();
            return Response.success(pageResult);
        }
    }

    @Override
    public Response<QuotaGoodsApplyVO> getQuotaGoodsDetail(Long seckillGoodsId) {
        if (seckillGoodsId == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "限量物资ID不能为空");
        }
        QuotaGoods seckillGoods = seckillGoodsMapper.selectByPrimaryKey(seckillGoodsId);
        if (seckillGoods == null) {
            throw new BizException(ResponseCodeEnum.QUOTA_GOODS_NOT_EXIST);
        }
        if (!QuotaGoodsAuditStatusEnum.APPROVED.getCode().equals(seckillGoods.getAuditStatus())) {
            throw new BizException(ResponseCodeEnum.QUOTA_GOODS_NOT_APPROVED);
        }

        QuotaGoodsApplyVO vo = QuotaGoodsApplyVO.fromEntity(seckillGoods);
        QuotaActivity activity = seckillActivityMapper.selectByPrimaryKey(seckillGoods.getActivityId());
        if (activity != null) {
            vo.setActivityName(activity.getActivityName());
            vo.setSeckillPurchasable(isQuotaPurchasable(activity));
        } else {
            vo.setSeckillPurchasable(false);
        }
        vo.setUserId(requireGoods(seckillGoods.getGoodsId()).getUserId());
        return Response.success(vo);
    }

    private boolean isQuotaPurchasable(QuotaActivity activity) {
        try {
            seckillActivityPurchaseValidator.assertCanQuotaPurchase(activity);
            return true;
        } catch (BizException e) {
            return false;
        }
    }

    private Goods requireGoods(Long goodsId) {
        Response<Goods> response = goodsFeignClient.getGoodsById(goodsId);
        if (response == null || !response.isSuccess() || response.getData() == null) {
            throw new BizException(ResponseCodeEnum.GOODS_NOT_EXIST);
        }
        return response.getData();
    }

    private GoodsSku requireSku(Long skuId) {
        Response<GoodsSku> response = goodsFeignClient.getSkuById(skuId);
        if (response == null || !response.isSuccess() || response.getData() == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "SKU不存在");
        }
        return response.getData();
    }

    /**
     * 活动进行中且已预热时，将新审核通过的物资库存写入 Redis，避免活动页只能看不能抢。
     */
    private void syncApprovedGoodsToRedis(Long activityId, Long seckillGoodsId) {
        QuotaActivity activity = seckillActivityMapper.selectByPrimaryKey(activityId);
        if (activity == null
                || !QuotaActivityStatusEnum.IN_PROGRESS.getCode().equals(activity.getStatus())
                || !seckillRedisService.isWarmedUp(activityId)) {
            return;
        }
        QuotaGoods approved = seckillGoodsMapper.selectByPrimaryKey(seckillGoodsId);
        if (approved != null) {
            seckillRedisService.supplementGoodsStock(activityId, approved);
        }
    }

    private void sendApplyNotify(Long userId, Long seckillGoodsId, String title, String activityName) {
        Notify notify = Notify.builder()
                .id(IdUtil.getSnowflakeNextId())
                .sendUserId(userId)
                .receiveUserId(ADMIN_USER_ID)
                .title("限量投放申请")
                .content(String.format("物资【%s】申请加入限量场次【%s】，请到限量申领审核界面处理", title, activityName))
                .notifyType(NotifyTypeConstants.QUOTA_GOODS_APPLY)
                .businessId(seckillGoodsId)
                .subType(1)
                .isRead(0)
                .isDelete(0)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        try {
            Response<Boolean> notifyResponse = notifyFeignClient.saveNotify(notify);
            if (notifyResponse == null || !notifyResponse.isSuccess()) {
                throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "限量投放申请通知发送失败");
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("==> 限量投放申请通知发送失败, seckillGoodsId={}", seckillGoodsId, e);
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "限量投放申请通知发送失败");
        }
    }

    private void markApplyNotifyRead(Long seckillGoodsId) {
        try {
            notifyFeignClient.markApplyNotifyRead(NotifyTypeConstants.QUOTA_GOODS_APPLY, seckillGoodsId);
        } catch (Exception e) {
            log.warn("==> 限量申领申请通知标记已读失败, seckillGoodsId={}", seckillGoodsId, e);
        }
    }

    private void sendAuditResultNotify(Long adminId, QuotaGoods seckillGoods, QuotaGoodsAuditRequest request) {
        boolean approved = QuotaGoodsAuditRequest.AUDIT_PASS.equals(request.getAuditStatus());
        String title = approved ? "限量投放申请已通过" : "限量投放申请被拒绝";
        String content = approved
                ? String.format("恭喜！您的限量物资【%s】已通过审核，活动开始后将参与限量申领", seckillGoods.getSeckillTitle())
                : String.format("抱歉，您的限量物资【%s】未通过审核：%s",
                seckillGoods.getSeckillTitle(), request.getAuditRemark());

        Long merchantUserId = requireGoods(seckillGoods.getGoodsId()).getUserId();
        Notify notify = Notify.builder()
                .id(IdUtil.getSnowflakeNextId())
                .sendUserId(adminId)
                .receiveUserId(merchantUserId)
                .title(title)
                .content(content)
                .notifyType(NotifyTypeConstants.AUDIT_NOTIFY)
                .businessId(seckillGoods.getId())
                .subType(3)
                .isRead(0)
                .isDelete(0)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        try {
            Response<Boolean> response = notifyFeignClient.saveNotify(notify);
            if (response == null || !response.isSuccess()) {
                throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "限量申领审核结果通知发送失败");
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("==> 限量申领审核结果通知发送失败, seckillGoodsId={}", seckillGoods.getId(), e);
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "限量申领审核结果通知发送失败");
        }
    }
}
