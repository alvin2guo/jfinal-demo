package com.shengmu.function.user;

import cn.dreampie.shiro.hasher.Hasher;
import cn.dreampie.shiro.hasher.HasherInfo;
import cn.dreampie.shiro.hasher.HasherKit;
import cn.dreampie.web.cache.CacheRemove;
import com.jfinal.aop.Before;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.activerecord.tx.Tx;
import com.shengmu.common.config.AppConstants;
import com.shengmu.common.web.controller.Controller;
import com.shengmu.function.order.model.Branch;
import com.shengmu.function.order.model.Region;
import com.shengmu.function.order.model.UserBranch;
import com.shengmu.function.user.model.User;

import java.util.Date;

/**
 * Created by wangrenhui on 14-1-3.
 */
public class MemberController extends Controller {

  public void index() {
    branch();
  }

  public void query() {
    User u = getModel(User.class);
    if (u.getLong("id") != null)
      setAttr("user", User.dao.findFirstBranchBy("`user`.id=?", u.get("id")));
    render("/view/app/user/detail.ftl");
  }

  public void branch() {
    keepPara();
    Integer pageNum = getParaToInt(0, 1);
    Integer pageSize = getParaToInt("pageSize", 15);
    String where = "";
    //branch
    Page<User> users = null;
    if (getParaToLong("branch_id") == null) {
      //region
      Long regionId = getParaToLong("region_id");
      if (regionId == null)
        users = User.dao.paginateByBranch(pageNum, pageSize, where + " `user`.deleted_at is null");
      else
        users = User.dao.paginateByRegion(pageNum, pageSize, where + " `branch`.region_id=?", regionId);
    } else
      users = User.dao.paginateByBranch(pageNum, pageSize, where + " `userBranch`.branch_id=?", getParaToLong("branch_id"));

    setAttr("regions", Region.dao.findBy("`region`.deleted_at is null"));
    setAttr("users", users);
    render("/view/app/user/branch.ftl");
  }

  @CacheRemove(name = AppConstants.DEFAULT_CACHENAME)
  @Before({Tx.class, MemberValidator.class})
  public void control() {
    User user = getModel(User.class);
    String todo = getPara("do");
    boolean result = false;
    if (todo != null) {
      if (todo.equals("delete") && user.getLong("id") != null) {
        result = user.set("deleted_at", new Date()).update();
      } else if (todo.equals("save") && getParaToLong("branch_id") != null) {
        HasherInfo hasher = HasherKit.hash(user.getStr("password"));

        if (user.getStr("first_name") == null)
          user.set("first_name", "");

        user.set("password", hasher.getHashResult()).set("salt", hasher.getSalt())
            .set("hasher", hasher.getHasher().value()).set("providername", "shengmu")
            .set("full_name", user.getStr("last_name") + "." + user.getStr("first_name"));
        result = user.save();
        user.addUserInfo(null).addRole(null).addBranch(Branch.dao.findById(getParaToLong("branch_id")));
      } else if (todo.equals("update") && user.getLong("id") != null) {
        if (getParaToLong("branch_id") != null && !user.getBranchId().equals(getParaToLong("branch_id")))
          UserBranch.dao.updateBy("SET branch_id=?", "user_id=?", getParaToLong("branch_id"), user.get("id"));
        result = user.removeNullValueAttrs().update();
      }
    }
    if (result) {
      setAttr("user", user);
      setSuccess();
    } else
      setError();
    render("/view/app/user/detail.ftl");
  }
}
