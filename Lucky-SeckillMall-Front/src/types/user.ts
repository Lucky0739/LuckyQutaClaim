/** 登录类型：1=密码登录，2=验证码登录 */
export const LoginType = {
  PASSWORD: 1,
  VERIFY_CODE: 2,
} as const
export type LoginType = (typeof LoginType)[keyof typeof LoginType]

/** 验证码类型：1=注册，2=登录，3=修改密码，4=注销账号 */
export const VerifyCodeType = {
  REGISTER: 1,
  LOGIN: 2,
  CHANGE_PASSWORD: 3,
  CANCEL_ACCOUNT: 4,
} as const
export type VerifyCodeType = (typeof VerifyCodeType)[keyof typeof VerifyCodeType]

/** 用户状态：0=禁用，1=启用 */
export const UserStatus = {
  DISABLED: 0,
  ENABLED: 1,
} as const
export type UserStatus = (typeof UserStatus)[keyof typeof UserStatus]

/** 用户注册请求 */
export interface RegisterUserRequest {
  mobile: string
  password: string
  verifyCode: string
}

/** 手机号格式（与后端 LoginUserRequest 一致） */
export const MOBILE_PATTERN = /^1[3-9]\d{9}$/

/** 用户登录请求（与后端 LoginUserRequest 对齐） */
export interface LoginUserRequest {
  mobile: string
  type: LoginType
  /** type=1 密码登录时必填 */
  password?: string
  /** type=2 验证码登录时必填 */
  verifyCode?: string
}

/** 按登录类型组装请求体，仅携带当前方式所需字段 */
export function buildLoginRequest(params: {
  mobile: string
  type: LoginType
  password?: string
  verifyCode?: string
}): LoginUserRequest {
  const { mobile, type, password, verifyCode } = params
  if (type === LoginType.PASSWORD) {
    return { mobile, type, password }
  }
  return { mobile, type, verifyCode }
}

/** 发送验证码请求 */
export interface SendVerifyCodeRequest {
  mobile: string
  type: VerifyCodeType
  /** 行为验证码校验通过后的 captchaId */
  captchaId: string
}

/** 完善用户资料请求（对应后端 UpdateUserProfileRequest） */
export interface UpdateUserProfileRequest {
  /** 用户昵称，不能为空 */
  nickname: string
}

/** 用户注销账号请求 */
export interface CancelAccountRequest {
  password: string
  verifyCode: string
}

/** 修改密码请求，对应后端 ChangePasswordRequest */
export interface ChangePasswordRequest {
  oldPassword: string
  newPassword: string
  verifyCode: string
}

/** 用户 ID（后端 Long，前端以 string 保存避免精度丢失） */
export type UserId = string

/** 管理员修改用户资料请求 */
export interface AdminUpdateUserProfileRequest {
  userId: UserId
  nickname: string
}

/** 管理员分页查询用户列表请求 */
export interface AdminUserQueryRequest {
  nickname?: string
  mobile?: string
  status?: UserStatus
  startTime?: string
  endTime?: string
  pageNum?: number
  pageSize?: number
}

/** 登录响应 */
export interface LoginUserRspVO {
  token: string
  userInfo: LoginUserInfo
}

export interface LoginUserInfo {
  id: number
  nickname: string
  avatar?: string
  /** 用户角色列表，如 admin、merchant */
  roles?: string[]
}

/** 用户资料响应（对应后端 UserProfileVO） */
export interface UserProfileVO {
  id: number
  nickname: string
  /** 手机号（后端已脱敏，如 138****8000） */
  mobile: string
  avatar?: string
  status: UserStatus
}

/** 管理员用户列表项 */
export interface AdminUserListVO {
  id: UserId
  nickname: string
  mobile: string
  avatar?: string
  status: UserStatus
  statusDesc?: string
  createTime?: string
  updateTime?: string
  /** 用户角色列表 */
  roleNames?: string[]
}

/** JSON 解析后的原始项（id 已在 parse 阶段转为 string） */
export type AdminUserListRaw = Omit<AdminUserListVO, 'id'> & {
  id?: string | number
}

function toUserIdString(value?: string | number | null): UserId {
  if (value === undefined || value === null || value === '') return ''
  return String(value)
}

/** 规范化管理员用户列表项，确保 id 为 string */
export function normalizeAdminUserListItem(raw: AdminUserListRaw): AdminUserListVO {
  return {
    id: toUserIdString(raw.id),
    nickname: raw.nickname,
    mobile: raw.mobile,
    avatar: raw.avatar,
    status: raw.status,
    statusDesc: raw.statusDesc,
    createTime: raw.createTime,
    updateTime: raw.updateTime,
    roleNames: raw.roleNames,
  }
}

export function normalizeAdminUserList(records: AdminUserListRaw[]): AdminUserListVO[] {
  if (!Array.isArray(records)) return []
  return records.map(normalizeAdminUserListItem).filter((item) => item.id)
}

/** 分配商户角色请求参数 */
export interface AssignMerchantRoleParams {
  userId: UserId
  roleId: number
}

/** 更新用户状态请求参数 */
export interface UpdateUserStatusParams {
  userId: UserId
  status: UserStatus
}
