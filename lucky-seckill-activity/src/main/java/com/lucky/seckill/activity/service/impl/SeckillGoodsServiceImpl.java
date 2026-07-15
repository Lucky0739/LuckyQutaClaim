package com.lucky.seckill.activity.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.lucky.seckill.activity.enums.SeckillActivityStatusEnum;
import com.lucky.seckill.activity.enums.SeckillGoodsAuditStatusEnum;
import com.lucky.seckill.activity.mapper.SeckillActivityMapper;
import com.lucky.seckill.activity.mapper.SeckillGoodsMapper;
import com.lucky.seckill.activity.model.dto.SeckillGoodsApplyQueryRequest;
import com.lucky.seckill.activity.model.dto.SeckillGoodsApplyRequest;
import com.lucky.seckill.activity.model.dto.SeckillGoodsAuditRequest;
import com.lucky.seckill.activity.model.entity.SeckillActivity;
import com.lucky.seckill.activity.model.entity.SeckillGoods;
import com.lucky.seckill.activity.model.vo.SeckillGoodsApplyVO;
import com.lucky.seckill.activity.support.SeckillActivityPurchaseValidator;
import com.lucky.seckill.activity.service.SeckillGoodsService;
import com.lucky.seckill.activity.service.SeckillRedisService;
import com.lucky.seckill.common.constant.NotifyTypeConstants;
import com.lucky.seckill.common.enums.ResponseCodeEnum;
import com.lucky.seckill.common.exception.BizException;
import com.lucky.seckill.common.model.entity.PageResult;
import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.goods.feign.GoodsFeignClient;
import com.lucky.seckill.goods.model.entity.Goods;
import com.lucky.seckill.goods.model.entity.GoodsSku;
import com.lucky.seckill.notify.feign.NotifyFeignClient;
import com.lucky.seckill.notify.model.entity.Notify;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SeckillGoodsServiceImpl implements SeckillGoodsService {

    private static final Long ADMIN_USER_ID = 5L;

    @Resource
    private SeckillGoodsMapper seckillGoodsMapper;

    @Resource
    private SeckillActivityMapper seckillActivityMapper;

    @Resource
    private GoodsFeignClient goodsFeignClient;

    @Resource
    private NotifyFeignClient notifyFeignClient;

    @Resource
    private SeckillRedisService seckillRedisService;

    @Resource
    private SeckillActivityPurchaseValidator seckillActivityPurchaseValidator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<Long> applySeckillGoods(SeckillGoodsApplyRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();

        SeckillActivity activity = seckillActivityMapper.selectByPrimaryKey(request.getActivityId());
        if (activity == null) {
            throw new BizException(ResponseCodeEnum.SECKILL_ACTIVITY_NOT_EXIST);
        }
        if (!SeckillActivityStatusEnum.NOT_STARTED.getCode().equals(activity.getStatus())) {
            throw new BizException(ResponseCodeEnum.SECKILL_ACTIVITY_ALREADY_STARTED, "仅未开始的活动可申请加入秒杀商品");
        }

        Goods goods = requireGoods(request.getGoodsId());
        if (!userId.equals(goods.getUserId())) {
            throw new BizException(ResponseCodeEnum.USER_PERMISSION_DENIED, "只能为自己的商品申请秒杀");
        }
        if (goods.getStatus() == null || goods.getStatus() != 1) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "商品未上架，无法申请秒杀");
        }
        if (goods.getAuditStatus() == null || goods.getAuditStatus() != 2) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "商品未通过上架审核，无法申请秒杀");
        }

        GoodsSku sku = requireSku(request.getSkuId());
        if (!request.getGoodsId().equals(sku.getGoodsId())) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "所选规格不属于该商品");
        }

        SeckillGoods existing = seckillGoodsMapper.selectByActivityAndGoodsAndSku(
                request.getActivityId(), request.getGoodsId(), request.getSkuId());
        if (existing != null) {
            Integer auditStatus = existing.getAuditStatus();
            if (SeckillGoodsAuditStatusEnum.PENDING.getCode().equals(auditStatus)) {
                throw new BizException(ResponseCodeEnum.SECKILL_GOODS_APPLY_PENDING);
            }
            if (SeckillGoodsAuditStatusEnum.APPROVED.getCode().equals(auditStatus)) {
                throw new BizException(ResponseCodeEnum.SECKILL_GOODS_ALREADY_EXISTS);
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

        SeckillGoods seckillGoods = SeckillGoods.builder()
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
                .auditStatus(SeckillGoodsAuditStatusEnum.PENDING.getCode())
                .version(0)
                .isDeleted(0)
                .createTime(now)
                .updateTime(now)
                .build();

        int rows = seckillGoodsMapper.insertSelective(seckillGoods);
        if (rows == 0) {
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "秒杀商品申请提交失败");
        }

        sendApplyNotify(userId, seckillGoodsId, title, activity.getActivityName());
        log.info("==> 秒杀商品申请提交成功, seckillGoodsId={}, goodsId={}, skuId={}, activityId={}",
                seckillGoodsId, request.getGoodsId(), request.getSkuId(), request.getActivityId());
        return Response.success(seckillGoodsId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<Void> auditSeckillGoods(SeckillGoodsAuditRequest request) {
        Long adminId = StpUtil.getLoginIdAsLong();

        SeckillGoods seckillGoods = seckillGoodsMapper.selectByPrimaryKey(request.getSeckillGoodsId());
        if (seckillGoods == null) {
            throw new BizException(ResponseCodeEnum.SECKILL_GOODS_NOT_EXIST);
        }
        if (!SeckillGoodsAuditStatusEnum.PENDING.getCode().equals(seckillGoods.getAuditStatus())) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "该秒杀商品已审核，请勿重复操作");
        }

        if (!SeckillGoodsAuditRequest.AUDIT_PASS.equals(request.getAuditStatus())
                && !SeckillGoodsAuditRequest.AUDIT_REJECT.equals(request.getAuditStatus())) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "审核状态无效");
        }
        if (SeckillGoodsAuditRequest.AUDIT_REJECT.equals(request.getAuditStatus())
                && (request.getAuditRemark() == null || request.getAuditRemark().isBlank())) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "拒绝时请填写审核备注");
        }

        int rows = seckillGoodsMapper.updateAuditStatus(
                request.getSeckillGoodsId(),
                request.getAuditStatus(),
                request.getAuditRemark(),
                LocalDateTime.now());
        if (rows == 0) {
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "秒杀商品审核状态更新失败");
        }

        markApplyNotifyRead(request.getSeckillGoodsId());
        sendAuditResultNotify(adminId, seckillGoods, request);

        if (SeckillGoodsAuditRequest.AUDIT_PASS.equals(request.getAuditStatus())) {
            syncApprovedGoodsToRedis(seckillGoods.getActivityId(), request.getSeckillGoodsId());
        }

        log.info("==> 秒杀商品审核完成, seckillGoodsId={}, auditStatus={}",
                request.getSeckillGoodsId(), request.getAuditStatus());
        return Response.success();
    }

    @Override
    public Response<PageResult<SeckillGoodsApplyVO>> listApplyPage(SeckillGoodsApplyQueryRequest request) {
        int pageNum = request.getPageNum() == null || request.getPageNum() < 1 ? 1 : request.getPageNum();
        int pageSize = request.getPageSize() == null || request.getPageSize() < 1 ? 10 : request.getPageSize();

        try (Page<SeckillGoods> page = PageHelper.startPage(pageNum, pageSize)) {
            List<SeckillGoods> list = seckillGoodsMapper.selectApplyPage(request);
            List<SeckillGoodsApplyVO> voList = list.stream()
                    .map(item -> {
                        SeckillGoodsApplyVO vo = SeckillGoodsApplyVO.fromEntity(item);
                        SeckillActivity activity = seckillActivityMapper.selectByPrimaryKey(item.getActivityId());
                        if (activity != null) {
                            vo.setActivityName(activity.getActivityName());
                        }
                        Goods ownerGoods = requireGoods(item.getGoodsId());
                        vo.setUserId(ownerGoods.getUserId());
                        return vo;
                    })
                    .collect(Collectors.toList());

            PageResult<SeckillGoodsApplyVO> pageResult = PageResult.<SeckillGoodsApplyVO>builder()
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
    public Response<SeckillGoodsApplyVO> getSeckillGoodsDetail(Long seckillGoodsId) {
        if (seckillGoodsId == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "秒杀商品ID不能为空");
        }
        SeckillGoods seckillGoods = seckillGoodsMapper.selectByPrimaryKey(seckillGoodsId);
        if (seckillGoods == null) {
            throw new BizException(ResponseCodeEnum.SECKILL_GOODS_NOT_EXIST);
        }
        if (!SeckillGoodsAuditStatusEnum.APPROVED.getCode().equals(seckillGoods.getAuditStatus())) {
            throw new BizException(ResponseCodeEnum.SECKILL_GOODS_NOT_APPROVED);
        }

        SeckillGoodsApplyVO vo = SeckillGoodsApplyVO.fromEntity(seckillGoods);
        SeckillActivity activity = seckillActivityMapper.selectByPrimaryKey(seckillGoods.getActivityId());
        if (activity != null) {
            vo.setActivityName(activity.getActivityName());
            vo.setSeckillPurchasable(isSeckillPurchasable(activity));
        } else {
            vo.setSeckillPurchasable(false);
        }
        vo.setUserId(requireGoods(seckillGoods.getGoodsId()).getUserId());
        return Response.success(vo);
    }

    private boolean isSeckillPurchasable(SeckillActivity activity) {
        try {
            seckillActivityPurchaseValidator.assertCanSeckillPurchase(activity);
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
     * 活动进行中且已预热时，将新审核通过的商品库存写入 Redis，避免活动页只能看不能抢。
     */
    private void syncApprovedGoodsToRedis(Long activityId, Long seckillGoodsId) {
        SeckillActivity activity = seckillActivityMapper.selectByPrimaryKey(activityId);
        if (activity == null
                || !SeckillActivityStatusEnum.IN_PROGRESS.getCode().equals(activity.getStatus())
                || !seckillRedisService.isWarmedUp(activityId)) {
            return;
        }
        SeckillGoods approved = seckillGoodsMapper.selectByPrimaryKey(seckillGoodsId);
        if (approved != null) {
            seckillRedisService.supplementGoodsStock(activityId, approved);
        }
    }

    private void sendApplyNotify(Long userId, Long seckillGoodsId, String title, String activityName) {
        Notify notify = Notify.builder()
                .id(IdUtil.getSnowflakeNextId())
                .sendUserId(userId)
                .receiveUserId(ADMIN_USER_ID)
                .title("秒杀商品申请")
                .content(String.format("商品【%s】申请加入秒杀活动【%s】，请到秒杀审核界面处理", title, activityName))
                .notifyType(NotifyTypeConstants.SECKILL_GOODS_APPLY)
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
                throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "秒杀商品申请通知发送失败");
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("==> 秒杀商品申请通知发送失败, seckillGoodsId={}", seckillGoodsId, e);
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "秒杀商品申请通知发送失败");
        }
    }

    private void markApplyNotifyRead(Long seckillGoodsId) {
        try {
            notifyFeignClient.markApplyNotifyRead(NotifyTypeConstants.SECKILL_GOODS_APPLY, seckillGoodsId);
        } catch (Exception e) {
            log.warn("==> 秒杀申请通知标记已读失败, seckillGoodsId={}", seckillGoodsId, e);
        }
    }

    private void sendAuditResultNotify(Long adminId, SeckillGoods seckillGoods, SeckillGoodsAuditRequest request) {
        boolean approved = SeckillGoodsAuditRequest.AUDIT_PASS.equals(request.getAuditStatus());
        String title = approved ? "秒杀商品申请已通过" : "秒杀商品申请被拒绝";
        String content = approved
                ? String.format("恭喜！您的秒杀商品【%s】已通过审核，活动开始后将参与秒杀", seckillGoods.getSeckillTitle())
                : String.format("抱歉，您的秒杀商品【%s】未通过审核：%s",
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
                throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "秒杀审核结果通知发送失败");
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("==> 秒杀审核结果通知发送失败, seckillGoodsId={}", seckillGoods.getId(), e);
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "秒杀审核结果通知发送失败");
        }
    }
}
