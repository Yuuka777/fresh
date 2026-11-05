package com.xianhe.constant;

/**
 * 业务提示文案常量
 */
public class MessageConstant {

    //—— 登录与账号 ——

    //登录时用户名查不到
    public static final String ACCOUNT_NOT_FOUND = "账号不存在";

    //登录时密码不匹配
    public static final String PASSWORD_ERROR = "密码错误";

    //账号被禁用后尝试登录
    public static final String ACCOUNT_LOCKED = "账号被锁定";

    //token 缺失或非法，由两个 JWT 拦截器使用
    public static final String LOGIN_STATE_INVALID = "登录状态已失效";

    //修改密码时原密码不匹配（契约 2.1.8）
    public static final String PASSWORD_EDIT_FAILED = "原密码错误";

    //删除店员时目标账号处于启用状态（契约 2.1.9）
    public static final String STAFF_ENABLED_DELETE_DENIED = "启用中的店员不能删除";

    //删除店员时目标账号正是当前登录人（契约 2.1.9）
    public static final String SELF_DELETE_DENIED = "不能删除当前登录账号";

    //—— 分类 ——

    // 删除分类时该分类下仍有商品或组合箱
    public static final String CATEGORY_BE_RELATED = "分类下关联商品，无法删除";

    //商品 / 组合箱引用了不存在的分类 id
    public static final String CATEGORY_NOT_FOUND = "分类不存在";

    //商品挂到组合分类下、或组合箱挂到商品分类下
    public static final String CATEGORY_TYPE_MISMATCH = "分类类型不匹配";

    //商品规格缺少类型名称或候选值
    public static final String GOODS_SPEC_INCOMPLETE = "商品规格的名称与候选值不能为空";

    //组合箱内含商品的份数不是正数
    public static final String BUNDLE_GOODS_COPIES_INVALID = "内含商品的份数必须大于 0";

    //—— 商品 ——

    //按 id 查商品查不到
    public static final String GOODS_NOT_FOUND = "商品不存在";

    //删除商品时商品处于上架状态
    public static final String GOODS_ON_SALE = "商品处于上架状态，不能删除";

    //删除商品时该商品仍被组合箱引用
    public static final String GOODS_BE_RELATED_BY_BUNDLE = "商品关联组合箱，不能删除";

    //上架商品时其所属分类处于禁用状态
    public static final String GOODS_ENABLE_FAILED = "所属分类已禁用，无法上架";

    //下架商品时该商品仍被起售中的组合箱引用
    public static final String GOODS_DISABLE_FAILED = "商品被起售中的组合箱引用，无法下架";

    //—— 组合箱 ——

    //按 id 查组合箱查不到
    public static final String BUNDLE_NOT_FOUND = "组合箱不存在";

    //删除组合箱时组合箱处于起售状态
    public static final String BUNDLE_ON_SALE = "组合箱处于起售状态，不能删除";

    //起售组合箱时内含商品存在未上架项
    public static final String BUNDLE_ENABLE_FAILED = "组合箱内含商品未上架，无法起售";

    //起售组合箱时其所属分类处于禁用状态
    public static final String BUNDLE_CATEGORY_DISABLED = "所属分类已禁用，无法起售";

    //新增或修改组合箱时未提交任何内含商品
    public static final String BUNDLE_GOODS_EMPTY = "组合箱至少需要一个内含商品";

    //新增或修改组合箱时同一商品被提交了多次
    public static final String BUNDLE_GOODS_DUPLICATE = "组合箱内含商品重复";

    //—— 会员与购物车（用户端）——

    //微信登录：code 换不到 openid（code 过期、AppID 不符、微信不可达）
    public static final String WX_LOGIN_FAILED = "微信登录失败";

    //减少购物车数量时该记录已不在购物车中
    public static final String CART_EMPTY = "购物车为空";

    //加购请求既没带 goodsId 也没带 bundleId
    public static final String CART_TARGET_REQUIRED = "商品或组合箱必须二选一";

    //—— 收货地址（用户端）——

    //按 id 查不到地址，或该地址不属于当前会员：两种情况共用一个文案，不给外人用 id 探测
    public static final String ADDRESS_NOT_FOUND = "地址不存在";

    //该会员没有任何默认地址（契约 8.7）
    public static final String ADDRESS_DEFAULT_NOT_FOUND = "没有查询到默认地址";

    //新增 / 修改地址时收货人为空
    public static final String ADDRESS_CONSIGNEE_REQUIRED = "收货人不能为空";

    //新增 / 修改地址时手机号不是 11 位
    public static final String ADDRESS_PHONE_INVALID = "手机号格式不正确";

    //新增 / 修改地址时详细地址为空
    public static final String ADDRESS_DETAIL_REQUIRED = "详细地址不能为空";

    //—— 订单 ——

    //下单时购物车为空（文案与契约 9.1 的失败场景一致）
    public static final String SHOPPING_CART_IS_NULL = "购物车为空";

    //下单时收货地址为空（地址 id 查不到、或不属于当前会员）
    public static final String ADDRESS_IS_NULL = "收货地址为空";

    //下单时购物车里存在已下架 / 已停售的项
    public static final String CART_ITEM_OFF_SHELF = "购物车中有商品已下架，请先处理";

    //订单流转时当前状态不允许该操作
    public static final String ORDER_STATUS_ERROR = "订单状态错误";

    //按 id 查不到订单
    public static final String ORDER_NOT_FOUND = "订单不存在";

    //—— 通用 ——

    //文件上传失败
    public static final String UPLOAD_FAILED = "文件上传失败";

    //唯一索引冲突时拼接在字段值后面，例如「zhangsan已存在」
    public static final String ALREADY_EXISTS = "已存在";

    //未归类异常的兜底文案
    public static final String UNKNOWN_ERROR = "未知错误";

    private MessageConstant() {
    }
}
