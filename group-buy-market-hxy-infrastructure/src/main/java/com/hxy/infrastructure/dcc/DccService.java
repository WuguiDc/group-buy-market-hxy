package com.hxy.infrastructure.dcc;


import com.hxy.types.annotations.DCCValue;
import org.springframework.stereotype.Service;

@Service
public class DccService {

    @DCCValue("downgradeSwitch:0")
    private String downgradeSwitch;

    @DCCValue("cutRange:100")
    private String cutRange;

    public boolean isDowngradeSwitch() {
        return "1".equals(this.downgradeSwitch);
    }

    public boolean isCutRange(String userId) {
        int hashCode = Math.abs(userId.hashCode());

        int lastTwoDigits = hashCode % 100;

        if (lastTwoDigits <= cutRange.hashCode()) {
            return true;
        }
        return false;
    }



}
