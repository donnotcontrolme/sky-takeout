package com.sky.enumeration;

import lombok.Getter;

/**
 * 营业状态
 */
@Getter
public enum ShopStatus {

    OPEN("营业中",1),
    CLOSE("已打烊",0);

    private final String des;
    private final Integer code;

    ShopStatus(String des, Integer code){
        this.des=des;
        this.code=code;
    }

    public static ShopStatus getStatus(Integer code){
        for(ShopStatus status:ShopStatus.values()){
            if(status.code.equals(code)){
                return status;
            }
        }
        return null;
    }

}
