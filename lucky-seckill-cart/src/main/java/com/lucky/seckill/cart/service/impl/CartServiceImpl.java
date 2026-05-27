package com.lucky.seckill.cart.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import com.lucky.seckill.cart.mapper.CartMapper;
import com.lucky.seckill.cart.model.dto.AddCartRequest;
import com.lucky.seckill.cart.model.entity.Cart;
import com.lucky.seckill.cart.model.vo.CartVO;
import com.lucky.seckill.cart.service.CartService;
import com.lucky.seckill.common.enums.ResponseCodeEnum;
import com.lucky.seckill.common.exception.BizException;
import com.lucky.seckill.common.utils.Response;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        if (existCart != null) {
            int newGoodsCount = existCart.getGoodsCount() + request.getGoodsCount();
            int rows = cartMapper.updateGoodsCountByUserIdAndSkuId(userId, request.getSkuId(), newGoodsCount);
            if (rows == 0) {
                throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "更新购物车失败");
            }
            log.info("==> 购物车商品数量累加成功, userId: {}, skuId: {}, newGoodsCount: {}",
                    userId, request.getSkuId(), newGoodsCount);
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
    public Response<List<CartVO>> getCartList() {
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("==> 查询购物车列表, userId: {}", userId);

        List<CartVO> cartVOList = cartMapper.selectCartVOListByUserId(userId);
        if (cartVOList == null || cartVOList.isEmpty()) {
            return Response.success(Collections.emptyList());
        }

        log.info("==> 查询购物车列表成功, userId: {}, count: {}", userId, cartVOList.size());
        return Response.success(cartVOList);
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

        Long userId = StpUtil.getLoginIdAsLong();
        log.info("==> 更新购物车商品数量, userId: {}, cartId: {}, goodsCount: {}",
                userId, cartId, goodsCount);

        Cart cart = cartMapper.selectById(cartId);
        if (cart == null || !cart.getUserId().equals(userId)) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "购物车商品不存在");
        }

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
}
