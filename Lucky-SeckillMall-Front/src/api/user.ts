import request, { get, post, postForm, postVoid } from '@/utils/request'
import type { ApiResponse, PageResult, UploadPictureResult } from '@/types/common'
import type {
  AdminUpdateUserProfileRequest,
  AdminUserListRaw,
  AdminUserListVO,
  AdminUserQueryRequest,
  AssignMerchantRoleParams,
  CancelAccountRequest,
  ChangePasswordRequest,
  LoginUserRequest,
  LoginUserRspVO,
  RegisterUserRequest,
  SendVerifyCodeRequest,
  UpdateUserProfileRequest,
  UpdateUserStatusParams,
  UserId,
  UserProfileVO,
} from '@/types/user'
import { normalizeAdminUserList } from '@/types/user'
import { parseJsonPreservingLongIds } from '@/utils/json'

const BASE = '/user'

/** 用户注册 */
export function register(data: RegisterUserRequest) {
  return postVoid(`${BASE}/register`, data)
}

/**
 * 用户登录
 * POST /user/login
 * @param data.mobile 手机号
 * @param data.type 登录类型（1=密码登录，2=验证码登录）
 * @param data.password type=1 时必填
 * @param data.verifyCode type=2 时必填
 */
export function login(data: LoginUserRequest) {
  return post<LoginUserRspVO>(`${BASE}/login`, data)
}

/**
 * 发送短信验证码
 * POST /user/code/send
 * @param data.mobile 手机号
 * @param data.type 验证码类型（1=注册，2=登录）
 * @param data.captchaId 行为验证码校验 ID（/captcha/check 返回）
 */
export function sendVerifyCode(data: SendVerifyCodeRequest) {
  return postVoid(`${BASE}/code/send`, data)
}

/** 退出登录 */
export function logout() {
  return postVoid(`${BASE}/logout`)
}

/** 用户注销账号 POST /user/account/cancel */
export function cancelAccount(data: CancelAccountRequest) {
  return postVoid(`${BASE}/account/cancel`, data)
}

/** 修改登录密码 POST /user/password/change */
export function changePassword(data: ChangePasswordRequest) {
  return postVoid(`${BASE}/password/change`, data)
}

/** 管理员删除用户 POST /user/admin/delete */
export function adminDeleteUser(userId: UserId) {
  return postVoid(`${BASE}/admin/delete`, null, { params: { userId } })
}

/**
 * 完善用户资料（本人）
 * POST /user/profile/update
 * @param data.nickname 用户昵称，不能为空
 */
export function updateUserProfile(data: UpdateUserProfileRequest) {
  return postVoid(`${BASE}/profile/update`, data)
}

/**
 * 上传用户头像（本人）
 * POST /user/avatar/upload
 * @param file 头像图片文件
 */
export function uploadAvatar(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return postForm<UploadPictureResult>(`${BASE}/avatar/upload`, formData)
}

/** 管理员为用户上传头像 */
export function adminUploadAvatar(userId: UserId, file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return postForm<UploadPictureResult>(`${BASE}/admin/avatar/upload/${userId}`, formData)
}

/** 管理员修改用户资料 */
export function adminUpdateUserProfile(data: AdminUpdateUserProfileRequest) {
  return postVoid(`${BASE}/admin/profile/update`, data)
}

/**
 * 查看用户资料（优先从 Redis 缓存获取）
 * GET /user/profile
 */
export function getUserProfile() {
  return get<UserProfileVO>(`${BASE}/profile`)
}

/** 管理员禁用/启用用户 */
export function updateUserStatus(params: UpdateUserStatusParams) {
  return postVoid(`${BASE}/status/update`, null, { params })
}

/** 管理员分配商户角色 */
export function assignMerchantRole(params: AssignMerchantRoleParams) {
  return postVoid(`${BASE}/role/assign`, null, { params })
}

/** 获取用户角色列表（供其他服务调用） */
export function getUserRoles(userId: UserId) {
  return get<string[]>(`${BASE}/roles/${userId}`)
}

/** 管理员分页查询用户列表 */
export async function getUserListByAdmin(params: AdminUserQueryRequest) {
  const response = await request.get(`${BASE}/admin/list`, {
    params,
    transformResponse: [(raw: string) => {
      if (typeof raw !== 'string' || !raw) return raw
      try {
        return parseJsonPreservingLongIds<ApiResponse<PageResult<AdminUserListRaw>>>(raw)
      } catch {
        return JSON.parse(raw)
      }
    }],
  })

  const res = response.data as ApiResponse<PageResult<AdminUserListRaw>>
  if (res.success === false) {
    throw new Error(res.message || '加载用户列表失败')
  }

  const page = res.data ?? {
    records: [],
    total: 0,
    pageNum: params.pageNum ?? 1,
    pageSize: params.pageSize ?? 10,
    totalPages: 0,
  }

  return {
    ...page,
    total: Number(page.total ?? 0),
    pageNum: Number(page.pageNum ?? params.pageNum ?? 1),
    pageSize: Number(page.pageSize ?? params.pageSize ?? 10),
    totalPages: Number(page.totalPages ?? 0),
    records: normalizeAdminUserList(page.records ?? []),
  } satisfies PageResult<AdminUserListVO>
}
