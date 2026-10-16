import { get, post } from '@/utils/request'
import type { RechargeBalanceRequest, UserBalanceVO } from '@/types/balance'

const BASE = '/user/balance'

/** 查询当前用户余额 GET /user/balance/get */
export function getBalance() {
  return get<UserBalanceVO>(`${BASE}/get`)
}

/** 当前用户余额充值 POST /user/balance/recharge */
export function rechargeBalance(data: RechargeBalanceRequest) {
  return post<UserBalanceVO>(`${BASE}/recharge`, data)
}
