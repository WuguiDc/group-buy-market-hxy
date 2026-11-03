package com.hxy.infrastructure.dcc;


import cn.bugstack.wrench.dynamic.config.center.types.annotations.DCCValue;
import com.hxy.types.common.Constants;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class DccService {

    @DCCValue("downgradeSwitch:0")
    private String downgradeSwitch;

    @DCCValue("cutRange:100")
    private String cutRange;

    @DCCValue("scBlacklist:s02c02")
    private String scBlacklist;

    @DCCValue("cacheOpenService:0")
    private String cacheOpenService;

    public boolean isDowngradeSwitch() {
        return "1".equals(this.downgradeSwitch);
    }

    public boolean isCutRange(String userId) {
        int lastTwoDigits = Math.floorMod(userId.hashCode(), 100);
        int range = Integer.parseInt(cutRange);
        return lastTwoDigits < range;
    }

    public boolean isScBlacklist(String source, String channel) {
        List<String> list = Arrays.asList(scBlacklist.split(Constants.SPLIT));
        return list.contains(source+channel);

    }

    /** 缓存开关：0 开启，1 关闭并直接查询数据库。 */
    public boolean isCacheOpenService() {
        return "0".equals(cacheOpenService);
    }


}
