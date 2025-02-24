package com.zzm.picturebackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzm.picturebackend.model.entity.User;
import com.zzm.picturebackend.service.UserService;
import com.zzm.picturebackend.mapper.UserMapper;
import org.springframework.stereotype.Service;

/**
* @author zhou
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2025-02-21 10:28:22
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService {

}




