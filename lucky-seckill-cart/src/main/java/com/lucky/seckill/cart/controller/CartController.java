package com.lucky.seckill.cart.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.lucky.seckill.cart.model.dto.AddCartRequest;
import com.lucky.seckill.cart.model.dto.BatchDeleteCartRequest;
import com.lucky.seckill.cart.model.vo.CartListVO;
import com.lucky.seckill.cart.service.CartService;
import com.lucky.seckill.common.aspect.ApiOperationLog;
import com.lucky.seckill.common.utils.Response;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cart")
@Slf4j
public class CartController {

    @Resource
    private CartService cartService;

    @SaCheckLogin
    @PostMapping("/add")
    @ApiOperationLog(description = "添加商品到购物车")
    public Response<Void> addCart(@Validated @RequestBody AddCartRequest request) {
        log.info("==> 添加商品到购物车请求, goodsId: {}, skuId: {}, goodsCount: {}",
                request.getGoodsId(), request.getSkuId(), request.getGoodsCount());
        return cartService.addCart(request);
    }

    @SaCheckLogin
    @GetMapping("/list")
    @ApiOperationLog(description = "查询购物车列表")
    public Response<CartListVO> getCartList() {
        log.info("==> 查询购物车列表请求");
        return cartService.getCartList();
    }

    @GetMapping("/count")
    @ApiOperationLog(description = "查询购物车商品总件数")
    public Response<Integer> getCartCount() {
        log.info("==> 查询购物车商品总件数请求");
        return cartService.getCartCount();
    }

    @SaCheckLogin
    @DeleteMapping("/delete/{cartId}")
    @ApiOperationLog(description = "删除购物车商品")
    public Response<Void> deleteCart(@PathVariable("cartId") Long cartId) {
        log.info("==> 删除购物车商品请求, cartId: {}", cartId);
        return cartService.deleteCart(cartId);
    }

    @SaCheckLogin
    @PutMapping("/updateQuantity/{cartId}")
    @ApiOperationLog(description = "更新购物车商品数量")
    public Response<Void> updateQuantity(@PathVariable("cartId") Long cartId,
                                         @RequestParam("goodsCount") Integer goodsCount) {
        log.info("==> 更新购物车商品数量请求, cartId: {}, goodsCount: {}", cartId, goodsCount);
        return cartService.updateQuantity(cartId, goodsCount);
    }

    @SaCheckLogin
    @PutMapping("/select/{cartId}")
    @ApiOperationLog(description = "选中/取消选中购物车商品")
    public Response<Void> selectCart(@PathVariable("cartId") Long cartId,
                                     @RequestParam("selected") Integer selected) {
        log.info("==> 选中/取消选中购物车商品请求, cartId: {}, selected: {}", cartId, selected);
        return cartService.selectCart(cartId, selected);
    }

    @SaCheckLogin
    @PutMapping("/selectAll")
    @ApiOperationLog(description = "全选/取消全选购物车")
    public Response<Void> selectAll(@RequestParam("selected") Integer selected) {
        log.info("==> 全选/取消全选购物车请求, selected: {}", selected);
        return cartService.selectAll(selected);
    }

    @SaCheckLogin
    @DeleteMapping("/batchDelete")
    @ApiOperationLog(description = "批量删除购物车商品")
    public Response<Void> batchDeleteCart(@RequestParam("cartIds") List<Long> cartIds) {
        log.info("==> 批量删除购物车商品请求, cartIds: {}", cartIds);
        return cartService.batchDeleteCart(cartIds);
    }

    @SaCheckLogin
    @PostMapping("/batchDelete")
    @ApiOperationLog(description = "批量删除购物车商品")
    public Response<Void> batchDeleteCartByBody(@Validated @RequestBody BatchDeleteCartRequest request) {
        log.info("==> 批量删除购物车商品请求(Body), cartIds: {}", request.getCartIds());
        return cartService.batchDeleteCart(request);
    }

    @SaCheckLogin
    @DeleteMapping("/deleteSelected")
    @ApiOperationLog(description = "删除已选购物车商品")
    public Response<Void> deleteSelected() {
        log.info("==> 删除已选购物车商品请求");
        return cartService.deleteSelected();
    }

    @SaCheckLogin
    @DeleteMapping("/clear")
    @ApiOperationLog(description = "清空购物车")
    public Response<Void> clearCart() {
        log.info("==> 清空购物车请求");
        return cartService.clearCart();
    }
}
