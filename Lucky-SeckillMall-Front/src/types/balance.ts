import type { UserId } from './user'

/** 用户余额充值请求（对应后端 RechargeBalanceRequest） */
export interface RechargeBalanceRequest {
  /** 充值金额，必须大于 0 */
  amount: number
}

/** 用户余额信息（对应后端 UserBalanceVO） */
export interface UserBalanceVO {
  userId: UserId
  balance: number
}

export function formatBalance(value?: number | null): string {
  if (value == null || !Number.isFinite(value)) return '0.00'
  return value.toFixed(2)
}
