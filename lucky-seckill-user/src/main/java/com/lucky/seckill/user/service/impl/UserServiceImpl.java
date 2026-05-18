package com.lucky.seckill.user.service.impl;

import cloud.tianai.captcha.application.ImageCaptchaApplication;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.lucky.seckill.common.enums.ResponseCodeEnum;
import com.lucky.seckill.common.exception.BizException;
import com.lucky.seckill.common.manager.upload.FilePictureUpload;
import com.lucky.seckill.common.model.file.UploadPictureResult;
import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.user.enums.LoginTypeEnum;
import com.lucky.seckill.user.enums.UserStatusEnum;
import com.lucky.seckill.user.enums.VerifyCodeTypeEnum;
import com.lucky.seckill.user.mapper.RoleMapper;
import com.lucky.seckill.user.mapper.UserMapper;
import com.lucky.seckill.user.mapper.UserRoleMapper;
import com.lucky.seckill.user.model.dto.*;
import com.lucky.seckill.user.model.entity.Role;
import com.lucky.seckill.user.model.entity.User;
import com.lucky.seckill.user.model.entity.UserRole;
import com.lucky.seckill.user.model.vo.LoginUserRspVO;
import com.lucky.seckill.user.model.vo.UserProfileVO;
import com.lucky.seckill.user.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Author: Lucky
 * @Date: 2026/4/10 18:11
 * @Version: v1.0.0
 * @Description: 用户业务实现类
 **/
