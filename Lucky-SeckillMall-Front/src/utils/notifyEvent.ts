/** 消息已读状态变更后触发，用于同步首页角标等 */
export const NOTIFY_CHANGED_EVENT = 'notify-changed'

export function dispatchNotifyChanged() {
  window.dispatchEvent(new CustomEvent(NOTIFY_CHANGED_EVENT))
}
