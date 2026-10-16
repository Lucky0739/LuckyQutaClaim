export interface SeckillItem {
  id: string
  name: string
  seckillPrice: number
  originalPrice: number
  color: string
}

export const homeSeckillItems: SeckillItem[] = [
  {
    id: '1',
    name: '无线蓝牙耳机',
    seckillPrice: 99,
    originalPrice: 299,
    color: '#ff6b35',
  },
  {
    id: '2',
    name: '运动休闲鞋',
    seckillPrice: 159,
    originalPrice: 399,
    color: '#4ecdc4',
  },
  {
    id: '3',
    name: '保温杯 500ml',
    seckillPrice: 39,
    originalPrice: 89,
    color: '#45b7d1',
  },
  {
    id: '4',
    name: '护肤套装礼盒',
    seckillPrice: 128,
    originalPrice: 268,
    color: '#f7b731',
  },
]
