package com.lucky.seckill.activity.model.vo;

import com.lucky.seckill.activity.model.entity.SeckillActivity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeckillActivityVO {

    private Long id;

    private String activityName;

    private LocalDateTime beginTime;

    private LocalDateTime endTime;

    private Integer status;

    private String description;

    public static SeckillActivityVO fromEntity(SeckillActivity activity) {
        if (activity == null) {
            return null;
        }
        return SeckillActivityVO.builder()
                .id(activity.getId())
                .activityName(activity.getActivityName())
                .beginTime(activity.getBeginTime())
                .endTime(activity.getEndTime())
                .status(activity.getStatus())
                .description(activity.getDescription())
                .build();
    }
}