@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private UserRoleMapper userRoleMapper;

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ImageCaptchaApplication imageCaptchaApplication;

    @Resource
    private RoleMapper roleMapper;

    @Resource(name = "bizExecutor")
    private Executor bizExecutor;

    // Redis 中验证码的 Key 前缀
    private static final String VERIFY_CODE_KEY_PREFIX = "verify_code:";
    // Redis 中发送频率限制的 Key 前缀
    private static final String VERIFY_CODE_LIMIT_KEY_PREFIX = "verify_code_limit:";
    // 验证码过期时间（分钟）
    private static final Long VERIFY_CODE_EXPIRE_MINUTES = 5L;
    // 发送频率限制时间（秒）
    private static final Long VERIFY_CODE_LIMIT_SECONDS = 60L;
    // Redis 中每日发送次数限制的 Key 前缀
    private static final String VERIFY_CODE_DAILY_LIMIT_KEY_PREFIX = "verify_code_daily:";
    // 每日发送次数上限
    private static final Integer VERIFY_CODE_DAILY_LIMIT = 10;
    // Redis 中登录失败次数的 Key 前缀
    private static final String LOGIN_FAIL_COUNT_KEY_PREFIX = "login_fail_count:";
    // 登录失败次数上限（超过此值则临时锁定账号）
    private static final Integer LOGIN_FAIL_MAX_COUNT = 5;
    // 账号临时锁定时间（分钟）
    private static final Long LOGIN_LOCK_MINUTES = 30L;


    /**
     * 验证码校验 Lua 脚本
     */
    private final DefaultRedisScript<Long> checkAndDeleteVerifyCodeScript;

    /**
     * 登录失败计数 Lua 脚本
     */
    private final DefaultRedisScript<Long> checkAndIncrementLoginFailScript;

    /**
     * 每日发送次数限制 Lua 脚本
     */
    private final DefaultRedisScript<Long> checkAndIncrementDailyLimitScript;



    /**
     * 构造函数：初始化 Lua 脚本
     */
    public UserServiceImpl() {
        // 1. 验证码校验 Lua 脚本
        checkAndDeleteVerifyCodeScript = new DefaultRedisScript<>();
        // 从 classpath 的 lua 目录下加载 Lua 脚本文件
        checkAndDeleteVerifyCodeScript.setLocation(new ClassPathResource("lua/check_and_delete_verify_code.lua"));
        // 指定脚本返回值类型
        checkAndDeleteVerifyCodeScript.setResultType(Long.class);

        // 2. 登录失败计数 Lua 脚本
        checkAndIncrementLoginFailScript = new DefaultRedisScript<>();
        checkAndIncrementLoginFailScript.setLocation(new ClassPathResource("lua/check_and_increment_login_fail_count.lua"));
        checkAndIncrementLoginFailScript.setResultType(Long.class);

        // 3. 每日发送次数限制 Lua 脚本
        checkAndIncrementDailyLimitScript = new DefaultRedisScript<>();
        checkAndIncrementDailyLimitScript.setLocation(new ClassPathResource("lua/check_and_increment_verify_code_daily_limit.lua"));
        checkAndIncrementDailyLimitScript.setResultType(Long.class);

    }

    // BCrypt 密码编码器
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();


    /**
     * 用户注册
     *
     * @param registerUserReqVO
     * @return
     */
    @Override
    public Response<?> register(RegisterUserRequest registerUserReqVO) {
        String mobile = registerUserReqVO.getMobile();
        String password = registerUserReqVO.getPassword();
        String verifyCode = registerUserReqVO.getVerifyCode();

        // 1. 校验验证码
        checkVerifyCode(verifyCode, mobile, VerifyCodeTypeEnum.REGISTER.getPurpose());


        // 2. 查询用户是否存在（包括已逻辑删除的用户）
        User existUser = userMapper.selectByMobile(mobile);

        if (Objects.nonNull(existUser)) {
            // 用户存在且未删除，提示已注册
            throw new BizException(ResponseCodeEnum.USER_MOBILE_EXISTS);
        }

        // 3. 查询是否存在已逻辑删除的用户
        Long deletedUserId = userMapper.selectDeletedIdByMobile(mobile);

        long userId;
        if (Objects.nonNull(deletedUserId)) {
            // 用户曾被逻辑删除，恢复用户数据
            userId = deletedUserId;
            recoverDeletedUser(userId, password);
            log.info("==> 用户重新注册（恢复已删除账号）, mobile: {}, userId: {}", mobile, userId);
        } else {
            // 全新注册用户
            userId = createNewUser(mobile, password);
            log.info("==> 新用户注册成功, mobile: {}, userId: {}", mobile, userId);
        }

        // 4. 处理用户角色关联（检查是否已有角色关联记录）
        handleUserRoleRelation(userId);

        return Response.success();
    }

    /**
     * 创建新用户
     */
    private Long createNewUser(String mobile, String password) {
        // 密码加密（使用 BCrypt 算法）
        String encodedPassword = PASSWORD_ENCODER.encode(password);

        long userId = IdUtil.getSnowflakeNextId();

        // 构建用户实体，插入数据库
        User user = User.builder()
                .id(userId)
                .mobile(mobile)
                .password(encodedPassword)
                .nickname(generateNickname()) // 生成随机用户昵称
                .status(UserStatusEnum.ENABLED.getCode()) // 默认启用
                .isDelete(0) // 未删除
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        userMapper.insertSelective(user);

        return userId;
    }

    /**
     * 恢复已逻辑删除的用户
     */
    private void recoverDeletedUser(Long userId, String password) {
        // 密码加密
        String encodedPassword = PASSWORD_ENCODER.encode(password);

        // 更新用户信息（恢复状态、更新密码等）
        User user = User.builder()
                .id(userId)
                .password(encodedPassword)
                .status(UserStatusEnum.ENABLED.getCode())
                .nickname(generateNickname()) // 重新生成昵称
                .isDelete(0) // 恢复为未删除状态
                .updateTime(LocalDateTime.now())
                .build();

        userMapper.updateByPrimaryKeySelective(user);
    }

    /**
     * 处理用户角色关联
     */
    private void handleUserRoleRelation(Long userId) {
        // 检查是否已存在未删除的角色关联记录
        UserRole existingRole = userRoleMapper.selectByUserId(userId);

        if (Objects.nonNull(existingRole)) {
            // 已存在角色关联，无需处理
            log.info("==> 用户角色关联已存在, userId: {}, roleId: {}", userId, existingRole.getRoleId());
            return;
        }

        // 检查是否有已删除的角色关联记录
        UserRole deletedRole = userRoleMapper.selectDeletedByUserId(userId);

        if (Objects.nonNull(deletedRole)) {
            // 恢复已删除的角色关联
            UserRole userRole = UserRole.builder()
                    .id(deletedRole.getId())
                    .isDelete(0) // 恢复为未删除状态
                    .build();
            userRoleMapper.updateByPrimaryKeySelective(userRole);
            log.info("==> 恢复已删除的用户角色关联, userId: {}, roleId: {}", userId, deletedRole.getRoleId());
        } else {
            // 创建新的角色关联
            try {
                UserRole userRole = UserRole.builder()
                        .id(IdUtil.getSnowflakeNextId())
                        .userId(userId)
                        .roleId(1L) // 默认分配普通用户角色
                        .isDelete(0) // 未删除
                        .createTime(new java.util.Date())
                        .build();
                userRoleMapper.insertSelective(userRole);

                log.info("==> 用户角色关联创建成功, userId: {}, roleId: 1", userId);
            } catch (Exception e) {
                log.error("==> 用户角色关联创建失败, userId: {}", userId, e);
                throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "用户角色关联创建失败");
            }
        }
    }


    /**
     * 生成随机昵称
     * 格式：用户 + 6 位随机数字，如：用户382910
     *
     * @return 昵称
     */
    private String generateNickname() {
        return "用户" + RandomUtil.randomNumbers(6);
    }

    /**
     * 用户登录
     *
     * @return
     */
    @Override
    public Response<LoginUserRspVO> login(LoginUserRequest loginUserRequest) {
        String mobile = loginUserRequest.getMobile();
        Integer type = loginUserRequest.getType();

        // 1. 根据手机号查询用户
        User user = userMapper.selectByMobile(mobile);

        // 2. 判断用户是否存在
        if (Objects.isNull(user)) {
            // 密码登录：返回模糊错误提示
            if (Objects.equals(type, LoginTypeEnum.PASSWORD.getCode())) {
                throw new BizException(ResponseCodeEnum.USER_LOGIN_CREDENTIAL_ERROR);
            }
            throw new BizException(ResponseCodeEnum.USER_MOBILE_NOT_REGISTERED);
        }


        // 3. 校验用户状态（是否被禁用，放在身份验证之前，避免被禁用账号还执行耗时的密码校验）
        if (Objects.equals(user.getStatus(), UserStatusEnum.DISABLED.getCode())) {
            throw new BizException(ResponseCodeEnum.USER_STATUS_DISABLED);
        }

        // 4. 根据登录类型，进行身份验证
        if (Objects.equals(type, LoginTypeEnum.PASSWORD.getCode())) {
            // 检查登录失败次数
            checkLoginFailLimit(mobile);

            // 密码登录：校验密码是否正确
            checkPassword(loginUserRequest.getPassword(), user.getPassword(), mobile);
        } else {
            // 验证码登录：校验验证码是否正确
            checkVerifyCode(loginUserRequest.getVerifyCode(), mobile, VerifyCodeTypeEnum.LOGIN.getPurpose());
        }

        // 5. 调用 SaToken 执行登录，传入用户 ID
        StpUtil.login(user.getId());

        // 6. 查询用户角色并缓存到 Sa-Token Session（底层自动存储到 Redis）
        List<Role> roles = roleMapper.selectRolesByUserId(user.getId());
        if (roles != null && !roles.isEmpty()) {
            List<String> roleCodes = roles.stream()
                    .map(Role::getRoleCode)
                    .collect(Collectors.toList());
            StpUtil.getSession().set("roles", roleCodes);
            log.info("==> 用户角色已缓存到 Redis, userId: {}, roles: {}", user.getId(), roleCodes);
        }

        // 6. 获取 SaToken 生成的 Token
        String token = StpUtil.getTokenValue();

        // 7. 构建返参对象
        LoginUserRspVO loginUserRspVO = LoginUserRspVO.builder()
                .token(token)
                .userInfo(LoginUserRspVO.UserInfo.builder()
                        .id(user.getId())
                        .nickname(user.getNickname())
                        .avatar(user.getAvatar())
                        .build())
                .build();

        log.info("==> 用户登录成功, userId: {}, mobile: {}", user.getId(), mobile);

        return Response.success(loginUserRspVO);
    }

    /**
     * 检查密码
     * @param rawPassword
     * @param encodedPassword
     * @param mobile
     */
    private void checkPassword(String rawPassword, String encodedPassword, String mobile) {
        // 密码不能为空
        if (StrUtil.isBlank(rawPassword)) {
            // 登录失败次数 +1
            addLoginFailCount(mobile);

            throw new BizException(ResponseCodeEnum.USER_LOGIN_CREDENTIAL_ERROR); // 改为模糊提示
        }

        // 使用 BCrypt 校验明文密码和密文密码是否匹配
        boolean matches = PASSWORD_ENCODER.matches(rawPassword, encodedPassword);

        if (!matches) {
            // 登录失败次数 +1
            addLoginFailCount(mobile);

            throw new BizException(ResponseCodeEnum.USER_LOGIN_CREDENTIAL_ERROR); // 改为模糊提示
        }

        // 密码校验成功，清除登录失败次数
        String failCountKey = LOGIN_FAIL_COUNT_KEY_PREFIX + mobile;
        stringRedisTemplate.delete(failCountKey);
    }



    /**
     * 发送验证码
     *
     * @param sendVerifyCodeReqVO
     * @return
     */
    @Override
    public Response<Boolean> sendVerifyCode(SendVerifyCodeRequest sendVerifyCodeReqVO) {
        String mobile = sendVerifyCodeReqVO.getMobile();
        Integer type = sendVerifyCodeReqVO.getType();

        // 行为验证码二次校验
     /*   String captchaId = sendVerifyCodeReqVO.getCaptchaId();
        if (StrUtil.isBlank(captchaId)) {
            throw new BizException(ResponseCodeEnum.CAPTCHA_VERIFICATION_FAILED);
        }

        // 判断 ImageCaptchaApplication 是否支持二次校验
        boolean verified = false;
        if (imageCaptchaApplication instanceof SecondaryVerificationApplication) {
            verified = ((SecondaryVerificationApplication) imageCaptchaApplication).secondaryVerification(captchaId);
        }
        if (!verified) {
            throw new BizException(ResponseCodeEnum.CAPTCHA_VERIFICATION_FAILED);
        }*/

        // 判断验证码类型是否合法
        VerifyCodeTypeEnum verifyCodeType = VerifyCodeTypeEnum.valueOf(type);
        if (Objects.isNull(verifyCodeType)) {
            throw new BizException(ResponseCodeEnum.VERIFY_CODE_TYPE_ERROR);
        }

        // 发送频率限制：检查是否在 60 秒内重复发送
        String limitKey = VERIFY_CODE_LIMIT_KEY_PREFIX + verifyCodeType.getPurpose() + ":" + mobile;

        // 如果 Key 已存在（60 秒内已发送过），返回 false；不存在则创建 Key 并返回 true
        Boolean absent = stringRedisTemplate.opsForValue()
                .setIfAbsent(limitKey, "1", VERIFY_CODE_LIMIT_SECONDS, TimeUnit.SECONDS);

        if (Boolean.FALSE.equals(absent)) {
            throw new BizException(ResponseCodeEnum.VERIFY_CODE_SEND_TOO_FREQUENT);
        }


        // 每日发送次数限制：同一手机号、同一场景，每天最多发送 10 条
        String dailyLimitKey = VERIFY_CODE_DAILY_LIMIT_KEY_PREFIX + verifyCodeType.getPurpose()
                + ":" + mobile + ":" + LocalDate.now();

        // 计算从当前时间，到第二天凌晨零点之间还剩下多少秒
        long secondsUntilMidnight = Duration.between(
                LocalDateTime.now(),
                LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.MIDNIGHT)
        ).getSeconds();

        // 执行 Lua 脚本：原子性地检查每日发送次数并累加
        Long dailyCount = stringRedisTemplate.execute(checkAndIncrementDailyLimitScript,
                Collections.singletonList(dailyLimitKey),
                String.valueOf(VERIFY_CODE_DAILY_LIMIT),
                String.valueOf(secondsUntilMidnight));

        // 如果已经超过 10 条，抛出业务异常
        if (Objects.nonNull(dailyCount) && dailyCount == -1) {
            throw new BizException(ResponseCodeEnum.VERIFY_CODE_DAILY_LIMIT_EXCEEDED);
        }


        // 生成 6 位随机数字验证码
        String verifyCode = RandomUtil.randomNumbers(6);

        // 构建验证码 Redis Key
        String redisKey = VERIFY_CODE_KEY_PREFIX + verifyCodeType.getPurpose() + ":" + mobile;

        // 缓存验证码（5 分钟 TTL）
        stringRedisTemplate.opsForValue().set(redisKey, verifyCode, VERIFY_CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);

        // 异步发送短信验证码
        bizExecutor.execute(() -> sendSms(mobile, verifyCode));

        return Response.success();
    }

    /**
     * 发送短信验证码（异步执行，由线程池调度）
     *
     * @param mobile     手机号
     * @param verifyCode 验证码
     */
    private void sendSms(String mobile, String verifyCode) {
        try {
            // TODO: 调用短信服务商 API 发送验证码

            // 开发阶段通过日志打印验证码，方便调试
            log.info("==> 验证码发送成功, mobile: {}, verifyCode: {}", mobile, verifyCode);
        } catch (Exception e) {
            log.error("==> 验证码发送失败, mobile: {}, verifyCode: {}", mobile, verifyCode, e);
        }
    }

    /**
     * 校验验证码
     *
     * @param verifyCode 验证码
     */
    private void checkVerifyCode(String verifyCode, String mobile, String purpose) {
        // 验证码不能为空
        if (StrUtil.isBlank(verifyCode)) {
            throw new BizException(ResponseCodeEnum.USER_VERIFY_CODE_ERROR);
        }

        // 构建 Redis Key
        String redisKey = VERIFY_CODE_KEY_PREFIX + purpose + ":" + mobile;

        // 执行 Lua 脚本：原子性地比对验证码并删除（匹配返回 1; 不匹配或 Key 不存在返回 0）
        Long result = stringRedisTemplate.execute(checkAndDeleteVerifyCodeScript,
                Collections.singletonList(redisKey),
                verifyCode);

        // 验证码错误或已过期
        if (result == null || result == 0) {
            throw new BizException(ResponseCodeEnum.USER_VERIFY_CODE_ERROR);
        }
    }

    /**
     * 检查登录失败次数是否超限
     *
     * @param mobile 手机号
     */
    private void checkLoginFailLimit(String mobile) {
        // 构建 Redis Key
        String failCountKey = LOGIN_FAIL_COUNT_KEY_PREFIX + mobile;

        // 查询 Redis 缓存中的计数
        String failCountStr = stringRedisTemplate.opsForValue().get(failCountKey);

        // 判断登录失败次数是否超过上限
        if (StrUtil.isNotBlank(failCountStr)) {
            int failCount = Integer.parseInt(failCountStr);
            if (failCount >= LOGIN_FAIL_MAX_COUNT) {
                throw new BizException(ResponseCodeEnum.LOGIN_FAIL_TOO_MANY);
            }
        }
    }
    /**
     * 累加登录失败次数（Lua 脚本原子操作）
     *
     * @param mobile 手机号
     */
    private void addLoginFailCount(String mobile) {
        // 构建 Redis Key
        String failCountKey = LOGIN_FAIL_COUNT_KEY_PREFIX + mobile;

        // 执行 Lua 脚本：原子性地检查失败次数并累加（超限返回 -1; 未超限返回累加后的值）
        Long result = stringRedisTemplate.execute(checkAndIncrementLoginFailScript,
                Collections.singletonList(failCountKey),
                String.valueOf(LOGIN_FAIL_MAX_COUNT),
                String.valueOf(LOGIN_LOCK_MINUTES * 60));

        // 失败次数已达上限，直接拒绝
        if (Objects.nonNull(result) && result == -1) {
            throw new BizException(ResponseCodeEnum.LOGIN_FAIL_TOO_MANY);
        }
    }
    /**
     * 退出登录
     *
     * @return
     */
    @Override
    public Response<?> logout() {
        // 获取当前请求中的 Token 值
        String tokenValue = StpUtil.getTokenValue();
        // 获取当前登录用户的 ID
        Object userId = StpUtil.getLoginId();

        // 调用 SaToken 的退出登录方法
        // 此方法会自动从请求头中获取 Token，然后清除该 Token 对应的会话信息
        StpUtil.logout();

        log.info("==> 用户退出登录, userId: {}, token: {}", userId, tokenValue);

        return Response.success();
    }

    /**
     * 管理员修改用户资料
     *
     * @param request
     * @return
     */
    @Override
    public Response<?> adminUpdateUserProfile(AdminUpdateUserProfileRequest request) {
        // 获取当前登录的管理员 ID
        Long adminId = StpUtil.getLoginIdAsLong();
        Long targetUserId = request.getUserId();

        // 权限校验：检查是否为管理员
        List<String> roles = StpUtil.getRoleList();
        if (roles == null || !roles.contains("admin")) {
            log.warn("==> 非管理员尝试修改用户资料, adminId: {}", adminId);
            throw new BizException(ResponseCodeEnum.USER_PERMISSION_DENIED);
        }

        // 校验：管理员不能修改自身资料（使用专门的接口）
        if (adminId.equals(targetUserId)) {
            log.warn("==> 管理员不能通过此接口修改自身资料, adminId: {}", adminId);
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "管理员请使用普通接口修改自身资料");
        }

        // 查询目标用户是否存在（包括已逻辑删除的）
        User existUser = userMapper.selectByPrimaryKey(targetUserId);
        if (existUser == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "用户不存在");
        }

        // 构建更新用户实体
        User user = User.builder()
                .id(targetUserId)
                .nickname(request.getNickname())
                .updateTime(LocalDateTime.now())
                .build();

        // 更新用户信息
        userMapper.updateByPrimaryKeySelective(user);

        log.info("==> 管理员修改用户资料成功, adminId: {}, targetUserId: {}, nickname: {}",
                adminId, targetUserId, request.getNickname());

        return Response.success();
    }

    /**
     * 完善用户资料
     *
     * @param updateUserProfileRequest
     * @return
     */
    @Override
    public Response<?> updateUserProfile(UpdateUserProfileRequest updateUserProfileRequest) {
        // 获取当前登录用户 ID
        Long currentUserId = StpUtil.getLoginIdAsLong();

        // 构建更新用户实体
        User user = User.builder()
                .id(currentUserId)
                .nickname(updateUserProfileRequest.getNickname())
                .updateTime(LocalDateTime.now())
                .build();

        // 更新用户信息到数据库
        userMapper.updateByPrimaryKeySelective(user);

        // 优化：主动从数据库拉取最新信息并更新 Redis 缓存
        refreshUserProfileCache(currentUserId);

        log.info("==> 用户资料更新成功, userId: {}, nickname: {}", currentUserId, updateUserProfileRequest.getNickname());

        return Response.success();
    }

    /**
     * 上传用户头像
     *
     * @param file
     * @return
     */
    @Override
    public Response<UploadPictureResult> uploadAvatar(MultipartFile file) {
        // 1. 获取当前登录用户 ID
        Long currentUserId = StpUtil.getLoginIdAsLong();

        // 2. 权限校验：检查是否为本人或管理员
        checkPermission(currentUserId, null);

        // 3. 使用文件上传工具上传头像
        UploadPictureResult uploadResult = filePictureUpload.uploadPicture(file, "avatar", true);

        // 4. 更新用户头像 URL 到数据库（默认更新当前登录用户的头像）
        User user = User.builder()
                .id(currentUserId)
                .avatar(uploadResult.getImageUrl())
                .updateTime(LocalDateTime.now())
                .build();
        userMapper.updateByPrimaryKeySelective(user);

        // 5. 优化：主动从数据库拉取最新信息并更新 Redis 缓存
        refreshUserProfileCache(currentUserId);

        log.info("==> 用户头像上传成功, userId: {}, avatarUrl: {}", currentUserId, uploadResult.getImageUrl());

        return Response.success(uploadResult);
    }

    /**
     * 管理员为用户上传头像
     *
     * @param userId 目标用户 ID
     * @param file   头像文件
     * @return
     */
    public Response<UploadPictureResult> adminUploadAvatar(Long userId, MultipartFile file) {
        // 1. 获取当前登录的管理员 ID
        Long adminId = StpUtil.getLoginIdAsLong();

        // 2. 权限校验：检查是否为管理员
        List<String> roles = StpUtil.getRoleList();
        if (roles == null || !roles.contains("admin")) {
            throw new BizException(ResponseCodeEnum.USER_PERMISSION_DENIED);
        }

        // 3. 查询目标用户是否存在
        User existUser = userMapper.selectByPrimaryKey(userId);
        if (existUser == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "用户不存在");
        }

        // 4. 上传头像
        UploadPictureResult uploadResult = filePictureUpload.uploadPicture(file, "avatar", true);

        // 5. 更新目标用户的头像
        User user = User.builder()
                .id(userId)
                .avatar(uploadResult.getImageUrl())
                .updateTime(LocalDateTime.now())
                .build();
        userMapper.updateByPrimaryKeySelective(user);

        // 6. 清除目标用户的缓存（因为管理员操作的是别人的账号）
        // 注意：StpUtil.getSession() 获取的是当前管理员的 Session，所以不能直接 set。
        // 如果需要精确清除目标用户缓存，可以使用 stringRedisTemplate 删除对应的 Key，
        // 或者简单地让目标用户下次查询时自动刷新。

        log.info("==> 管理员为用户上传头像成功, adminId: {}, targetUserId: {}, avatarUrl: {}",
                adminId, userId, uploadResult.getImageUrl());

        return Response.success(uploadResult);
    }

    /**
     * 权限校验：检查当前用户是否有权限操作目标用户
     *
     * @param currentUserId 当前登录用户 ID
     * @param targetUserId 目标用户 ID（如果为 null，则只检查是否为管理员）
     */
    private void checkPermission(Long currentUserId, Long targetUserId) {
        // 1. 获取当前用户的角色列表
        List<String> roles = StpUtil.getRoleList();

        // 2. 判断是否为管理员
        boolean isAdmin = roles != null && roles.contains("admin");

        if (isAdmin) {
            // 管理员拥有所有权限
            log.debug("==> 当前用户是管理员，拥有操作权限, userId: {}", currentUserId);
            return;
        }

        // 3. 如果不是管理员，检查是否为本人操作
        if (targetUserId != null && !currentUserId.equals(targetUserId)) {
            log.warn("==> 权限不足，非本人且非管理员, currentUserId: {}, targetUserId: {}", currentUserId, targetUserId);
            throw new BizException(ResponseCodeEnum.USER_PERMISSION_DENIED);
        }

        // 4. 本人操作，允许
        log.debug("==> 本人操作，拥有权限, userId: {}", currentUserId);
    }


    /**
     * 刷新用户资料缓存（从数据库拉取最新数据存入 Redis）
     *
     * @param userId 用户 ID
     */
    private void refreshUserProfileCache(Long userId) {
        // 1. 从数据库查询最新用户信息
        User user = userMapper.selectByPrimaryKey(userId);
        if (user == null) {
            return;
        }

        // 2. 构建 VO 对象（包含脱敏处理）
        UserProfileVO profileVO = UserProfileVO.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .mobile(maskMobile(user.getMobile()))
                .avatar(user.getAvatar())
                .status(user.getStatus())
                .build();

        // 3. 更新 Sa-Token Session (Redis) 中的缓存
        StpUtil.getSession().set("user_profile", profileVO);

        log.debug("==> 用户资料缓存已主动刷新, userId: {}", userId);
    }

    /**
     * 查看用户资料（优先从 Redis 缓存获取）
     *
     * @return
     */
    @Override
    public Response<UserProfileVO> getUserProfile() {
        // 1. 获取当前登录用户 ID
        Long userId = StpUtil.getLoginIdAsLong();

        // 2. 尝试从 Sa-Token Session (Redis) 中获取用户资料缓存
        SaSession session = StpUtil.getSession();
        UserProfileVO cachedProfile = (UserProfileVO) session.get("user_profile");

        if (cachedProfile != null) {
            log.info("==> 从 Redis 缓存获取用户资料, userId: {}", userId);
            return Response.success(cachedProfile);
        }

        // 3. 缓存未命中，调用刷新方法从数据库加载
        log.info("==> 缓存未命中，从数据库查询用户资料, userId: {}", userId);
        refreshUserProfileCache(userId);

        // 4. 再次从 Session 获取刚刚存入的数据
        UserProfileVO freshProfile = (UserProfileVO) session.get("user_profile");
        return Response.success(freshProfile);
    }


    /**
     * 手机号脱敏处理
     * 例如：13800138000 -> 138****8000
     */
    private String maskMobile(String mobile) {
        if (StrUtil.isBlank(mobile) || mobile.length() != 11) {
            return mobile;
        }
        return mobile.substring(0, 3) + "****" + mobile.substring(7);
    }


    /**
     * 禁用/启用用户
     *
     * @param userId
     * @param status
     * @return
     */
    @Override
    public Response<?> updateUserStatus(Long userId, Integer status) {
        // 获取当前登录的管理员 ID
        Long currentAdminId = StpUtil.getLoginIdAsLong();

        // 校验：管理员不能修改自身的状态
        if (currentAdminId.equals(userId)) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "管理员不能修改自身账号状态");
        }


        // 校验状态值是否合法
        if (status != UserStatusEnum.DISABLED.getCode() && status != UserStatusEnum.ENABLED.getCode()) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "用户状态不合法");
        }

        // 查询用户是否存在
        User existUser = userMapper.selectByPrimaryKey(userId);
        if (existUser == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "用户不存在");
        }

        // 更新用户状态
        User user = User.builder()
                .id(userId)
                .status(status)
                .build();
        userMapper.updateByPrimaryKeySelective(user);

        // 如果用户被禁用，强制下线并清除 Redis 中的会话信息
        if (UserStatusEnum.DISABLED.getCode().equals(status)) {
            // 检查用户是否在线
            if (StpUtil.isLogin(userId)) {
                // 强制用户下线，清除所有登录态和 Redis 会话数据
                StpUtil.kickout(userId);
                log.info("==> 用户已被禁用，强制下线并清除登录态, userId: {}", userId);
            } else {
                log.info("==> 用户已被禁用，但该用户当前未在线, userId: {}", userId);
            }
        }

        log.info("==> 用户状态更新成功, userId: {}, status: {}", userId, status);

        return Response.success();
    }
}

