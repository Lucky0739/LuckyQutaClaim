package com.lucky.seckill.goods.feign;

import com.lucky.seckill.common.utils.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "lucky-seckill-goods", path = "/goods")
public interface GoodsFeignClient {

    @GetMapping("/get/{id}")
    Object getGoodsById(@PathVariable("id") Long id);

    @GetMapping("/list")
    Object getGoodsList();

    @GetMapping("/detail/{id}")
    Object getGoodsDetail(@PathVariable("id") Long id);

    @GetMapping("/sku/{skuId}")
    Object getSkuById(@PathVariable("skuId") Long skuId);

    @PostMapping("/sku/lockStock")
    Response<Boolean> lockStock(@RequestParam("skuId") Long skuId, @RequestParam("stockNum") Integer stockNum);

    @PostMapping("/sku/deductStock")
    Response<Boolean> deductStock(@RequestParam("skuId") Long skuId, @RequestParam("stockNum") Integer stockNum);

    @PostMapping("/sku/releaseStock")
    Response<Boolean> releaseStock(@RequestParam("skuId") Long skuId, @RequestParam("stockNum") Integer stockNum);
}