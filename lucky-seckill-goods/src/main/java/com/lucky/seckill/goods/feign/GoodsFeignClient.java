package com.lucky.seckill.goods.feign;

import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.goods.model.entity.Goods;
import com.lucky.seckill.goods.model.entity.GoodsSku;
import com.lucky.seckill.goods.model.vo.GoodsDetailVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(contextId = "goodsFeignClient",name = "lucky-seckill-goods", path = "/goods")
public interface GoodsFeignClient {

    @GetMapping("/get/{id}")
    Response<Goods> getGoodsById(@PathVariable(name = "id") Long id);

    @GetMapping("/list")
    Object getGoodsList();

    @GetMapping("/detail/{id}")
    Response<GoodsDetailVO> getGoodsDetail(@PathVariable(name = "id") Long id);

    @GetMapping("/sku/{skuId}")
    Response<GoodsSku> getSkuById(@PathVariable(name = "skuId") Long skuId);

    @PostMapping("/sku/lockStock")
    Response<Boolean> lockStock(@RequestParam("skuId") Long skuId, @RequestParam("buyGoodsNum") Integer buyGoodsNum);

    @PostMapping("/sku/deductStock")
    Response<Boolean> deductStock(@RequestParam("skuId") Long skuId, @RequestParam("stockNum") Integer stockNum);

    @PostMapping("/sku/releaseStock")
    Response<Boolean> releaseStock(@RequestParam("skuId") Long skuId, @RequestParam("stockNum") Integer stockNum);
}