package com.lucky.seckill.cart.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import com.lucky.seckill.cart.constant.CartConstants;
import com.lucky.seckill.cart.mapper.CartMapper;
import com.lucky.seckill.cart.model.dto.AddCartRequest;
import com.lucky.seckill.cart.model.dto.BatchDeleteCartRequest;
import com.lucky.seckill.cart.model.entity.Cart;
import com.lucky.seckill.cart.model.vo.CartListVO;
import com.lucky.seckill.cart.model.vo.CartSummaryVO;
import com.lucky.seckill.cart.model.vo.CartVO;
import com.lucky.seckill.cart.service.CartService;
import com.lucky.seckill.common.enums.ResponseCodeEnum;
import com.lucky.seckill.common.exception.BizException;
import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.goods.feign.GoodsFeignClient;
import com.lucky.seckill.goods.model.dto.GoodsAuditRequest;
import com.lucky.seckill.goods.model.entity.Goods;
import com.lucky.seckill.goods.model.entity.GoodsSku;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class CartServiceImpl implements CartService {

    @Resource
    private CartMapper cartMapper;

    @Resource
    private GoodsFeignClient goodsFeignClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<Void> addCart(AddCartRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("==> 添加商品到购物车, userId: {}, goodsId: {}, skuId: {}, goodsCount: {}",
                userId, request.getGoodsId(), request.getSkuId(), request.getGoodsCount());

        Cart existCart = cartMapper.selectByUserIdAndSkuId(userId, request.getSkuId());
        int targetCount = existCart == null
                ? request.getGoodsCount()
                : existCart.getGoodsCount() + request.getGoodsCount();

        validateGoodsAndSku(request.getGoodsId(), request.getSkuId(), targetCount);

        if (existCart != null) {
            int rows = cartMapper.updateGoodsCountByUserIdAndSkuId(userId, request.getSkuId(), targetCount);
            if (rows == 0) {
                throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "更新购物车失败");
            }
            log.info("==> 购物车商品数量累加成功, userId: {}, skuId: {}, newGoodsCount: {}",
                    userId, request.getSkuId(), targetCount);
        } else {
            long cartId = IdUtil.getSnowflakeNextId();
            Cart cart = Cart.builder()
                    .id(cartId)
                    .userId(userId)
                    .goodsId(request.getGoodsId())
                    .skuId(request.getSkuId())
                    .goodsCount(request.getGoodsCount())
                    .selected(1)
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();

            int rows = cartMapper.insertSelective(cart);
            if (rows == 0) {
                throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "添加购物车失败");
            }
            log.info("==> 新增购物车商品成功, userId: {}, cartId: {}", userId, cartId);
        }

        return Response.success();
    }

    @Override
    public Response<CartListVO> getCartList() {
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("==> 查询购物车列表, userId: {}", userId);

        List<CartVO> cartVOList = cartMapper.selectCartVOListByUserId(userId);
        if (cartVOList == null || cartVOList.isEmpty()) {
            return Response.success(buildEmptyCartList());
        }

        for (CartVO cartVO : cartVOList) {
            enrichCartItem(cartVO);
        }

        CartListVO result = CartListVO.builder()
                .items(cartVOList)
                .summary(buildSummary(cartVOList))
                .build();
        log.info("==> 查询购物车列表成功, userId: {}, count: {}", userId, cartVOList.size());
        return Response.success(result);
    }

    @Override
    public Response<Integer> getCartCount() {
        if (!StpUtil.isLogin()) {
            return Response.success(0);
        }
        Long userId = StpUtil.getLoginIdAsLong();
        int count = cartMapper.sumGoodsCountByUserId(userId);
        return Response.success(count);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<Void> deleteCart(Long cartId) {
        if (cartId == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "购物车ID不能为空");
        }

        Long userId = StpUtil.getLoginIdAsLong();
        log.info("==> 删除购物车商品, userId: {}, cartId: {}", userId, cartId);

        Cart cart = cartMapper.selectById(cartId);
        if (cart == null || !cart.getUserId().equals(userId)) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "购物车商品不存在");
        }

        int rows = cartMapper.deleteById(cartId);
        if (rows == 0) {
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "删除失败");
        }

        log.info("==> 删除购物车商品成功, userId: {}, cartId: {}", userId, cartId);
        return Response.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<Void> updateQuantity(Long cartId, Integer goodsCount) {
        if (cartId == null || goodsCount == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "参数不能为空");
        }

        if (goodsCount < 1) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "商品数量至少为1");
        }

        if (goodsCount > CartConstants.MAX_GOODS_COUNT_PER_ITEM) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID,
                    "单件商品购买数量不能超过" + CartConstants.MAX_GOODS_COUNT_PER_ITEM);
        }

        Long userId = StpUtil.getLoginIdAsLong();
        log.info("==> 更新购物车商品数量, userId: {}, cartId: {}, goodsCount: {}",
                userId, cartId, goodsCount);

        Cart cart = cartMapper.selectById(cartId);
        if (cart == null || !cart.getUserId().equals(userId)) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "购物车商品不存在");
        }

        validateGoodsAndSku(cart.getGoodsId(), cart.getSkuId(), goodsCount);

        int rows = cartMapper.updateGoodsCountById(cartId, goodsCount);
        if (rows == 0) {
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "更新失败");
        }

        log.info("==> 更新购物车商品数量成功, userId: {}, cartId: {}", userId, cartId);
        return Response.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<Void> selectCart(Long cartId, Integer selected) {
        if (cartId == null || selected == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "参数不能为空");
        }

        if (selected != 0 && selected != 1) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "选中状态只能是0或1");
        }

        Long userId = StpUtil.getLoginIdAsLong();
        log.info("==> 选中/取消选中购物车商品, userId: {}, cartId: {}, selected: {}",
                userId, cartId, selected);

        Cart cart = cartMapper.selectById(cartId);
        if (cart == null || !cart.getUserId().equals(userId)) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "购物车商品不存在");
        }

        if (selected == 1) {
            validateGoodsAndSku(cart.getGoodsId(), cart.getSkuId(), cart.getGoodsCount());
        }

        int rows = cartMapper.updateSelectedById(cartId, selected);
        if (rows == 0) {
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "更新失败");
        }

        log.info("==> 选中/取消选中购物车商品成功, userId: {}, cartId: {}", userId, cartId);
        return Response.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<Void> selectAll(Integer selected) {
        if (selected == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "选中状态不能为空");
        }

        if (selected != 0 && selected != 1) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "选中状态只能是0或1");
        }

        Long userId = StpUtil.getLoginIdAsLong();
        log.info("==> 全选/取消全选购物车, userId: {}, selected: {}", userId, selected);

        if (selected == 1) {
            List<CartVO> cartVOList = cartMapper.selectCartVOListByUserId(userId);
            for (CartVO cartVO : cartVOList) {
                enrichCartItem(cartVO);
            }
            long invalidCount = cartVOList.stream()
                    .filter(item -> !Boolean.TRUE.equals(item.getValid()))
                    .count();
            if (invalidCount > 0) {
                throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID,
                        "存在不可购买商品，无法全选");
            }
        }

        int rows = cartMapper.updateAllSelectedByUserId(userId, selected);
        log.info("==> 全选/取消全选购物车成功, userId: {}, affectedRows: {}", userId, rows);
        return Response.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<Void> batchDeleteCart(List<Long> cartIds) {
        if (cartIds == null || cartIds.isEmpty()) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "购物车ID列表不能为空");
        }

        Long userId = StpUtil.getLoginIdAsLong();
        log.info("==> 批量删除购物车商品, userId: {}, cartIds: {}", userId, cartIds);

        int rows = cartMapper.batchDeleteByIds(userId, cartIds);
        log.info("==> 批量删除购物车商品成功, userId: {}, deletedCount: {}", userId, rows);
        return Response.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<Void> batchDeleteCart(BatchDeleteCartRequest request) {
        return batchDeleteCart(request.getCartIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<Void> deleteSelected() {
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("==> 删除已选购物车商品, userId: {}", userId);
        int rows = cartMapper.deleteSelectedByUserId(userId);
        log.info("==> 删除已选购物车商品成功, userId: {}, deletedCount: {}", userId, rows);
        return Response.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<Void> clearCart() {
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("==> 清空购物车, userId: {}", userId);
        int rows = cartMapper.deleteByUserId(userId);
        log.info("==> 清空购物车成功, userId: {}, deletedCount: {}", userId, rows);
        return Response.success();
    }

    private void validateGoodsAndSku(Long goodsId, Long skuId, int goodsCount) {
        GoodsSku sku = fetchSku(skuId);
        if (!goodsId.equals(sku.getGoodsId())) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "商品与规格不匹配");
        }

        Goods goods = fetchGoods(goodsId);
        assertGoodsSellable(goods);
        assertSkuSellable(sku);

        if (goodsCount > CartConstants.MAX_GOODS_COUNT_PER_ITEM) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID,
                    "单件商品购买数量不能超过" + CartConstants.MAX_GOODS_COUNT_PER_ITEM);
        }

        int stockNum = sku.getStockNum() == null ? 0 : sku.getStockNum();
        if (goodsCount > stockNum) {
            throw new BizException(ResponseCodeEnum.STOCK_NOT_ENOUGH, "库存不足，当前库存：" + stockNum);
        }
    }

    private Goods fetchGoods(Long goodsId) {
        Response<Goods> response = goodsFeignClient.getGoodsById(goodsId);
        if (response == null || !response.isSuccess() || response.getData() == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "商品不存在");
        }
        return response.getData();
    }

    private GoodsSku fetchSku(Long skuId) {
        Response<GoodsSku> response = goodsFeignClient.getSkuById(skuId);
        if (response == null || !response.isSuccess() || response.getData() == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "商品规格不存在");
        }
        return response.getData();
    }

    private void assertGoodsSellable(Goods goods) {
        if (goods.getIsDelete() != null && goods.getIsDelete() == 1) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "商品已下架");
        }
        if (goods.getStatus() == null || goods.getStatus() != CartConstants.GOODS_STATUS_ON_SALE) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "商品未上架");
        }
        if (goods.getAuditStatus() == null || !goods.getAuditStatus().equals(GoodsAuditRequest.AUDIT_PASS)) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "商品未通过审核");
        }
    }

    private void assertSkuSellable(GoodsSku sku) {
        if (sku.getIsDelete() != null && sku.getIsDelete() == 1) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "商品规格已失效");
        }
        if (sku.getStatus() == null || sku.getStatus() != CartConstants.SKU_STATUS_ENABLED) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "商品规格已停用");
        }
    }

    private void enrichCartItem(CartVO cartVO) {
        if (cartVO.getSalePrice() != null && cartVO.getGoodsCount() != null) {
            cartVO.setSubtotal(cartVO.getSalePrice().multiply(BigDecimal.valueOf(cartVO.getGoodsCount())));
        } else {
            cartVO.setSubtotal(BigDecimal.ZERO);
        }

        String invalidReason = resolveInvalidReason(cartVO);
        cartVO.setValid(invalidReason == null);
        cartVO.setInvalidReason(invalidReason);
    }

    private String resolveInvalidReason(CartVO cartVO) {
        if (cartVO.getGoodsName() == null) {
            return "商品不存在或已删除";
        }
        if (cartVO.getSkuSpecName() == null && cartVO.getSalePrice() == null) {
            return "商品规格不存在或已删除";
        }
        if (cartVO.getSalePrice() == null) {
            return "商品价格异常";
        }
        if (cartVO.getGoodsStatus() == null || cartVO.getGoodsStatus() != CartConstants.GOODS_STATUS_ON_SALE) {
            return "商品未上架";
        }
        if (cartVO.getAuditStatus() == null || !cartVO.getAuditStatus().equals(GoodsAuditRequest.AUDIT_PASS)) {
            return "商品未通过审核";
        }
        if (cartVO.getSkuStatus() == null || cartVO.getSkuStatus() != CartConstants.SKU_STATUS_ENABLED) {
            return "商品规格已停用";
        }

        int stockNum = cartVO.getStockNum() == null ? 0 : cartVO.getStockNum();
        if (cartVO.getGoodsCount() != null && cartVO.getGoodsCount() > stockNum) {
            return "库存不足，当前库存：" + stockNum;
        }
        return null;
    }

    private CartSummaryVO buildSummary(List<CartVO> cartVOList) {
        int totalLines = cartVOList.size();
        int totalQuantity = 0;
        int selectedLines = 0;
        int selectedQuantity = 0;
        BigDecimal selectedAmount = BigDecimal.ZERO;
        int validLines = 0;
        int validSelectedLines = 0;

        for (CartVO cartVO : cartVOList) {
            int count = cartVO.getGoodsCount() == null ? 0 : cartVO.getGoodsCount();
            totalQuantity += count;

            if (Boolean.TRUE.equals(cartVO.getValid())) {
                validLines++;
                if (cartVO.getSelected() != null && cartVO.getSelected() == 1) {
                    validSelectedLines++;
                }
            }

            if (cartVO.getSelected() != null && cartVO.getSelected() == 1) {
                selectedLines++;
                selectedQuantity += count;
                if (Boolean.TRUE.equals(cartVO.getValid()) && cartVO.getSubtotal() != null) {
                    selectedAmount = selectedAmount.add(cartVO.getSubtotal());
                }
            }
        }

        boolean allSelected = validLines > 0 && validSelectedLines == validLines;

        return CartSummaryVO.builder()
                .totalLines(totalLines)
                .totalQuantity(totalQuantity)
                .selectedLines(selectedLines)
                .selectedQuantity(selectedQuantity)
                .selectedAmount(selectedAmount)
                .allSelected(allSelected)
                .build();
    }

    private CartListVO buildEmptyCartList() {
        return CartListVO.builder()
                .items(Collections.emptyList())
                .summary(CartSummaryVO.builder()
                        .totalLines(0)
                        .totalQuantity(0)
                        .selectedLines(0)
                        .selectedQuantity(0)
                        .selectedAmount(BigDecimal.ZERO)
                        .allSelected(false)
                        .build())
                .build();
    }
}
