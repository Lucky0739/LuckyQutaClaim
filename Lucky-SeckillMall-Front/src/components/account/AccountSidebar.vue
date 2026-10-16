<template>
  <aside class="account-sidebar">
    <div class="account-sidebar-head">账户管理</div>
    <nav class="account-nav">
      <div class="nav-group">
        <h3 class="nav-group-title">账户设置</h3>
        <router-link to="/account/profile" class="nav-item" active-class="nav-item--active">
          个人资料
        </router-link>
        <router-link to="/account/address" class="nav-item" active-class="nav-item--active">
          收货地址
        </router-link>
        <router-link to="/account/balance" class="nav-item" active-class="nav-item--active">
          账户余额
        </router-link>
        <router-link to="/account/password" class="nav-item" active-class="nav-item--active">
          修改密码
        </router-link>
        <router-link to="/account/cancel" class="nav-item nav-item--danger" active-class="nav-item--active">
          注销账号
        </router-link>
      </div>
    </nav>
  </aside>
</template>
