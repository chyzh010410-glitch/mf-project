package com.mf.fertilizer.user.service;

import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.user.entity.SysUser;
import com.mf.fertilizer.user.entity.User;
import com.mf.fertilizer.vo.PageVO;

import java.util.Map;

public interface AdminUserApplicationService {

    PageVO<User> listUsers(PageDTO page, String keyword, Integer status);

    void updateUserStatus(Long id, Integer status);

    PageVO<SysUser> listAdmins(PageDTO page, String keyword, Integer status);

    SysUser getAdmin(Long id);

    void createAdmin(Map<String, Object> body);

    void updateAdmin(Long id, Map<String, Object> body);

    void updateAdminStatus(Long id, Integer status);

    void disableAdmin(Long id);
}
