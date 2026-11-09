package com.campusloop.app;

import com.campusloop.common.Database;
import com.campusloop.common.SchemaLoader;
import com.campusloop.common.exception.CampusException;
import com.campusloop.common.util.PasswordEncoder;
import com.campusloop.dao.DaoFactory;
import com.campusloop.dao.UserDao;
import com.campusloop.dao.jdbc.SqliteDaoFactory;
import com.campusloop.domain.Category;
import com.campusloop.domain.Item;
import com.campusloop.domain.ItemCondition;
import com.campusloop.domain.ItemType;
import com.campusloop.domain.Role;
import com.campusloop.domain.SearchCriteria;
import com.campusloop.domain.User;
import com.campusloop.domain.UserStatus;
import com.campusloop.service.ItemService;
import com.campusloop.service.UserService;
import com.campusloop.service.dto.ItemCommand;
import com.campusloop.service.dto.UserCommand;
import com.campusloop.service.sort.ItemSorter;
import com.campusloop.service.sort.ItemSorters;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * 迭代二控制台演示入口（迭代三将被 Swing 图形界面取代）。
 *
 * <p>作用：在不依赖界面的情况下验证用户、物品、检索、收藏全链路，
 * 也是服务层可被多种界面复用的证明。
 */
public final class ConsoleApp {

    private final UserService userService;
    private final ItemService itemService;
    private final DaoFactory daoFactory;
    private final Scanner scanner = new Scanner(System.in);
    private User currentUser;

    private ConsoleApp(DaoFactory daoFactory) {
        this.daoFactory = daoFactory;
        this.userService = new UserService(daoFactory);
        this.itemService = new ItemService(daoFactory);
    }

    public static void main(String[] args) {
        Database database = Database.getDefault();
        SchemaLoader.initialize(database);
        SqliteDaoFactory factory = new SqliteDaoFactory(database);
        seedAdmin(factory.userDao());
        System.out.println("低碳校园资源循环流转系统（迭代二：核心业务模块）");
        System.out.println("数据库：" + database.getUrl());
        new ConsoleApp(factory).run();
    }

    private void run() {
        while (true) {
            printMenu();
            String choice = scanner.nextLine().trim();
            try {
                switch (choice) {
                    case "1":
                        register();
                        break;
                    case "2":
                        login();
                        break;
                    case "3":
                        publishItem();
                        break;
                    case "4":
                        searchItems();
                        break;
                    case "5":
                        myItems();
                        break;
                    case "6":
                        toggleFavorite();
                        break;
                    case "7":
                        myFavorites();
                        break;
                    case "8":
                        showProfile();
                        break;
                    case "0":
                        System.out.println("再见！");
                        return;
                    default:
                        System.out.println("无效选项，请重新输入。");
                }
            } catch (CampusException e) {
                System.out.println("[业务提示] " + e.getMessage());
            } catch (RuntimeException e) {
                System.out.println("[系统异常] " + e.getMessage());
            }
        }
    }

    private void printMenu() {
        System.out.println();
        System.out.println("========== 主菜单 ==========");
        System.out.println("当前用户：" + (currentUser == null ? "未登录"
                : currentUser.getNickname() + "（" + currentUser.getUsername() + "，信用 "
                + currentUser.getCreditScore() + "）"));
        System.out.println("1. 注册   2. 登录   3. 发布物品   4. 检索物品");
        System.out.println("5. 我的发布   6. 收藏/取消收藏   7. 我的收藏   8. 我的资料   0. 退出");
        System.out.print("请选择：");
    }

    private void register() {
        System.out.print("用户名（4-20 位，字母开头）：");
        String username = scanner.nextLine();
        System.out.print("密码（8-32 位，含字母和数字）：");
        String password = scanner.nextLine();
        System.out.print("昵称：");
        String nickname = scanner.nextLine();
        System.out.print("联系方式（可空）：");
        String contact = scanner.nextLine();
        UserCommand command = new UserCommand()
                .setUsername(username).setPassword(password).setConfirmPassword(password)
                .setNickname(nickname).setContact(contact);
        User user = userService.register(command);
        System.out.println("注册成功，欢迎 " + user.getNickname() + "！");
    }

    private void login() {
        System.out.print("用户名：");
        String username = scanner.nextLine();
        System.out.print("密码：");
        String password = scanner.nextLine();
        currentUser = userService.login(username, password);
        System.out.println("登录成功，信用分：" + currentUser.getCreditScore());
    }

