package com.zzm.picturebackend.model.enums;


import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

// 使用@Getter注解，自动生成所有字段的getter方法
@Getter
// 定义一个枚举类UserRoleEnum，表示用户角色
public enum UserRoleEnum {

    // 定义枚举常量USER，表示用户角色，文本描述为"用户"，值为"user"
    USER("用户", "user"),
    // 定义枚举常量ADMIN，表示管理员角色，文本描述为"管理员"，值为"admin"
    ADMIN("管理员", "admin");

    // 定义私有final字段text，表示枚举项的文本描述
    private final String text;

    // 定义私有final字段value，表示枚举项的值
    private final String value;

    // 定义枚举构造方法，初始化text和value字段
    UserRoleEnum(String text, String value) {
        // 将传入的text参数赋值给字段text
        this.text = text;
        // 将传入的value参数赋值给字段value
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值的value
     * @return 枚举值
     */
    public static UserRoleEnum getEnumByValue(String value) {
        // 如果传入的value为空或null，返回null
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        // 遍历UserRoleEnum的所有枚举常量
        for (UserRoleEnum anEnum : UserRoleEnum.values()) {
            // 如果当前枚举常量的value字段等于传入的value，返回该枚举常量
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        // 如果没有找到匹配的枚举常量，返回null
        return null;
    }
}