    private void publishItem() {
        requireLogin();
        List<Category> categories = daoFactory.categoryDao().findAll();
        System.out.println("分类列表：");
        for (Category category : categories) {
            System.out.println("  " + category.getId() + ". " + category.getName());
        }
        System.out.print("标题：");
        String title = scanner.nextLine();
        System.out.print("分类编号：");
        long categoryId = parseLong(scanner.nextLine());
        System.out.print("流转方式（1 出售 / 2 交换 / 3 赠送）：");
        String type = scanner.nextLine().trim();
        ItemType itemType;
        switch (type) {
            case "2":
                itemType = ItemType.EXCHANGE;
                break;
            case "3":
                itemType = ItemType.FREE;
                break;
            default:
                itemType = ItemType.SELL;
        }
        System.out.print("期望价格（交换/赠送填 0）：");
        double price = parseDouble(scanner.nextLine());
        System.out.print("描述：");
        String description = scanner.nextLine();
        System.out.print("标签（逗号分隔，可空）：");
        String tagLine = scanner.nextLine();
        List<String> tags = new ArrayList<>();
        for (String tag : tagLine.split("[,，]")) {
            if (!tag.isBlank()) {
                tags.add(tag.trim());
            }
        }
        ItemCommand command = new ItemCommand()
                .setTitle(title).setCategoryId(categoryId).setDescription(description)
                .setItemType(itemType).setPrice(price).setCondition(ItemCondition.GOOD)
                .setTags(tags);
        Item item = itemService.publish(currentUser.getId(), command);
        System.out.println("发布成功：" + item);
    }

    private void searchItems() {
        System.out.print("关键词（可空）：");
        String keyword = scanner.nextLine();
        System.out.print("排序（1 最新 / 2 价格升序 / 3 价格降序 / 4 最热）：");
        String sort = scanner.nextLine().trim();
        SearchCriteria criteria = SearchCriteria.builder().keyword(keyword).limit(20).build();
        ItemSorter sorter;
        switch (sort) {
            case "2":
                sorter = ItemSorters.PRICE_ASC;
                break;
            case "3":
                sorter = ItemSorters.PRICE_DESC;
                break;
            case "4":
                sorter = ItemSorters.HOTTEST;
                break;
            default:
                sorter = ItemSorters.NEWEST;
        }
        List<Item> items = itemService.search(criteria, sorter);
        if (items.isEmpty()) {
            System.out.println("没有符合条件的物品。");
            return;
        }
        System.out.printf("%-4s %-24s %-8s %-8s %-8s %s%n", "ID", "标题", "方式", "价格", "成色", "标签");
        for (Item item : items) {
            System.out.printf("%-4d %-24s %-8s %-8.2f %-8s %s%n", item.getId(),
                    ellipsis(item.getTitle(), 22), item.getItemType().getLabel(), item.getPrice(),
                    item.getCondition().getLabel(), String.join("/", item.getTags()));
        }
    }

    private void myItems() {
        requireLogin();
        List<Item> items = itemService.listByOwner(currentUser.getId());
        if (items.isEmpty()) {
            System.out.println("你还没有发布过物品。");
            return;
        }
        for (Item item : items) {
            System.out.println(item.getId() + " | " + item.getTitle() + " | " + item.getStatus()
                    + " | 浏览 " + item.getViewCount());
        }
        System.out.print("输入要下架的物品 ID（直接回车跳过）：");
        String input = scanner.nextLine().trim();
        if (!input.isEmpty()) {
            Item item = itemService.offShelf(currentUser.getId(), parseLong(input));
            System.out.println("已下架：" + item.getTitle());
        }
    }

    private void toggleFavorite() {
        requireLogin();
        System.out.print("物品 ID：");
        long itemId = parseLong(scanner.nextLine());
        if (itemService.isFavorite(currentUser.getId(), itemId)) {
            itemService.unfavorite(currentUser.getId(), itemId);
            System.out.println("已取消收藏。");
        } else {
            itemService.favorite(currentUser.getId(), itemId);
            System.out.println("收藏成功。");
        }
    }

    private void myFavorites() {
        requireLogin();
        List<Item> items = itemService.listFavorites(currentUser.getId());
        if (items.isEmpty()) {
            System.out.println("收藏夹为空。");
            return;
        }
        for (Item item : items) {
            System.out.println(item.getId() + " | " + item.getTitle() + " | " + item.getPrice());
        }
    }

    private void showProfile() {
        requireLogin();
        User user = userService.findById(currentUser.getId());
        System.out.println("用户名：" + user.getUsername());
        System.out.println("昵称：" + user.getNickname());
        System.out.println("角色：" + user.getRole().getLabel());
        System.out.println("信用分：" + user.getCreditScore());
        System.out.println("注册时间：" + user.getCreatedAt());
        System.out.println("发布数量：" + itemService.countByOwner(user.getId()));
    }

    private void requireLogin() {
        if (currentUser == null) {
            throw new CampusException("请先登录");
        }
    }

    private static long parseLong(String text) {
        try {
            return Long.parseLong(text.trim());
        } catch (NumberFormatException e) {
            throw new CampusException("请输入合法的数字");
        }
    }

    private static double parseDouble(String text) {
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            throw new CampusException("请输入合法的金额");
        }
    }

    private static String ellipsis(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }

    /**
     * 首次运行写入默认管理员账号，便于演示后台功能（正式部署应强制修改密码）。
     */
    private static void seedAdmin(UserDao userDao) {
        if (userDao.existsByUsername("admin")) {
            return;
        }
        User admin = new User(null, "admin", new PasswordEncoder().encode("Admin123456"),
                "系统管理员", null, Role.ADMIN, UserStatus.ACTIVE, 100, LocalDateTime.now(), null);
        userDao.insert(admin);
        System.out.println("已创建默认管理员：admin / Admin123456");
    }
}
